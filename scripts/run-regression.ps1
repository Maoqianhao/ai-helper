param(
    [string]$BaseUrl = "http://localhost:8081/api",
    [int]$MemoryIterations = 3,
    [int]$ForgettingIterations = 3,
    [string]$OutputDir = "benchmark\results"
)

$ErrorActionPreference = "Stop"

function Ensure-Dir([string]$p) { if (-not (Test-Path $p)) { New-Item -ItemType Directory -Path $p | Out-Null } }
function Stamp { return Get-Date -Format "yyyyMMdd-HHmmss" }

function Invoke-Delete([string]$u) {
    try { Invoke-RestMethod -Method Delete -Uri $u -ContentType "application/json" | Out-Null } catch {}
}

function Invoke-Get([string]$u) { Invoke-RestMethod -Method Get -Uri $u -ContentType "application/json" }

function Http-Get([string]$u) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    try {
        $r = Invoke-WebRequest -Method Get -Uri $u -UseBasicParsing
        $sw.Stop()
        return @{ Status = [int]$r.StatusCode; Ms = [int]$sw.Elapsed.TotalMilliseconds; Body = "$($r.Content)" }
    } catch {
        $sw.Stop()
        return @{ Status = -1; Ms = [int]$sw.Elapsed.TotalMilliseconds; Body = "" }
    }
}

function Normalize-Sse([string]$t) {
    if ([string]::IsNullOrEmpty($t)) { return "" }
    $x = $t.ToLowerInvariant() -replace "event:message","" -replace "data:","" -replace "\s+",""
    return $x
}

function Token-Hit([string]$body, [string[]]$keys) {
    $n = Normalize-Sse $body
    foreach ($k in $keys) {
        if ($n.Contains($k.ToLowerInvariant().Replace(" ",""))) { return 1 }
    }
    return 0
}

Ensure-Dir $OutputDir
$s = Stamp
$memDetail = Join-Path $OutputDir "memory-detail-$s.csv"
$memSummary = Join-Path $OutputDir "memory-summary-$s.txt"
$forgetDetail = Join-Path $OutputDir "forgetting-detail-$s.csv"
$forgetSummary = Join-Path $OutputDir "forgetting-summary-$s.txt"
$promptDetail = Join-Path $OutputDir "prompt-detail-$s.csv"
$promptSummary = Join-Path $OutputDir "prompt-summary-$s.txt"

$expectedDb = @("zxqdb_2026_token")
$facts = @(
    "Remember: my database token is ZXQDB_2026_TOKEN.",
    "Remember: my name token is USER_TOKEN_7788."
)
$q = "Tell me which database token I mainly use. Reply with token only."

# --- Memory A/B ---
$rows = @()
for ($i = 1; $i -le $MemoryIterations; $i++) {
    Invoke-Delete "$BaseUrl/memory/clear/1001"
    Invoke-Delete "$BaseUrl/memory/clear/2001"
    foreach ($f in $facts) {
        $enc = [uri]::EscapeDataString($f)
        $null = Http-Get "$BaseUrl/ai/chat/routing?memoryId=1001&message=$enc"
        Start-Sleep -Milliseconds 400
    }
    $encQ = [uri]::EscapeDataString($q)
    $w = Http-Get "$BaseUrl/ai/chat/routing?memoryId=1001&message=$encQ"
    Start-Sleep -Milliseconds 400
    $wo = Http-Get "$BaseUrl/ai/chat/routing?memoryId=2001&message=$encQ"
    $rows += [pscustomobject]@{ iter=$i; case="with_memory"; hit=(Token-Hit $w.Body $expectedDb); ms=$w.Ms; status=$w.Status }
    $rows += [pscustomobject]@{ iter=$i; case="without_memory"; hit=(Token-Hit $wo.Body $expectedDb); ms=$wo.Ms; status=$wo.Status }
}
$rows | Export-Csv $memDetail -NoTypeInformation -Encoding UTF8
$wm = @($rows | ? { $_.case -eq "with_memory" })
$nm = @($rows | ? { $_.case -eq "without_memory" })
$wh = ($wm | Measure-Object hit -Average).Average
$nh = ($nm | Measure-Object hit -Average).Average
$wms = ($wm | Measure-Object ms -Average).Average
$nms = ($nm | Measure-Object ms -Average).Average
@"
Memory benchmark
Time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
with_memory hit_rate=$([math]::Round($wh,4)) avg_ms=$([math]::Round($wms,2))
without_memory hit_rate=$([math]::Round($nh,4)) avg_ms=$([math]::Round($nms,2))
latency_improvement_pct=$([math]::Round((($nms-$wms)/[math]::Max($nms,0.001))*100,2))
Detail: $memDetail
"@ | Set-Content $memSummary -Encoding UTF8

# --- Forgetting ---
$frows = @()
for ($i = 1; $i -le $ForgettingIterations; $i++) {
    Invoke-Delete "$BaseUrl/memory/clear/3001"
    foreach ($t in @("Remember: my preferred database token is PG_TOKEN_99.")) {
        $e = [uri]::EscapeDataString($t)
        $null = Http-Get "$BaseUrl/ai/chat/routing?memoryId=3001&message=$e"
        Start-Sleep -Milliseconds 400
    }
    $eq = [uri]::EscapeDataString("Which database token do I prefer? Reply with token only.")
    $b = Http-Get "$BaseUrl/ai/chat/routing?memoryId=3001&message=$eq"
    $bh = Token-Hit $b.Body @("pg_token_99")
    $hb = Invoke-Get "$BaseUrl/memory/history/3001"
    $histBefore = if ($hb.messages) { $hb.messages.Count } else { 0 }
    Invoke-Delete "$BaseUrl/memory/clear/3001"
    $ha = Invoke-Get "$BaseUrl/memory/history/3001"
    $cnt = if ($ha.messages) { $ha.messages.Count } else { 0 }
    $a = Http-Get "$BaseUrl/ai/chat/routing?memoryId=3001&message=$eq"
    $ah = Token-Hit $a.Body @("pg_token_99")
    $frows += [pscustomobject]@{ iter=$i; phase="before_clear"; hit=$bh; ms=$b.Ms; hist=$histBefore }
    $frows += [pscustomobject]@{ iter=$i; phase="after_clear"; hit=$ah; ms=$a.Ms; immediate_clear=$cnt }
}
$frows | Export-Csv $forgetDetail -NoTypeInformation -Encoding UTF8
@"
Forgetting benchmark
Time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
Detail: $forgetDetail
"@ | Set-Content $forgetSummary -Encoding UTF8

# --- Prompt (minimal: SQL routing) ---
$pcases = @(
    @{ id="sql_routing"; path="/ai/chat/routing"; prompt="Return only one MySQL line: SELECT 1" }
)
$prows = @()
foreach ($c in $pcases) {
    $e = [uri]::EscapeDataString($c.prompt)
    $u = "$BaseUrl$($c.path)?memoryId=9100&message=$e"
    $r = Http-Get $u
    $ok = if ((Normalize-Sse $r.Body) -match "select") { 1 } else { 0 }
    $prows += [pscustomobject]@{ case_id=$c.id; hit=$ok; ms=$r.Ms; status=$r.Status }
}
$prows | Export-Csv $promptDetail -NoTypeInformation -Encoding UTF8
$pr = ($prows | Measure-Object hit -Average).Average
@"
Prompt smoke test
Time: $(Get-Date -Format "yyyy-MM-dd HH:mm:ss")
pass_rate=$([math]::Round($pr,4))
Detail: $promptDetail
"@ | Set-Content $promptSummary -Encoding UTF8

Write-Host "Done."
Write-Host $memSummary
Write-Host $forgetSummary
Write-Host $promptSummary

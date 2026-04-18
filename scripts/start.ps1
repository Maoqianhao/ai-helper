param(
    [string]$Profile = ""
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
Set-Location $root

$cmd = ".\mvnw.cmd spring-boot:run"
if ($Profile -ne "") {
    $cmd = "$cmd -Dspring-boot.run.profiles=$Profile"
}

Write-Host "[ai-helper] starting with command: $cmd"
Invoke-Expression $cmd

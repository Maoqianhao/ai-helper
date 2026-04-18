package com.example.aihelper.agent.workers;
import com.example.aihelper.rag.vectorstore.MetadataVectorStore;
import com.example.aihelper.utils.TraceIdUtil;
import com.example.aihelper.utils.TextUtil;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL 专家实现
 * 利用 RAG 检索表结构，并执行 SQL 查询
 */
@Slf4j
@Service
public class SqlExpertImpl implements SqlExpert {
    private static final String TOOL_NAME = "sqlExpert.queryDatabase";
    private static final String READ_ONLY_BLOCK_MESSAGE = "出于安全限制，仅支持只读查询（SELECT）。请改为查询类问题。";
    private static final List<String> DEFAULT_WRITE_OPERATION_HINTS = List.of(
            "delete ", "update ", "insert ", "drop ", "truncate ", "alter ", "create ", "replace ", "grant ", "revoke "
    );
    private static final Pattern WRITE_SQL_PATTERN = Pattern.compile("(?i)\\b(delete|update|insert|drop|truncate|alter|create|replace|grant|revoke)\\b");

    private final DataSource dataSource;
    private final ChatModel qwenChatModel;
    private final MetadataVectorStore vectorStore;
    private final List<String> writeOperationHints;

    public SqlExpertImpl(DataSource dataSource,
                         ChatModel qwenChatModel,
                         MetadataVectorStore vectorStore,
                         @Value("${sql-guard.write-operation-hints:}") List<String> configuredHints) {
        this.dataSource = dataSource;
        this.qwenChatModel = qwenChatModel;
        this.vectorStore = vectorStore;
        this.writeOperationHints = normalizeHints(configuredHints);
    }

    @Override
    @Tool("专门处理数据库查询、SQL 生成、数据统计、报表分析、店铺信息查询")
    public String queryDatabase(String request) {
        String traceId = TraceIdUtil.getTraceId();
        long totalStart = System.currentTimeMillis();
        int requestHash = request == null ? 0 : request.hashCode();
        log.info("[traceId={}] [route] routeDecision=sql", traceId);
        log.info("[traceId={}] [tool-call] toolName={}, argsHash={}", traceId, TOOL_NAME, requestHash);
        log.debug("[traceId={}] [tool-input] toolName={} requestPreview={}",
                traceId, TOOL_NAME, TextUtil.truncate(request, 80));

        if (isPotentialWriteOperation(request)) {
            log.warn("[traceId={}] [tool-guard] blocked toolName={}, reason=potential_write_operation, argsHash={}",
                    traceId, TOOL_NAME, requestHash);
            return READ_ONLY_BLOCK_MESSAGE;
        }

        try {
            // 1. 利用向量检索获取相关表结构描述（RAG 第一步）
            long step1Start = System.currentTimeMillis();
            List<EmbeddingMatch<TextSegment>> matches = vectorStore.searchRelevantTables(request, 5);
            log.info("[traceId={}] [tool-step] toolName={}, step=vector_retrieval, tableMatches={}, durationMs={}",
                    traceId, TOOL_NAME, matches.size(), System.currentTimeMillis() - step1Start);
            
            // 2. 提取相关表的描述信息作为上下文
            String context = matches.stream()
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n\n"));

            log.debug("[traceId={}] [tool-context] toolName={}, contextLength={}", traceId, TOOL_NAME, context.length());

            // 3. 构建 SQL 数据库检索器（具备执行 SQL 的能力）
            long step2Start = System.currentTimeMillis();
            ContentRetriever sqlRetriever = dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever.builder()
                    .dataSource(dataSource)
                    .chatModel(qwenChatModel)
                    .build();
            log.info("[traceId={}] [tool-step] toolName={}, step=build_sql_retriever, durationMs={}",
                    traceId, TOOL_NAME, System.currentTimeMillis() - step2Start);

            // 4. 直接使用检索器执行查询（不再创建内部 Executor，消除嵌套）
            long step3Start = System.currentTimeMillis();
            
            // 直接调用 contentRetriever 检索（它会内部完成：生成SQL -> 执行 -> 返回结果）
            List<dev.langchain4j.rag.content.Content> contents = sqlRetriever.retrieve(Query.from(request));
            
            String result = contents.stream()
                    .map(c -> c.textSegment().text())
                    .collect(Collectors.joining("\n"));

            // 第二层护栏：即使请求文本看起来安全，也阻断包含写操作 SQL 片段的结果回传
            if (containsPotentialWriteSql(result)) {
                log.error("[traceId={}] [tool-guard] blocked toolName={}, reason=detected_write_sql_in_result, argsHash={}",
                        traceId, TOOL_NAME, requestHash);
                log.warn("[traceId={}] [tool-call] toolName={}, success=false, durationMs={}, errorType=WriteSqlDetectedInResult, argsHash={}",
                        traceId, TOOL_NAME, System.currentTimeMillis() - totalStart, requestHash);
                return READ_ONLY_BLOCK_MESSAGE;
            }
            
            log.info("[traceId={}] [tool-step] toolName={}, step=execute_sql_query, durationMs={}",
                    traceId, TOOL_NAME, System.currentTimeMillis() - step3Start);
            log.info("[traceId={}] [tool-call] toolName={}, success=true, durationMs={}, argsHash={}",
                    traceId, TOOL_NAME, System.currentTimeMillis() - totalStart, requestHash);
            return result;

        } catch (Exception e) {
            log.warn("[traceId={}] [tool-call] toolName={}, success=false, durationMs={}, errorType={}, argsHash={}",
                    traceId, TOOL_NAME, System.currentTimeMillis() - totalStart, e.getClass().getSimpleName(), requestHash, e);
            return "数据库查询失败: " + e.getMessage();
        }
    }

    private boolean isPotentialWriteOperation(String request) {
        if (request == null || request.isBlank()) {
            return false;
        }
        String normalized = normalizeForKeywordScan(request);
        return WRITE_SQL_PATTERN.matcher(normalized).find()
                || writeOperationHints.stream().anyMatch(normalized::contains);
    }

    private boolean containsPotentialWriteSql(String resultText) {
        if (resultText == null || resultText.isBlank()) {
            return false;
        }
        String normalized = normalizeForKeywordScan(resultText);
        if (!(normalized.contains("sql") || normalized.contains("query") || normalized.contains("语句"))) {
            return false;
        }
        return WRITE_SQL_PATTERN.matcher(normalized).find()
                || writeOperationHints.stream().anyMatch(normalized::contains);
    }

    private List<String> normalizeHints(List<String> configuredHints) {
        if (configuredHints == null || configuredHints.isEmpty()) {
            return DEFAULT_WRITE_OPERATION_HINTS;
        }
        List<String> normalizedHints = configuredHints.stream()
                .filter(hint -> hint != null && !hint.isBlank())
                .map(hint -> hint.toLowerCase(Locale.ROOT))
                .toList();
        return normalizedHints.isEmpty() ? DEFAULT_WRITE_OPERATION_HINTS : normalizedHints;
    }

    private String normalizeForKeywordScan(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replace('`', ' ');
    }

}

package com.example.aihelper.memory;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.logical.And;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Chroma 记忆数据访问层。
 *
 * 仅负责与向量库交互，不承载业务策略判断。
 */
@Slf4j
@Repository
public class ChromaMemoryRepository {
    private static final int MAX_RANGE_SPLIT_DEPTH = 16;
    private static final double IMPORTANCE_SCORE_MIN = 0.0;
    private static final double IMPORTANCE_SCORE_MAX = 1.0;
    private static final double MIN_SPLIT_RANGE = 0.0001;
    private final Embedding filterProbeEmbedding;
    private final int perSearchMaxResults;

    private final EmbeddingStore<TextSegment> memoryEmbeddingStore;
    private final EmbeddingModel embeddingModel;

    public ChromaMemoryRepository(@Qualifier("memoryEmbeddingStore") EmbeddingStore<TextSegment> memoryEmbeddingStore,
                                  EmbeddingModel embeddingModel,
                                  @Value("${chroma.memory-per-search-max-results:10}") int perSearchMaxResults) {
        this.memoryEmbeddingStore = memoryEmbeddingStore;
        this.embeddingModel = embeddingModel;
        this.perSearchMaxResults = Math.max(1, perSearchMaxResults);
        // Chroma 的 metadata 过滤仍依赖向量检索入口，使用非零探针向量避免全零向量导致空结果。
        this.filterProbeEmbedding = embeddingModel.embed("filter_probe").content();
    }
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    /**
     * 保存单个消息到 Chroma
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param role 角色（USER/ASSISTANT/SYSTEM）
     * @param content 消息内容
     * @param importanceScore 重要性评分
     * @return 文档ID
     */
    public String saveMessage(String userId, String sessionId, String role, 
                              String content, double importanceScore) {
        try {
            // 1. 生成 Embedding
            Embedding embedding = embeddingModel.embed(content).content();
            
            // 2. 构建元数据
            Metadata metadata = new Metadata();
            metadata.put("user_id", userId);
            metadata.put("session_id", sessionId);
            metadata.put("role", role);
            metadata.put("importance_score", importanceScore);
            metadata.put("access_count", 0);
            metadata.put("created_at", LocalDateTime.now().format(DATE_FORMATTER));
            metadata.put("updated_at", LocalDateTime.now().format(DATE_FORMATTER));
            metadata.put("status", "active");
            
            // 3. 生成唯一 ID
            String documentId = generateDocumentId(sessionId, LocalDateTime.now());
            metadata.put("document_id", documentId);
            
            // 4. 存入 Chroma
            TextSegment segment = TextSegment.from(content, metadata);
            memoryEmbeddingStore.add(embedding, segment);
            
            log.debug("[memory-repo] event=save_success documentId={} role={}", documentId, role);
            return documentId;
            
        } catch (Exception e) {
            log.error("[memory-repo] event=save_error contentPreview={}",
                    content.substring(0, Math.min(20, content.length())), e);
            throw new RuntimeException("保存消息到 Chroma 失败", e);
        }
    }
    
    /**
     * 查询指定会话的所有活跃记忆
     * 
     * @param sessionId 会话ID
     * @return 匹配结果列表
     */
    public List<EmbeddingMatch<TextSegment>> findBySessionId(String sessionId) {
        Filter activeFilter = metadataKey("status").isEqualTo("active");
        Filter sessionFilter = metadataKey("session_id").isEqualTo(sessionId);
        return fetchAllByFilter(new And(activeFilter, sessionFilter));
    }
    
    /**
     * 查询所有活跃记忆
     * 
     * @return 匹配结果列表
     */
    public List<EmbeddingMatch<TextSegment>> findAllActive() {
        Filter filter = metadataKey("status").isEqualTo("active");
        return fetchAllByFilter(filter);
    }
    
    /**
     * 按过滤条件删除记忆
     * 
     * @param filter 过滤条件
     * @return 删除的记录数（Chroma 不返回具体数量，这里返回 0）
     */
    public int deleteByFilter(Filter filter) {
        try {
            memoryEmbeddingStore.removeAll(filter);
            log.debug("[memory-repo] event=delete_by_filter_completed");
            return 0; // Chroma 不返回删除数量
        } catch (Exception e) {
            log.error("[memory-repo] event=delete_by_filter_error", e);
            throw new RuntimeException("从 Chroma 删除记录失败", e);
        }
    }
    
    /**
     * 按 Chroma 内部 ID 删除单条记录
     * 
     * @param chromaId Chroma 内部 ID
     */
    public void deleteById(String chromaId) {
        try {
            memoryEmbeddingStore.remove(chromaId);
            log.debug("[memory-repo] event=delete_by_id_success chromaId={}", chromaId);
        } catch (Exception e) {
            log.error("[memory-repo] event=delete_by_id_error chromaId={}", chromaId, e);
            throw new RuntimeException("从 Chroma 删除记录失败", e);
        }
    }
    
    /**
     * 通用搜索方法（使用虚拟向量，仅用于元数据过滤）
     * 
     * @param filter 过滤条件
     * @param maxResults 最大结果数
     * @return 匹配结果列表
     */
    private List<EmbeddingMatch<TextSegment>> searchWithFilter(Filter filter, int maxResults) {
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
            .queryEmbedding(filterProbeEmbedding)
            // langchain4j 1.2.0 要求 minScore 必须在 [0, 1] 区间内
            .minScore(0.0)
            .maxResults(maxResults)
            .filter(filter)
            .build();
        
        EmbeddingSearchResult<TextSegment> result = memoryEmbeddingStore.search(request);
        
        return result.matches().stream()
            .collect(Collectors.toList());
    }

    private List<EmbeddingMatch<TextSegment>> fetchAllByFilter(Filter baseFilter) {
        List<EmbeddingMatch<TextSegment>> fetched = fetchByImportanceRange(
                baseFilter,
                IMPORTANCE_SCORE_MIN,
                IMPORTANCE_SCORE_MAX,
                true,
                0
        );
        return deduplicate(fetched);
    }

    private List<EmbeddingMatch<TextSegment>> fetchByImportanceRange(
            Filter baseFilter,
            double minScore,
            double maxScore,
            boolean includeUpperBound,
            int depth) {
        Filter scopedFilter = scoreScopedFilter(baseFilter, minScore, maxScore, includeUpperBound);
        List<EmbeddingMatch<TextSegment>> matches = searchWithFilter(scopedFilter, perSearchMaxResults);
        if (matches.size() < perSearchMaxResults) {
            return matches;
        }

        double range = maxScore - minScore;
        if (depth >= MAX_RANGE_SPLIT_DEPTH || range <= MIN_SPLIT_RANGE) {
            log.warn("[memory-repo] event=fetch_truncated depth={} range=[{}, {}] cap={}",
                    depth, minScore, maxScore, perSearchMaxResults);
            return matches;
        }

        double midpoint = minScore + range / 2.0;

        List<EmbeddingMatch<TextSegment>> left = fetchByImportanceRange(baseFilter, minScore, midpoint, false, depth + 1);
        List<EmbeddingMatch<TextSegment>> right = fetchByImportanceRange(baseFilter, midpoint, maxScore, includeUpperBound, depth + 1);
        List<EmbeddingMatch<TextSegment>> merged = new ArrayList<>(left.size() + right.size());
        merged.addAll(left);
        merged.addAll(right);
        return merged;
    }

    private Filter scoreScopedFilter(Filter baseFilter, double minScore, double maxScore, boolean includeUpperBound) {
        Filter lowerBound = metadataKey("importance_score").isGreaterThanOrEqualTo(minScore);
        Filter upperBound = includeUpperBound
                ? metadataKey("importance_score").isLessThanOrEqualTo(maxScore)
                : metadataKey("importance_score").isLessThan(maxScore);
        return new And(baseFilter, new And(lowerBound, upperBound));
    }

    private List<EmbeddingMatch<TextSegment>> deduplicate(List<EmbeddingMatch<TextSegment>> matches) {
        Map<String, EmbeddingMatch<TextSegment>> unique = new LinkedHashMap<>();
        for (EmbeddingMatch<TextSegment> match : matches) {
            unique.putIfAbsent(uniqueKey(match), match);
        }
        return new ArrayList<>(unique.values());
    }

    private String uniqueKey(EmbeddingMatch<TextSegment> match) {
        if (match == null) {
            return "null";
        }
        String embeddingId = match.embeddingId();
        if (embeddingId != null && !embeddingId.isBlank()) {
            return embeddingId;
        }
        TextSegment segment = match.embedded();
        if (segment == null) {
            return "segment:null";
        }
        String documentId = segment.metadata().getString("document_id");
        if (documentId != null && !documentId.isBlank()) {
            return "doc:" + documentId;
        }
        return "fallback:" + Objects.hash(segment.text(), segment.metadata());
    }
    
    /**
     * 生成文档 ID
     * 
     * @param sessionId 会话ID
     * @param createdAt 创建时间
     * @return 文档ID
     */
    private String generateDocumentId(String sessionId, LocalDateTime createdAt) {
        return sessionId + "_" + createdAt.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
    }
}

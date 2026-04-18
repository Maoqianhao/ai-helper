package com.example.aihelper.rag.vectorstore;

import com.example.aihelper.rag.metadata.TableMetadata;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 元数据向量存储服务。
 */
@Slf4j
@Service
public class MetadataVectorStore {
    
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;
    @SuppressWarnings("unchecked")
    public MetadataVectorStore(EmbeddingStore<?> embeddingStore, 
                               EmbeddingModel embeddingModel) {
        this.embeddingStore = (EmbeddingStore<TextSegment>) embeddingStore;
        this.embeddingModel = embeddingModel;
    }
    
    /**
     * 添加表元数据到向量存储
     * 
     * @param tableName 表名
     * @param description 表的描述文本
     * @param tableMetadata 表元数据对象（用于后续检索过滤）
     */
    public void addTableMetadata(String tableName, String description, Object tableMetadata) {
        log.info("[metadata-vector-store] event=add_start table={}", tableName);
        
        // 创建文本段，同时添加元数据
        Metadata metadata = new Metadata();
        metadata.put("table_name", tableName);
        metadata.put("indexed_at", java.time.LocalDateTime.now().toString());
        
        // 如果是 TableMetadata 对象，可以添加更多元数据
        if (tableMetadata instanceof TableMetadata) {
            TableMetadata tm =
                (TableMetadata) tableMetadata;
            // TableMetadata 使用的是 tableComment 而非 businessScenario
            metadata.put("table_comment", tm.getTableComment() != null ? tm.getTableComment() : "");
            metadata.put("primary_key", tm.getPrimaryKey() != null ? tm.getPrimaryKey() : "");
        }
        
        TextSegment textSegment = TextSegment.from(description, metadata);
        
        // 向量化
        Embedding embedding = embeddingModel.embed(textSegment).content();
        
        // 使用 table_name 过滤实现幂等更新（先删后增）
        Filter filter = metadataKey("table_name").isEqualTo(tableName);
        embeddingStore.removeAll(filter);
        embeddingStore.add(embedding, textSegment);
        
        log.info("[metadata-vector-store] event=add_completed table={}", tableName);
    }
    
    /**
     * 批量添加表元数据
     * 
     * @param metadataMap 表名 -> 描述文本的映射
     */
    public void addAllTableMetadata(java.util.Map<String, String> metadataMap) {
        log.info("[metadata-vector-store] event=batch_add_start tableCount={}", metadataMap.size());
        
        metadataMap.forEach((tableName, description) -> {
            addTableMetadata(tableName, description, null);
        });
        
        log.info("[metadata-vector-store] event=batch_add_completed");
    }
    
    /**
     * 根据查询文本检索相关表
     * 
     * @param query 查询文本（自然语言描述）
     * @param maxResults 最大返回结果数
     * @return 匹配的表列表，按相似度排序
     */
    public List<EmbeddingMatch<TextSegment>> searchRelevantTables(String query, int maxResults) {
        if (query == null || query.isBlank()) {
            log.warn("[metadata-vector-store] event=search_skipped reason=blank_query");
            return List.of();
        }
        log.info("[metadata-vector-store] event=search_start query={}", query);
        
        // 将查询向量化
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        
        // 在向量数据库中检索
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .build();
                
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        List<EmbeddingMatch<TextSegment>> matches = result.matches();
        
        log.info("[metadata-vector-store] event=search_completed matchCount={}", matches.size());
        
        // 打印结果
        matches.forEach(match -> {
            log.info("[metadata-vector-store] event=match tablePreview={} score={}",
                    match.embedded().text().substring(0, 
                            Math.min(50, match.embedded().text().length())),
                    match.score());
        });
        
        return matches;
    }
    
    /**
     * 根据查询文本检索相关表（带最小相似度阈值）
     * 
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @param minScore 最小相似度阈值（0-1之间）
     * @return 匹配的表列表
     */
    public List<EmbeddingMatch<TextSegment>> searchRelevantTables(
            String query, 
            int maxResults, 
            double minScore) {
        
        List<EmbeddingMatch<TextSegment>> allMatches = searchRelevantTables(query, maxResults);
        
        // 过滤低相似度的结果
        return allMatches.stream()
                .filter(match -> match.score() >= minScore)
                .toList();
    }
    
    /**
     * 删除指定表的元数据
     * 
     * @param tableName 表名
     */
    public void removeTableMetadata(String tableName) {
        log.info("[metadata-vector-store] event=remove table={}", tableName);
        Filter filter = metadataKey("table_name").isEqualTo(tableName);
        embeddingStore.removeAll(filter);
    }
    
    /**
     * 清空所有元数据
     */
    public void clearAllMetadata() {
        log.info("[metadata-vector-store] event=clear_all");
        embeddingStore.removeAll();
    }
    /**
     * 获取存储的表数量
     * 
     * @return 表数量
     */
    public int getTableCount() {
        // Chroma 没有直接的 count 方法，需要通过其他方式统计
        // 这里简化处理
        return 0;
    }
}

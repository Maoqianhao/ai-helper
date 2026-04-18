package com.example.aihelper.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 向量存储配置。
 */
@Slf4j
@Configuration
public class VectorStoreConfig {
    
    @Value("${chroma.base-url}")
    private String chromaBaseUrl;
    
    @Value("${chroma.memory-collection-name}")
    private String memoryCollectionName;
    
    @Value("${chroma.metadata-collection-name}")
    private String metadataCollectionName;
    
    /**
     * Chroma 向量存储 - 用于 RAG 元数据持久化
     * 存储数据库表结构描述，支持 SQL 专家智能定位相关表
     * 
     * 优势：
     * - 数据持久化（重启不丢失）
     * - HNSW 索引支持高效语义检索
     * - 支持元数据过滤（按表类型/业务域筛选）
     * - 适合生产环境
     */
    @Bean("metadataEmbeddingStore")
    @Primary
    public EmbeddingStore<TextSegment> metadataEmbeddingStore() {
        log.info("[vector-store] event=init_start domain=metadata collection={}", metadataCollectionName);
        
        try {
            ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaBaseUrl)
                    .collectionName(metadataCollectionName)
                    .build();
            
            log.info("[vector-store] event=init_completed domain=metadata collection={}", metadataCollectionName);
            return store;
            
        } catch (Exception e) {
            log.error("[vector-store] event=init_error domain=metadata baseUrl={}", chromaBaseUrl, e);
            throw new RuntimeException("Chroma 连接失败", e);
        }
    }
    
    /**
     * Chroma 向量存储 - 用于聊天记忆持久化
     * 优点：
     * - 数据持久化（重启不丢失）
     * - 内置高效的相似度搜索（HNSW 索引）
     * - 支持元数据过滤和批量删除
     * - 适合生产环境
     * 
     * 使用前需要：
     * 1. 安装 Chroma: pip install chromadb
     * 2. 启动服务: chroma run --path ./chroma_data
     * 3. 配置 application.yaml 中的 chroma.base-url
     */
    @Bean("memoryEmbeddingStore")
    public EmbeddingStore<TextSegment> memoryEmbeddingStore() {
        log.info("[vector-store] event=init_start domain=memory collection={}", memoryCollectionName);
        
        try {
            ChromaEmbeddingStore store = ChromaEmbeddingStore.builder()
                    .baseUrl(chromaBaseUrl)
                    .collectionName(memoryCollectionName)
                    .build();
            
            log.info("[vector-store] event=init_completed domain=memory collection={}", memoryCollectionName);
            return store;
            
        } catch (Exception e) {
            log.error("[vector-store] event=init_error domain=memory baseUrl={}", chromaBaseUrl, e);
            throw new RuntimeException("Chroma 连接失败", e);
        }
    }
}

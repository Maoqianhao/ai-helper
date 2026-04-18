package com.example.aihelper.config;

import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Embedding 模型配置。
 */
@Slf4j
@Configuration
public class EmbeddingModelConfig {
    
    /**
     * 创建本地 BGE 量化版 Embedding 模型。
     */
    @Bean
    @Primary
    public EmbeddingModel bgeEmbeddingModel() {
        log.info("[embedding-model] event=init type=bge-small-en-v15-quantized source=local");
        return new BgeSmallEnV15QuantizedEmbeddingModel();
    }
    
    /**
     * 注释掉的是 DashScope Embedding 模型（需要网络）
     * 如果需要更好的中文效果，可以切换到这个
     */
    // @Bean
    // public EmbeddingModel dashscopeEmbeddingModel() {
    //     log.info("初始化 DashScope Embedding 模型");
    //     return DashScopeEmbeddingModel.builder()
    //             .apiKey("your-api-key")
    //             .modelName("text-embedding-v3")
    //             .build();
    // }
}

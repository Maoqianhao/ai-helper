package com.example.aihelper.service;

import com.example.aihelper.rag.metadata.DatabaseMetadataExtractor;
import com.example.aihelper.rag.metadata.MetadataTextBuilder;
import com.example.aihelper.rag.metadata.TableMetadata;
import com.example.aihelper.rag.vectorstore.MetadataVectorStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 元数据向量化编排服务。
 */
@Slf4j
@Service
public class MetadataVectorizationService {
    
    private final DatabaseMetadataExtractor metadataExtractor;
    private final MetadataTextBuilder textBuilder;
    private final MetadataVectorStore vectorStore;

    @Value("${metadata.vectorize-on-startup:true}")
    private boolean vectorizeOnStartup;

    @Value("${metadata.async-initialization:true}")
    private boolean asyncInitialization;
    
    public MetadataVectorizationService(DatabaseMetadataExtractor metadataExtractor,
                                        MetadataTextBuilder textBuilder,
                                        MetadataVectorStore vectorStore) {
        this.metadataExtractor = metadataExtractor;
        this.textBuilder = textBuilder;
        this.vectorStore = vectorStore;
    }
    
    /**
     * 应用启动时自动执行元数据向量化
     * 检查向量库是否为空，如果为空则执行全量向量化
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initMetadataVectorization() {
        if (!vectorizeOnStartup) {
            log.info("[metadata-init] event=skipped reason=config_disabled");
            return;
        }

        if (asyncInitialization) {
            CompletableFuture.runAsync(this::doInitMetadataVectorization);
            return;
        }
        doInitMetadataVectorization();
    }

    private void doInitMetadataVectorization() {
        log.info("[metadata-init] event=start");
        
        try {
            log.info("[metadata-init] event=extract_start");
            List<TableMetadata> tables = metadataExtractor.extractAllTables();
            
            if (tables.isEmpty()) {
                log.warn("[metadata-init] event=skipped reason=no_tables");
                return;
            }
            
            log.info("[metadata-init] event=vectorize_start tableCount={}", tables.size());
            VectorizationSummary summary = vectorizeTables(tables);
            log.info("[metadata-init] event=completed successCount={} failCount={}",
                    summary.successCount(), summary.failCount());
            
        } catch (Exception e) {
            log.error("[metadata-init] event=error", e);
        }
        
        log.info("[metadata-init] event=end");
    }

    private VectorizationSummary vectorizeTables(List<TableMetadata> tables) {
        int successCount = 0;
        int failCount = 0;

        for (TableMetadata table : tables) {
            try {
                String enhancedText = textBuilder.buildEnhancedVectorText(table);
                vectorStore.addTableMetadata(table.getTableName(), enhancedText, table);
                successCount++;
                log.debug("[metadata-vectorize] event=table_success table={}", table.getTableName());
            } catch (Exception e) {
                failCount++;
                log.error("[metadata-vectorize] event=table_error table={}", table.getTableName(), e);
            }
        }

        return new VectorizationSummary(successCount, failCount);
    }

    private record VectorizationSummary(int successCount, int failCount) {
    }
}

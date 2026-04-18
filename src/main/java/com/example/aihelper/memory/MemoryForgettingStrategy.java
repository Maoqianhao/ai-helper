package com.example.aihelper.memory;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 记忆遗忘策略服务。
 */
@Slf4j
@Service
public class MemoryForgettingStrategy {
    
    private static final int KEEP_RECENT_COUNT = 50;
    private static final double IMPORTANCE_THRESHOLD = 0.5;
    private final MemorySelectionPolicy memorySelectionPolicy;

    public MemoryForgettingStrategy(MemorySelectionPolicy memorySelectionPolicy) {
        this.memorySelectionPolicy = memorySelectionPolicy;
    }
    
    /**
     * 对所有会话应用遗忘策略
     * 
     * @param allMatches 所有活跃记忆
     * @return 删除的总数
     */
    public int execute(List<EmbeddingMatch<TextSegment>> allMatches) {
        if (allMatches.isEmpty()) {
            log.info("[memory-forgetting] event=skipped reason=no_messages");
            return 0;
        }
        
        // 按 session_id 分组
        Map<String, List<EmbeddingMatch<TextSegment>>> sessionsMap = allMatches.stream()
            .collect(Collectors.groupingBy(
                match -> match.embedded().metadata().getString("session_id")
            ));
        
        log.info("[memory-forgetting] event=start sessionCount={} totalMessages={}", sessionsMap.size(), allMatches.size());
        
        // 对每个会话执行遗忘策略
        int totalDeleted = 0;
        for (Map.Entry<String, List<EmbeddingMatch<TextSegment>>> entry : sessionsMap.entrySet()) {
            String sessionId = entry.getKey();
            List<EmbeddingMatch<TextSegment>> sessionMatches = entry.getValue();
            
            int deletedCount = applyToSession(sessionId, sessionMatches);
            totalDeleted += deletedCount;
        }
        
        log.info("[memory-forgetting] event=completed deletedCount={}", totalDeleted);
        return totalDeleted;
    }
    
    /**
     * 对单个会话应用遗忘策略
     * 
     * @param sessionId 会话ID
     * @param sessionMatches 该会话的所有记忆
     * @return 删除的数量
     */
    public int applyToSession(String sessionId, List<EmbeddingMatch<TextSegment>> sessionMatches) {
        List<EmbeddingMatch<TextSegment>> toDelete = getMessagesToDelete(sessionMatches);
        int deletedCount = toDelete.size();
        int keepCount = Math.min(KEEP_RECENT_COUNT, sessionMatches.size());
        toDelete.forEach(match -> log.debug("[memory-forgetting] event=mark_delete chromaId={} documentId={} score={}",
                match.embeddingId(),
                match.embedded().metadata().getString("document_id"),
                match.embedded().metadata().getDouble("importance_score")));
        
        if (deletedCount > 0) {
            log.info("[memory-forgetting] event=session_processed sessionId={} keepCount={} deletedCount={}",
                sessionId, keepCount, deletedCount);
        }
        
        return deletedCount;
    }
    
    /**
     * 获取应该删除的记忆列表
     * 
     * @param sessionMatches 该会话的所有记忆
     * @return 应该删除的记忆列表
     */
    public List<EmbeddingMatch<TextSegment>> getMessagesToDelete(List<EmbeddingMatch<TextSegment>> sessionMatches) {
        return memorySelectionPolicy.selectMessagesToDelete(
                sessionMatches,
                KEEP_RECENT_COUNT,
                IMPORTANCE_THRESHOLD
        );
    }
}


package com.example.aihelper.memory;

import com.example.aihelper.utils.MemoryUtils;
import dev.langchain4j.data.message.ChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 记忆持久化策略。
 */
@Slf4j
@Service
public class MemoryPersistenceStrategy {

    private final ChromaMemoryRepository chromaMemoryRepository;
    private final MemoryImportanceScoringStrategy memoryImportanceScoringStrategy;

    public MemoryPersistenceStrategy(ChromaMemoryRepository chromaMemoryRepository,
                                     MemoryImportanceScoringStrategy memoryImportanceScoringStrategy) {
        this.chromaMemoryRepository = chromaMemoryRepository;
        this.memoryImportanceScoringStrategy = memoryImportanceScoringStrategy;
    }
    
    /**
     * 保存消息列表到 Chroma
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param messages 消息列表
     */
    @Transactional
    public void saveMessages(String userId, String sessionId, List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("[memory-persistence] event=skipped reason=empty_messages sessionId={}", sessionId);
            return;
        }
        
        log.info("[memory-persistence] event=start userId={} sessionId={} messageCount={}",
            userId, sessionId, messages.size());
        
        int successCount = 0;
        for (ChatMessage message : messages) {
            try {
                // 1. 提取消息内容和角色
                String role = MemoryUtils.determineRole(message);
                String content = MemoryUtils.extractContent(message);
                
                // 2. 计算重要性评分
                BigDecimal importanceScore = memoryImportanceScoringStrategy.calculateImportance(
                    content, "USER".equals(role));
                
                // 3. 保存到 Chroma
                chromaMemoryRepository.saveMessage(userId, sessionId, role, content, importanceScore.doubleValue());
                
                successCount++;
                log.debug("[memory-persistence] event=item_saved sessionId={} importance={}", sessionId, importanceScore);
                
            } catch (Exception e) {
                log.error("[memory-persistence] event=item_error sessionId={}", sessionId, e);
            }
        }
        
        log.info("[memory-persistence] event=completed sessionId={} successCount={} totalCount={}",
                sessionId, successCount, messages.size());
    }
}

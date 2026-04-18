package com.example.aihelper.service.impl;

import com.example.aihelper.memory.ChromaMemoryRepository;
import com.example.aihelper.memory.MemoryForgettingStrategy;
import com.example.aihelper.memory.MemoryLoadingStrategy;
import com.example.aihelper.memory.MemoryPersistenceStrategy;
import com.example.aihelper.service.ChatMemoryService;
import com.example.aihelper.service.ChatMemoryRuntimeService;
import com.example.aihelper.utils.MemoryUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 聊天记忆服务实现（统一入口）。
 *
 * 负责记忆加载、持久化、删除与遗忘的流程协调。
 */
@Slf4j
@Service
public class ChatMemoryServiceImpl implements ChatMemoryService, ChatMemoryRuntimeService {
    private static final String DEFAULT_USER_ID = "default_user";
    private static final String ROLE_USER = "USER";
    private static final String ROLE_USER_OUTPUT = "user";
    private static final String ROLE_ASSISTANT_OUTPUT = "assistant";

    private final MemoryLoadingStrategy memoryLoadingStrategy;
    private final MemoryPersistenceStrategy memoryPersistenceStrategy;
    private final MemoryForgettingStrategy memoryForgettingStrategy;
    private final ChromaMemoryRepository chromaMemoryRepository;

    public ChatMemoryServiceImpl(MemoryLoadingStrategy memoryLoadingStrategy,
                                 MemoryPersistenceStrategy memoryPersistenceStrategy,
                                 MemoryForgettingStrategy memoryForgettingStrategy,
                                 ChromaMemoryRepository chromaMemoryRepository) {
        this.memoryLoadingStrategy = memoryLoadingStrategy;
        this.memoryPersistenceStrategy = memoryPersistenceStrategy;
        this.memoryForgettingStrategy = memoryForgettingStrategy;
        this.chromaMemoryRepository = chromaMemoryRepository;
    }
    
    // 内存缓存活跃的 ChatMemory
    private final Map<Integer, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    
    // 追踪每个会话已持久化的消息数量（防止重复保存）
    private final Map<Integer, Integer> persistedMessageCount = new ConcurrentHashMap<>();
    // 追踪每个会话已持久化的消息签名（适配窗口滚动，避免 skip 丢消息）
    private final Map<Integer, Set<String>> persistedMessageSignatures = new ConcurrentHashMap<>();
    
    // 默认窗口大小（保留最近的消息数量）
    private static final int DEFAULT_MAX_MESSAGES = 50;
    
    // ==================== 记忆加载 ====================
    
    /**
     * 加载历史记忆到 ChatMemory
     * 
     * @param memoryId 会话ID
     * @param userMessage 用户当前消息（用于语义检索，预留）
     * @return 加载的记忆列表
     */
    @Override
    public List<ChatMessage> loadMemories(int memoryId, String userMessage) {
        try {
            log.debug("[chat-memory] event=load_start memoryId={}", memoryId);
            String sessionId = toSessionId(memoryId);
            
            // 从 Chroma 查询所有记忆
            var allMatches = chromaMemoryRepository.findBySessionId(sessionId);
            
            // 执行加载策略
            return memoryLoadingStrategy.execute(allMatches, sessionId);
            
        } catch (Exception e) {
            log.error("[chat-memory] event=load_error memoryId={}", memoryId, e);
            return List.of();
        }
    }
    
    // ==================== 记忆持久化 ====================
    
    /**
     * 持久化新消息到 Chroma
     * 
     * @param memoryId 会话ID
     */
    @Override
    public void persistNewMessages(int memoryId) {
        try {
            String sessionId = toSessionId(memoryId);
            ChatMemory memory = memoryCache.get(memoryId);
            if (memory == null) {
                log.warn("[chat-memory] event=persist_skipped reason=memory_missing memoryId={}", memoryId);
                return;
            }
            
            List<ChatMessage> allMessages = memory.messages();
            if (allMessages.isEmpty()) {
                log.debug("[chat-memory] event=persist_skipped reason=no_messages memoryId={}", memoryId);
                return;
            }
            
            int alreadyPersisted = persistedMessageCount.getOrDefault(memoryId, 0);
            Set<String> signatureCache = persistedMessageSignatures.computeIfAbsent(memoryId, key -> ConcurrentHashMap.newKeySet());

            // 基于消息签名做增量检测，避免窗口滚动后出现 skip 丢消息
            List<ChatMessage> newMessages = allMessages.stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .filter(msg -> signatureCache.add(buildMessageSignature(msg)))
                    .toList();
            
            if (!newMessages.isEmpty()) {
                log.info("[chat-memory] event=persist_start memoryId={} totalMessages={} persistedCount={} newMessages={}",
                    memoryId, allMessages.size(), alreadyPersisted, newMessages.size());
                
                memoryPersistenceStrategy.saveMessages(DEFAULT_USER_ID, sessionId, newMessages);
                persistedMessageCount.put(memoryId, Math.max(alreadyPersisted, allMessages.size()));
                log.info("[chat-memory] event=persist_completed memoryId={} newMessages={} totalMessages={}",
                    memoryId, newMessages.size(), allMessages.size());
            } else {
                log.debug("[chat-memory] event=persist_skipped reason=no_new_messages memoryId={} totalMessages={}",
                        memoryId, allMessages.size());
            }
        } catch (Exception e) {
            log.error("[chat-memory] event=persist_error memoryId={}", memoryId, e);
        }
    }
    
    // ==================== 记忆删除 ====================
    
    /**
     * 获取指定会话的历史消息（用于前端展示）
     * 
     * @param sessionId 会话ID
     * @return 历史消息列表（包含角色和内容，按时间升序）
     */
    @Override
    public List<Map<String, String>> getHistoryMessages(String sessionId) {
        try {
            log.info("[chat-memory] event=history_start sessionId={}", sessionId);
            
            // 1. 从 Repository 查询
            List<EmbeddingMatch<TextSegment>> allMatches = chromaMemoryRepository.findBySessionId(sessionId);
            
            if (allMatches.isEmpty()) {
                List<Map<String, String>> cacheHistory = getHistoryFromCache(sessionId);
                log.info("[chat-memory] event=history_fallback sessionId={} reason=vector_empty count={}",
                        sessionId, cacheHistory.size());
                return cacheHistory;
            }
            
            List<TextSegment> allSegments = allMatches.stream()
                .map(EmbeddingMatch::embedded)
                .collect(Collectors.toList());
            
            log.debug("[chat-memory] event=history_vector_loaded sessionId={} segmentCount={}", sessionId, allSegments.size());
            
            // 2. 按创建时间升序排序
            List<TextSegment> sortedSegments = MemoryUtils.sortSegmentsByTimeAsc(allSegments);
            
            // 3. 转换为 Map 格式
            List<Map<String, String>> historyMessages = sortedSegments.stream()
                .map(this::toHistoryMessage)
                .collect(Collectors.toList());
            
            log.info("[chat-memory] event=history_completed sessionId={} messageCount={}", sessionId, historyMessages.size());
            return historyMessages;
            
        } catch (Exception e) {
            log.error("[chat-memory] event=history_error sessionId={}", sessionId, e);
            return getHistoryFromCache(sessionId);
        }
    }
    
    /**
     * 清除指定会话的所有记忆（包括内存和 Chroma）
     * 
     * @param sessionId 会话ID
     */
    @Override
    public void clearMemories(String sessionId) {
        try {
            int memoryId = Integer.parseInt(sessionId);
            
            log.info("[chat-memory] event=clear_start sessionId={} memoryId={}", sessionId, memoryId);
            
            // 1. 清除内存缓存
            ChatMemory removed = memoryCache.remove(memoryId);
            if (removed != null) {
                log.debug("[chat-memory] event=clear_cache_removed memoryId={}", memoryId);
            }
            
            // 2. 清除持久化计数
            Integer count = persistedMessageCount.remove(memoryId);
            log.debug("[chat-memory] event=clear_counter_removed memoryId={} previousCount={}", memoryId, count);
            persistedMessageSignatures.remove(memoryId);
            
            // 3. 清除 Chroma 中的记录
            Filter filter = metadataKey("session_id").isEqualTo(sessionId);
            chromaMemoryRepository.deleteByFilter(filter);
            
            log.info("[chat-memory] event=clear_completed sessionId={}", sessionId);
            
        } catch (NumberFormatException e) {
            log.error("[chat-memory] event=clear_error sessionId={} reason=invalid_session_id", sessionId, e);
            throw new IllegalArgumentException("无效的 sessionId: " + sessionId, e);
        } catch (Exception e) {
            log.error("[chat-memory] event=clear_error sessionId={}", sessionId, e);
            throw new RuntimeException("清除记忆失败: " + e.getMessage(), e);
        }
    }
    
    // ==================== 记忆遗忘 ====================
    
    /**
     * 定时遗忘策略 - 每6小时执行一次
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    @Transactional
    @Override
    public void forgetOldMemories() {
        log.info("[chat-memory] event=forget_start");
        
        try {
            // 1. 查询所有活跃记忆
            var allMatches = chromaMemoryRepository.findAllActive();
            
            // 2. 执行遗忘策略
            int totalDeleted = memoryForgettingStrategy.execute(allMatches);
            
            // 3. 实际删除标记的记忆
            deleteMarkedMemories(allMatches);
            
            log.info("[chat-memory] event=forget_completed deletedCount={}", totalDeleted);
        } catch (Exception e) {
            log.error("[chat-memory] event=forget_error", e);
        }
    }
    
    /**
     * 删除被遗忘策略标记的记忆
     */
    private void deleteMarkedMemories(List<EmbeddingMatch<TextSegment>> allMatches) {
        // 按 session_id 分组
        Map<String, List<EmbeddingMatch<TextSegment>>> sessionsMap = allMatches.stream()
            .collect(Collectors.groupingBy(
                match -> match.embedded().metadata().getString("session_id")
            ));
        
        for (Map.Entry<String, List<EmbeddingMatch<TextSegment>>> entry : sessionsMap.entrySet()) {
            List<EmbeddingMatch<TextSegment>> sessionMatches = entry.getValue();
            
            // 获取应该删除的记忆
            List<EmbeddingMatch<TextSegment>> toDelete = memoryForgettingStrategy.getMessagesToDelete(sessionMatches);
            
            // 执行删除
            for (var match : toDelete) {
                String chromaId = match.embeddingId();
                if (chromaId != null && !chromaId.isEmpty()) {
                    try {
                        chromaMemoryRepository.deleteById(chromaId);
                    } catch (Exception e) {
                        log.warn("[chat-memory] event=forget_delete_failed chromaId={}", chromaId, e);
                    }
                }
            }
        }
    }
    
    // ==================== ChatMemory 管理 ====================
    
    /**
     * 获取或创建 ChatMemory
     * 
     * @param memoryId 会话ID
     * @return ChatMemory 实例
     */
    @Override
    public ChatMemory getOrCreateMemory(int memoryId) {
        return memoryCache.computeIfAbsent(memoryId, key -> {
            log.info("[chat-memory] event=memory_created memoryId={}", key);
            
            dev.langchain4j.memory.chat.MessageWindowChatMemory memory = 
                dev.langchain4j.memory.chat.MessageWindowChatMemory.builder()
                    .id(String.valueOf(key))
                    .maxMessages(DEFAULT_MAX_MESSAGES)
                    .build();
            
            // 确保持久化计数初始化
            persistedMessageCount.putIfAbsent(key, 0);
            persistedMessageSignatures.putIfAbsent(key, ConcurrentHashMap.newKeySet());
            
            return memory;
        });
    }
    
    /**
     * 更新持久化计数器
     * 
     * @param memoryId 会话ID
     * @param count 已持久化的消息数量
     */
    @Override
    public void updatePersistedCount(int memoryId, int count) {
        persistedMessageCount.put(memoryId, count);
        log.debug("[chat-memory] event=persist_count_updated memoryId={} count={}", memoryId, count);
    }
    
    /**
     * 获取内存缓存中的 ChatMemory
     * 
     * @param memoryId 会话ID
     * @return ChatMemory 实例，如果不存在返回 null
     */
    @Override
    public ChatMemory getMemoryFromCache(int memoryId) {
        return memoryCache.get(memoryId);
    }

    private Map<String, String> toHistoryMessage(TextSegment segment) {
        Map<String, String> message = new HashMap<>();
        String role = segment.metadata().getString("role");
        message.put("role", ROLE_USER.equals(role) ? ROLE_USER_OUTPUT : ROLE_ASSISTANT_OUTPUT);
        message.put("content", segment.text());
        return message;
    }

    private String toSessionId(int memoryId) {
        return String.valueOf(memoryId);
    }
    
    /**
     * 加载历史记忆并填充到 ChatMemory 中
     * 
     * @param memoryId 会话ID
     * @param userMessage 用户当前消息（用于语义检索，预留）
     */
    @Override
    public void loadAndPrepareMemory(int memoryId, String userMessage) {
        var memories = loadMemories(memoryId, userMessage);
        var memory = getOrCreateMemory(memoryId);
        memory.clear();
        log.info("[chat-memory] event=preload_start memoryId={} loadedCount={}", memoryId, memories.size());
        if (!memories.isEmpty()) {
            memories.forEach(memory::add);
            updatePersistedCount(memoryId, memories.size());
            seedPersistedMessageSignatures(memoryId, memories);
            log.info("[chat-memory] event=preload_completed memoryId={} loadedCount={}", memoryId, memories.size());
        } else {
            updatePersistedCount(memoryId, 0);
            persistedMessageSignatures.put(memoryId, ConcurrentHashMap.newKeySet());
            log.info("[chat-memory] event=preload_completed memoryId={} loadedCount=0", memoryId);
        }
    }

    @Override
    public void appendTurnAndPersist(int memoryId, String userMessage, String assistantReply) {
        ChatMemory memory = getOrCreateMemory(memoryId);
        if (userMessage != null && !userMessage.isBlank()) {
            memory.add(UserMessage.from(userMessage));
        }
        if (assistantReply != null && !assistantReply.isBlank()) {
            memory.add(AiMessage.from(assistantReply));
        }
        persistNewMessages(memoryId);
    }

    private void seedPersistedMessageSignatures(int memoryId, List<ChatMessage> messages) {
        Set<String> signatures = messages.stream()
                .filter(msg -> !(msg instanceof SystemMessage))
                .map(this::buildMessageSignature)
                .collect(Collectors.toCollection(HashSet::new));
        persistedMessageSignatures.put(memoryId, ConcurrentHashMap.newKeySet());
        persistedMessageSignatures.get(memoryId).addAll(signatures);
    }

    private String buildMessageSignature(ChatMessage message) {
        String role = MemoryUtils.determineRole(message);
        String content = MemoryUtils.extractContent(message);
        return role + "::" + content;
    }

    private List<Map<String, String>> getHistoryFromCache(String sessionId) {
        try {
            int memoryId = Integer.parseInt(sessionId);
            ChatMemory memory = memoryCache.get(memoryId);
            if (memory == null) {
                return List.of();
            }
            return memory.messages().stream()
                    .filter(msg -> !(msg instanceof SystemMessage))
                    .map(this::toHistoryMessageFromChatMessage)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("[chat-memory] event=history_fallback_error sessionId={}", sessionId, e);
            return List.of();
        }
    }

    private Map<String, String> toHistoryMessageFromChatMessage(ChatMessage message) {
        Map<String, String> mapped = new HashMap<>();
        if (message instanceof UserMessage userMessage) {
            mapped.put("role", ROLE_USER_OUTPUT);
            mapped.put("content", userMessage.singleText());
        } else if (message instanceof AiMessage aiMessage) {
            mapped.put("role", ROLE_ASSISTANT_OUTPUT);
            mapped.put("content", aiMessage.text());
        } else {
            mapped.put("role", ROLE_ASSISTANT_OUTPUT);
            mapped.put("content", MemoryUtils.extractContent(message));
        }
        return mapped;
    }
}

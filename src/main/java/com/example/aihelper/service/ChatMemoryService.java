package com.example.aihelper.service;

import dev.langchain4j.data.message.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * 聊天记忆服务接口。
 */
public interface ChatMemoryService {

    /**
     * 加载指定会话的历史记忆。
     */
    List<ChatMessage> loadMemories(int memoryId, String userMessage);

    /**
     * 持久化当前会话新增消息。
     */
    void persistNewMessages(int memoryId);

    /**
     * 查询会话历史消息（供前端展示）。
     */
    List<Map<String, String>> getHistoryMessages(String sessionId);

    /**
     * 清除指定会话的记忆数据。
     */
    void clearMemories(String sessionId);

    /**
     * 执行定时遗忘策略。
     */
    void forgetOldMemories();

    /**
     * 预加载历史并准备运行时记忆。
     */
    void loadAndPrepareMemory(int memoryId, String userMessage);

    /**
     * 追加一轮对话并触发持久化。
     */
    void appendTurnAndPersist(int memoryId, String userMessage, String assistantReply);
}

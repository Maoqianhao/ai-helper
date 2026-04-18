package com.example.aihelper.service;

import dev.langchain4j.memory.ChatMemory;

/**
 * 运行时 ChatMemory 访问接口。
 */
public interface ChatMemoryRuntimeService {

    /**
     * 获取或创建会话内存实例。
     */
    ChatMemory getOrCreateMemory(int memoryId);

    /**
     * 更新会话已持久化计数。
     */
    void updatePersistedCount(int memoryId, int count);

    /**
     * 从内存缓存读取会话内存实例。
     */
    ChatMemory getMemoryFromCache(int memoryId);
}

package com.example.aihelper.memory;

import com.example.aihelper.service.ChatMemoryRuntimeService;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 自定义聊天记忆提供者。
 */
@Slf4j
@Component
public class CustomChatMemoryProvider implements ChatMemoryProvider {

    private final ChatMemoryRuntimeService chatMemoryRuntimeService;

    public CustomChatMemoryProvider(ChatMemoryRuntimeService chatMemoryRuntimeService) {
        this.chatMemoryRuntimeService = chatMemoryRuntimeService;
    }
    
    /**
     * 获取或创建 ChatMemory（实现 ChatMemoryProvider 接口）
     * 
     * @param memoryId 会话ID
     * @return ChatMemory 实例
     */
    @Override
    public ChatMemory get(Object memoryId) {
        int id = Integer.parseInt(memoryId.toString());
        log.debug("[chat-memory-provider] event=get memoryId={}", id);
        return chatMemoryRuntimeService.getOrCreateMemory(id);
    }
}

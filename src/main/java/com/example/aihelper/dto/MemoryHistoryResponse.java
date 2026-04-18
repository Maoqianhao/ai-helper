package com.example.aihelper.dto;

import java.util.List;
import java.util.Map;

/**
 * 会话历史消息响应。
 */
public record MemoryHistoryResponse(
        boolean success,
        String sessionId,
        List<Map<String, String>> messages,
        int count
) {
    /**
     * 根据会话消息构造成功响应。
     */
    public static MemoryHistoryResponse of(String sessionId, List<Map<String, String>> messages) {
        return new MemoryHistoryResponse(true, sessionId, messages, messages.size());
    }
}

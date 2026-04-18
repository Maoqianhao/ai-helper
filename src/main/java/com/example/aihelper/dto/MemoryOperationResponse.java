package com.example.aihelper.dto;

/**
 * 记忆操作通用响应。
 */
public record MemoryOperationResponse(
        boolean success,
        String message,
        String sessionId
) {
    /**
     * 构造成功响应。
     */
    public static MemoryOperationResponse success(String message, String sessionId) {
        return new MemoryOperationResponse(true, message, sessionId);
    }
}

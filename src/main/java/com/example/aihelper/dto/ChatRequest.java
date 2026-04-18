package com.example.aihelper.dto;

/**
 * 聊天请求模型（用于统一参数校验与日志摘要）
 */
public record ChatRequest(int memoryId, String message) {

    public static ChatRequest of(int memoryId, String message) {
        String normalizedMessage = message == null ? null : message.trim();
        ChatRequest request = new ChatRequest(memoryId, normalizedMessage);
        request.validate();
        return request;
    }

    public void validate() {
        if (memoryId <= 0) {
            throw new IllegalArgumentException("memoryId 必须大于 0");
        }
        if (message == null || message.isEmpty()) {
            throw new IllegalArgumentException("message 不能为空");
        }
    }

    public String messageSummary(int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}

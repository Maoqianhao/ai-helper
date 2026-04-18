package com.example.aihelper.dto;

/**
 * 会话概要数据传输对象。
 *
 * @param sessionId 会话ID
 * @param title 会话标题
 */
public record ChatSessionDto(long sessionId, String title) {
}

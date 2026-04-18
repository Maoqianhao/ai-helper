package com.example.aihelper.dto;

/**
 * 创建会话请求体。
 *
 * @param title 期望创建的会话标题
 */
public record CreateSessionRequest(String title) {
}

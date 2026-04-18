package com.example.aihelper.dto;

/**
 * 统一错误响应。
 */
public record ApiErrorResponse(
        boolean success,
        String error,
        String message
) {
    /**
     * 构造统一错误响应。
     */
    public static ApiErrorResponse of(String error, String message) {
        return new ApiErrorResponse(false, error, message);
    }
}

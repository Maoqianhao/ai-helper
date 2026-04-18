package com.example.aihelper.dto;

public record AuthResponse(boolean success, String token, long userId, String username, String message) {

    public static AuthResponse success(String token, long userId, String username) {
        return new AuthResponse(true, token, userId, username, "ok");
    }

    public static AuthResponse failure(String message) {
        return new AuthResponse(false, null, 0L, null, message);
    }
}

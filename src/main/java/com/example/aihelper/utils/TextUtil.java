package com.example.aihelper.utils;

/**
 * 文本工具类
 */
public final class TextUtil {

    private TextUtil() {
    }

    /**
     * 文本截断，限制日志 IO
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || maxLength <= 0) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

package com.example.aihelper.utils;

/**
 * 文本工具类。
 */
public final class TextUtils {

    private TextUtils() {
    }

    /**
     * 截断文本，限制日志输出长度。
     */
    public static String truncate(String text, int maxLength) {
        if (text == null || maxLength <= 0) {
            return "";
        }
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}

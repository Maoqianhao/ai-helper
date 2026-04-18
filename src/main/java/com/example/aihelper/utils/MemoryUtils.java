package com.example.aihelper.utils;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;

import java.util.List;

/**
 * 记忆工具类
 * 
 * 职责：提供记忆相关的通用工具方法
 */
public class MemoryUtils {
    
    /**
     * 按创建时间降序排序 TextSegment（最新的在前）
     * 
     * @param segments 待排序的 TextSegment 列表
     * @return 按时间降序排序后的列表
     */
    public static List<TextSegment> sortSegmentsByTimeDesc(List<TextSegment> segments) {
        return segments.stream()
            .sorted((s1, s2) -> {
                String time1 = s1.metadata().getString("created_at");
                String time2 = s2.metadata().getString("created_at");
                return time2.compareTo(time1);
            })
            .toList();
    }
    
    /**
     * 按创建时间升序排序 TextSegment（最旧的在前）
     * 
     * @param segments 待排序的 TextSegment 列表
     * @return 按时间升序排序后的列表
     */
    public static List<TextSegment> sortSegmentsByTimeAsc(List<TextSegment> segments) {
        return segments.stream()
            .sorted((s1, s2) -> {
                String time1 = s1.metadata().getString("created_at");
                String time2 = s2.metadata().getString("created_at");
                return time1.compareTo(time2);
            })
            .toList();
    }
    
    /**
     * 按创建时间降序排序 EmbeddingMatch（最新的在前）
     * 
     * @param matches 待排序的 EmbeddingMatch 列表
     * @return 按时间降序排序后的列表
     */
    public static List<EmbeddingMatch<TextSegment>> sortMatchesByTimeDesc(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
            .sorted((m1, m2) -> {
                String time1 = m1.embedded().metadata().getString("created_at");
                String time2 = m2.embedded().metadata().getString("created_at");
                return time2.compareTo(time1);
            })
            .toList();
    }
    
    /**
     * 将 TextSegment 转换为 ChatMessage
     * 
     * @param segment TextSegment
     * @return ChatMessage
     */
    public static ChatMessage convertToChatMessage(TextSegment segment) {
        String role = segment.metadata().getString("role");
        String content = segment.text();
        
        if ("USER".equals(role)) {
            return UserMessage.from(content);
        } else if ("ASSISTANT".equals(role)) {
            return AiMessage.from(content);
        } else {
            return UserMessage.from(content);
        }
    }
    
    /**
     * 判断消息角色
     * 
     * @param message ChatMessage
     * @return 角色字符串（USER/ASSISTANT/SYSTEM）
     */
    public static String determineRole(ChatMessage message) {
        if (message instanceof UserMessage) {
            return "USER";
        } else if (message instanceof AiMessage) {
            return "ASSISTANT";
        } else {
            return "SYSTEM";
        }
    }
    
    /**
     * 提取消息内容
     * 
     * @param message ChatMessage
     * @return 消息文本内容
     */
    public static String extractContent(ChatMessage message) {
        if (message instanceof UserMessage userMsg) {
            return userMsg.singleText();
        } else if (message instanceof AiMessage aiMsg) {
            return aiMsg.text();
        } else {
            return message.toString();
        }
    }
}

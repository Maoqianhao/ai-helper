package com.example.aihelper.memory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 重要性评分服务。
 */
@Slf4j
@Service
public class MemoryImportanceScoringStrategy {
    
    /**
     * 计算消息重要性评分
     * 
     * @param content 消息内容
     * @param isUserMessage 是否为用户消息
     * @return 重要性评分（0.0 - 1.0）
     */
    public BigDecimal calculateImportance(String content, boolean isUserMessage) {
        // 维度 1：消息长度（权重 0.3）
        double lengthScore = calculateLengthScore(content);
        
        // 维度 2：语义复杂度（权重 0.4）
        double semanticScore = calculateSemanticScore(content);
        
        // 维度 3：关键字匹配（权重 0.3）
        double keywordScore = calculateKeywordScore(content);
        
        // 加权计算
        double finalScore = (lengthScore * 0.3) + (semanticScore * 0.4) + (keywordScore * 0.3);
        
        // 用户消息加分
        if (isUserMessage) {
            finalScore = Math.min(1.0, finalScore + 0.1);
        }
        
        // 限制在 [0.0, 1.0] 范围内
        finalScore = Math.max(0.0, Math.min(1.0, finalScore));
        
        log.debug("[memory-importance] event=scored length={} semantic={} keyword={} final={}",
            String.format("%.2f", lengthScore),
            String.format("%.2f", semanticScore),
            String.format("%.2f", keywordScore),
            String.format("%.2f", finalScore));
        
        return new BigDecimal(String.format("%.2f", finalScore));
    }
    
    /**
     * 维度 1：消息长度评分
     * 
     * @param content 消息内容
     * @return 长度评分（0.0 - 1.0）
     */
    private double calculateLengthScore(String content) {
        int charCount = content.length();
        
        if (charCount < 10) {
            return 0.1;
        } else if (charCount < 50) {
            return 0.5;
        } else if (charCount < 200) {
            return 0.8;
        } else {
            return 1.0;
        }
    }
    
    /**
     * 维度 2：语义复杂度评分
     * 
     * @param content 消息内容
     * @return 语义复杂度评分（0.0 - 1.0）
     */
    private double calculateSemanticScore(String content) {
        double score = 0.3; // 基础分
        String lowerContent = content.toLowerCase();
        
        // 问题特征（+0.2）
        if (content.contains("?") || content.contains("？") || 
            lowerContent.contains("吗") || lowerContent.contains("呢") || 
            lowerContent.contains("什么") || lowerContent.contains("怎么") ||
            lowerContent.contains("为什么") || lowerContent.contains("如何")) {
            score += 0.2;
        }
        
        // 技术术语（+0.2）
        if (lowerContent.matches(".*[a-z]{3,}.*") ||
            lowerContent.contains("java") || lowerContent.contains("spring") ||
            lowerContent.contains("mysql") || lowerContent.contains("redis")) {
            score += 0.2;
        }
        
        // 逻辑连接词（+0.1）
        if (lowerContent.contains("因为") || lowerContent.contains("所以") ||
            lowerContent.contains("但是") || lowerContent.contains("虽然") ||
            lowerContent.contains("如果") || lowerContent.contains("那么")) {
            score += 0.1;
        }
        
        // 句子数量（最多 +0.3）
        int sentenceCount = content.split("[。！？;；]").length;
        score += Math.min(0.3, sentenceCount * 0.1);
        
        return Math.min(1.0, score);
    }
    
    /**
     * 维度 3：关键字匹配评分
     * 
     * @param content 消息内容
     * @return 关键字评分（0.0 - 1.0）
     */
    private double calculateKeywordScore(String content) {
        double score = 0.5; // 基础分
        String lowerContent = content.toLowerCase();
        
        // 高价值关键词
        String[] highValueKeywords = {"重要", "关键", "记住", "注意", "必须", "核心", "重点", "务必"};
        for (String keyword : highValueKeywords) {
            if (lowerContent.contains(keyword)) {
                return 1.0;
            }
        }
        
        // 中价值关键词
        String[] midValueKeywords = {"希望", "想要", "需要", "建议", "请问", "能否"};
        for (String keyword : midValueKeywords) {
            if (lowerContent.contains(keyword)) {
                score = 0.8;
                break;
            }
        }
        
        // 低价值关键词（降低评分）
        String[] lowValueKeywords = {"你好", "谢谢", "好的", "再见", "嗯嗯", "哈哈", "哦哦"};
        for (String keyword : lowValueKeywords) {
            if (lowerContent.contains(keyword)) {
                score = Math.min(score, 0.3);
                break;
            }
        }
        
        return score;
    }
}

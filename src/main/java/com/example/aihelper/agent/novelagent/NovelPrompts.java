package com.example.aihelper.agent.novelagent;

import com.example.aihelper.utils.TextUtil;
import com.example.aihelper.prompt.PromptTemplateLoader;

/**
 * 小说创作提示词模板
 * 
 * 职责：统一管理所有提示词模板，避免硬编码
 */
public class NovelPrompts {
    private static final String ANALYSIS_TEMPLATE_PATH = "templates/novel/analysis_prompt.txt";
    private static final String FINAL_APPROVAL_TEMPLATE_PATH = "templates/novel/final_approval_prompt.txt";
    
    // ==================== 标记常量 ====================
    public static final String DRAFT_COMPLETE = "DRAFT_COMPLETE";
    public static final String AUDIENCE_READY = "AUDIENCE_READY";
    public static final String STYLE_READY = "STYLE_READY";
    public static final String FINAL_READY = "FINAL_READY";
    public static final String APPROVED = "APPROVED";
    public static final String ANALYSIS_COMPLETE = "ANALYSIS_COMPLETE";
    public static final String EDIT_READY = "EDIT_READY";
    
    // ==================== 信息提取模板 ====================
    
    /**
     * 从用户消息中提取创作信息的提示词
     */
    public static String buildExtractionPrompt(String userMessage) {
        return """
            请从以下用户消息中提取小说创作的关键信息：
            
            用户消息: %s
            
            请提取以下信息并以 JSON 格式返回：
            {
              "topic": "小说主题/核心创意（必填）",
              "audience": "目标受众（可选，默认为'普通读者'）",
              "style": "写作风格（可选，默认为'标准风格'）"
            }
            
            如果用户没有明确说明某些信息，请根据上下文合理推断或使用默认值。
            只返回 JSON，不要有其他内容。
            """.formatted(userMessage);
    }
    
    // ==================== 通用模板 ====================
    
    /**
     * 评估并改进模板
     */
    public static String buildEvaluationPrompt(
            String taskName,
            String content,
            String context,
            String completeMarker) {
        return """
            请评估%s的质量：
            
            当前内容: %s
            上下文信息: %s
            
            评估标准：
            1. 是否符合要求？(1-10 分)
            2. 是否需要改进？
            
            如果满意，输出"%s"；否则指出需要改进的地方。
            """.formatted(taskName, truncate(content), context, completeMarker);
    }
    
    /**
     * 改进行动模板
     */
    public static String buildImprovementPrompt(
            String taskName,
            String feedback,
            String currentContent) {
        return """
            根据以下意见改进%s：
            
            改进意见: %s
            当前版本: %s
            
            请直接输出改进后的内容。
            """.formatted(taskName, truncate(feedback), truncate(currentContent));
    }
    
    /**
     * 就绪检查模板
     */
    public static String buildReadinessPrompt(
            String itemName,
            String content,
            String criteria,
            String readyMarker) {
        return """
            请检查%s是否准备就绪：
            
            内容: %s
            
            检查标准: %s
            
            如果准备就绪，输出"%s"；否则说明原因。
            """.formatted(itemName, truncate(content), criteria, readyMarker);
    }
    
    // ==================== 各阶段专用模板 ====================
    
    /**
     * 需求分析提示词
     */
    public static String buildAnalysisPrompt(String topic, String audience, String style) {
        String template = PromptTemplateLoader.load(ANALYSIS_TEMPLATE_PATH, """
            请分析以下小说创作需求：
            - 主题: %s
            - 目标受众: %s
            - 写作风格: %s
            
            请简要分析创作要点，然后输出"%s"表示分析完成。
            """);
        return template.formatted(topic, audience, style, ANALYSIS_COMPLETE);
    }
    
    /**
     * 草稿评估提示词
     */
    public static String buildDraftEvaluationPrompt(String draft, String topic) {
        return buildEvaluationPrompt(
            "当前草稿",
            draft,
            "主题: " + topic,
            DRAFT_COMPLETE
        );
    }
    
    /**
     * 草稿改进提示词
     */
    public static String buildDraftImprovementPrompt(String reasoning, String draft) {
        return buildImprovementPrompt("草稿", reasoning, draft);
    }
    
    /**
     * 草稿就绪检查
     */
    public static String buildDraftReadinessPrompt(String draft) {
        return buildReadinessPrompt(
            "草稿",
            draft,
            "1. 故事是否完整?\n2. 是否可以进入受众编辑环节?",
            EDIT_READY
        );
    }
    
    /**
     * 受众适配评估
     */
    public static String buildAudienceFitPrompt(String story, String audience) {
        return """
            当前编辑版本是否适合%s受众？
            
            当前版本: %s
            
            如果适合，输出"%s"；否则指出需要改进的地方。
            """.formatted(audience, truncate(story), AUDIENCE_READY);
    }
    
    /**
     * 受众适配改进
     */
    public static String buildAudienceImprovementPrompt(String audience, String reasoning, String story) {
        return """
            根据以下意见改进故事以更好地适应%s受众：
            
            改进意见: %s
            当前版本: %s
            
            请直接输出改进后的故事。
            """.formatted(audience, truncate(reasoning), truncate(story));
    }
    
    /**
     * 风格适配评估
     */
    public static String buildStyleFitPrompt(String story, String style) {
        return """
            当前版本是否符合%s风格？
            
            当前版本: %s
            
            如果符合，输出"%s"；否则指出需要改进的地方。
            """.formatted(style, truncate(story), FINAL_READY);
    }
    
    /**
     * 风格改进
     */
    public static String buildStyleImprovementPrompt(String style, String reasoning, String story) {
        return """
            根据以下意见调整为%s风格：
            
            改进意见: %s
            当前版本: %s
            
            请直接输出调整后的故事。
            """.formatted(style, truncate(reasoning), truncate(story));
    }
    
    /**
     * 最终审定
     */
    public static String buildFinalApprovalPrompt(String story) {
        String template = PromptTemplateLoader.load(FINAL_APPROVAL_TEMPLATE_PATH, """
            请对这部小说进行最终审定：
            
            小说内容: %s
            
            审定标准：
            1. 故事是否完整且有吸引力？
            2. 是否符合目标受众？
            3. 是否符合指定风格？
            
            如果通过审定，输出"%s"；否则指出最后需要修改的地方。
            """);
        return template.formatted(truncate(story, 800), APPROVED);
    }
    
    // ==================== 辅助方法 ====================
    
    private static String truncate(String text) {
        return TextUtil.truncate(text, 500);
    }
    
    private static String truncate(String text, int maxLength) {
        return TextUtil.truncate(text, maxLength);
    }
}

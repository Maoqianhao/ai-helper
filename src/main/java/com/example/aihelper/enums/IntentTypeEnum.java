package com.example.aihelper.enums;

/**
 * 意图类型枚举
 * 
 * 定义系统中支持的所有意图类型及其描述
 */
public enum IntentTypeEnum {
    
    /**
     * 小说创作意图
     */
    NOVEL_CREATION(
        "小说创作",
        """
        用户明确要求创作虚构文学作品，包括但不限于：
        - 写小说、创作小说、编故事、写故事
        - 创作章节、连载故事
        - 指定题材的创作：科幻、奇幻、武侠、言情、悬疑、恐怖等
        - "帮我写一个关于...的故事/小说"
        - "创作一个...题材的小说"
        
        注意：
        - 询问已有作品（如"推荐一本好小说"）不属于此意图
        - 必须是创作新内容的请求
        """
    ),
    
    /**
     * 旅行规划意图
     */
    TRAVEL_PLANNING(
        "旅行规划",
        """
        用户要求制定旅行计划、行程安排，包括但不限于：
        - 规划旅行路线
        - 制定行程表
        - 安排多天旅行计划
        - "帮我规划一个...的旅行"
        - "设计一个...天的行程"
        """
    ),
    
    /**
     * 数据分析意图
     */
    DATA_ANALYSIS(
        "数据分析",
        """
        用户要求分析数据、生成报告，包括但不限于：
        - 分析销售数据
        - 生成统计报告
        - 数据可视化建议
        - "分析一下...的数据"
        - "帮我看看...的趋势"
        """
    );
    
    private final String name;
    private final String definition;
    
    IntentTypeEnum(String name, String definition) {
        this.name = name;
        this.definition = definition;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDefinition() {
        return definition;
    }
}

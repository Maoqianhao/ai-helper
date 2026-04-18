package com.example.aihelper.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小说创作状态枚举。
 */
public enum NovelCreationStateEnum {
    INIT("初始化", true),
    ANALYZING("分析需求", true),
    DRAFTING("创作草稿", true),
    EVALUATING_DRAFT("评估草稿", true),
    EDITING_AUDIENCE("受众编辑", true),
    EVALUATING_EDIT("评估编辑", true),
    STYLING("风格调整", true),
    FINALIZING("最终审定", true),
    COMPLETED("完成", false),
    FAILED("失败", false);
    
    private final String description;
    private final boolean hasHandler;
    
    NovelCreationStateEnum(String description, boolean hasHandler) {
        this.description = description;
        this.hasHandler = hasHandler;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean hasHandler() {
        return hasHandler;
    }
    
    /**
     * 获取所有需要注册处理器的状态（排除终止状态）
     * 
     * @return 需要处理器的状态列表
     */
    public static List<NovelCreationStateEnum> getStatesWithHandlers() {
        return Arrays.stream(values())
            .filter(state -> state.hasHandler)
            .collect(Collectors.toList());
    }
    
    @Override
    public String toString() {
        return description;
    }
}

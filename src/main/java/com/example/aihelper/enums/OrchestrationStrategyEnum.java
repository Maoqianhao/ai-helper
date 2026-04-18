package com.example.aihelper.enums;

/**
 * 智能体编排策略枚举
 * 用户可选择的主策略模式
 */
public enum OrchestrationStrategyEnum {
    
    /**
     * 路由策略 - 适用于简单意图
     * 特点：快速响应、低成本、单专家调用
     */
    ROUTING("路由策略"),
    
    /**
     * 编排者策略 - 适用于复杂意图
     * 特点：多步骤推理、多专家协作、状态机+ReAct
     */
    ORCHESTRATOR("编排者策略");
    
    private final String description;
    
    OrchestrationStrategyEnum(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}

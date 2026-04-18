package com.example.aihelper.service.strategy;

import com.example.aihelper.enums.OrchestrationStrategyEnum;
import reactor.core.publisher.Flux;

/**
 * 智能体编排策略接口（策略层）。
 */
public interface OrchestrationStrategy {
    /**
     * 当前策略所属的枚举类型，用于工厂自动注册。
     */
    OrchestrationStrategyEnum strategyType();

    /**
     * 是否支持处理当前输入，默认支持。
     */
    default boolean supports(String userMessage) {
        return true;
    }
    
    /**
     * 执行流式对话。
     * 
     * @param memoryId 会话ID
     * @param userMessage 用户消息
     * @return SSE流式响应
     */
    Flux<String> execute(int memoryId, String userMessage);
}

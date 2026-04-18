package com.example.aihelper.service;

import com.example.aihelper.enums.OrchestrationStrategyEnum;
import reactor.core.publisher.Flux;

/**
 * 智能体编排服务接口（业务层）。
 */
public interface AgentOrchestrationService {

    /**
     * 执行智能体协作任务（流式输出）。
     *
     * @param memoryId 会话ID
     * @param userMessage 用户消息
     * @param strategy 用户选择的策略
     * @return SSE流式响应
     */
    Flux<String> executeStream(int memoryId, String userMessage, OrchestrationStrategyEnum strategy);
}

package com.example.aihelper.agent.routeragent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * 路由协调器接口（主智能体）。
 */
public interface RouterCoordinator {
    /**
     * 按会话维度执行路由并返回流式回复。
     */
    @SystemMessage(fromResource = "templates/systemmessage/router_expert_system_message.txt")
    Flux<String> chatStream(@MemoryId int memoryId, @UserMessage String userMessage);
}

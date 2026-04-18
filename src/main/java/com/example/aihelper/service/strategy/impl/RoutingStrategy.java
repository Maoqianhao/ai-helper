package com.example.aihelper.service.strategy.impl;

import com.example.aihelper.agent.routeragent.RouterCoordinator;
import com.example.aihelper.enums.OrchestrationStrategyEnum;
import com.example.aihelper.service.strategy.OrchestrationStrategy;
import com.example.aihelper.utils.TraceIdUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 路由策略实现（快速模式）。
 */
@Slf4j
@Component("routingStrategy")
public class RoutingStrategy implements OrchestrationStrategy {
    private static final int LOG_MESSAGE_MAX_LENGTH = 120;

    private final RouterCoordinator routerCoordinator;

    public RoutingStrategy(RouterCoordinator routerCoordinator) {
        this.routerCoordinator = routerCoordinator;
    }
    
    @Override
    public OrchestrationStrategyEnum strategyType() {
        return OrchestrationStrategyEnum.ROUTING;
    }

    @Override
    public Flux<String> execute(int memoryId, String userMessage) {
        String traceId = TraceIdUtil.getTraceId();
        long startTime = System.currentTimeMillis();
        String messageSummary = summarize(userMessage, LOG_MESSAGE_MAX_LENGTH);
        log.info("[traceId={}] [routing] event=start memoryId={} message={}", traceId, memoryId, messageSummary);
        return routerCoordinator.chatStream(memoryId, userMessage)
                .doOnComplete(() -> log.info("[traceId={}] [routing] event=completed memoryId={} latencyMs={}",
                        traceId, memoryId, System.currentTimeMillis() - startTime))
                .doOnError(error -> log.error("[traceId={}] [routing] event=error memoryId={} latencyMs={} errorType={} message={}",
                        traceId, memoryId, System.currentTimeMillis() - startTime,
                        error.getClass().getSimpleName(), error.getMessage(), error));
    }

    private String summarize(String message, int maxLength) {
        if (message == null) {
            return "";
        }
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}

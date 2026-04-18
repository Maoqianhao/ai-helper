package com.example.aihelper.service.impl;

import com.example.aihelper.enums.OrchestrationStrategyEnum;
import com.example.aihelper.service.AgentOrchestrationService;
import com.example.aihelper.service.strategy.OrchestrationStrategy;
import com.example.aihelper.service.strategy.OrchestrationStrategyFactory;
import com.example.aihelper.utils.TraceIdUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;

/**
 * 智能体编排服务实现。
 *
 * 职责：
 * 1) 根据策略枚举选择并执行编排策略
 * 2) 处理不支持场景下的降级与澄清逻辑
 */
@Slf4j
@Service
public class AgentOrchestrationServiceImpl implements AgentOrchestrationService {
    private static final String FALLBACK_NOTICE = "ℹ️ 已自动切换到快速模式处理该请求。\n";
    private static final String NOVEL_CLARIFY_MESSAGE = "我检测到你可能有小说创作意图。请补充主题、目标读者和风格，我会切换到高质量创作模式继续完成。\n";
    private static final List<String> NOVEL_INTENT_HINTS = List.of(
            "小说", "故事", "剧情", "人物", "写作", "创作", "大纲", "章节"
    );

    private final OrchestrationStrategyFactory strategyFactory;

    public AgentOrchestrationServiceImpl(OrchestrationStrategyFactory strategyFactory) {
        this.strategyFactory = strategyFactory;
    }
    
    /**
     * 执行智能体协作任务（流式输出）
     * 
     * @param memoryId 会话ID
     * @param userMessage 用户消息
     * @param strategy 用户选择的策略
     * @return SSE流式响应
     */
    @Override
    public Flux<String> executeStream(int memoryId, String userMessage, OrchestrationStrategyEnum strategy) {
        String traceId = TraceIdUtil.getTraceId();
        log.info("[traceId={}] [orchestration] event=start memoryId={} strategy={}", traceId, memoryId, strategy.name());
        
        // 1. 获取对应策略并执行
        OrchestrationStrategy orchestrationStrategy = strategyFactory.getStrategy(strategy);
        
        // 2. 处理退化逻辑：策略不支持时回退到路由策略
        if (!orchestrationStrategy.supports(userMessage)) {
            // 灰区请求：避免过早降级，先向用户确认创作意图
            if (strategy == OrchestrationStrategyEnum.ORCHESTRATOR && isNovelIntentGrayZone(userMessage)) {
                log.info("[traceId={}] [orchestration-fallback] fallback=false from={} to=NONE reason=novel_intent_gray_zone",
                        traceId, strategy.name());
                return Flux.just(NOVEL_CLARIFY_MESSAGE);
            }

            log.info("[traceId={}] [orchestration-fallback] fallback=true from={} to={} reason=intent_not_supported",
                    traceId, strategy.name(), OrchestrationStrategyEnum.ROUTING.name());
            OrchestrationStrategy fallbackStrategy = strategyFactory.getStrategy(OrchestrationStrategyEnum.ROUTING);
            return Flux.concat(Flux.just(FALLBACK_NOTICE), fallbackStrategy.execute(memoryId, userMessage));
        }

        return orchestrationStrategy.execute(memoryId, userMessage);
    }

    private boolean isNovelIntentGrayZone(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }
        String lowered = userMessage.toLowerCase(Locale.ROOT);
        return NOVEL_INTENT_HINTS.stream().anyMatch(lowered::contains);
    }
}

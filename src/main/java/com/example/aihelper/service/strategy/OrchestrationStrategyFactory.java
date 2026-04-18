package com.example.aihelper.service.strategy;

import com.example.aihelper.enums.OrchestrationStrategyEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 编排策略工厂。
 *
 * 负责在应用启动时注册策略，并在运行时按枚举获取对应实现。
 */
@Slf4j
@Component
public class OrchestrationStrategyFactory {
    
    private final Map<OrchestrationStrategyEnum, OrchestrationStrategy> strategyMap = new EnumMap<>(OrchestrationStrategyEnum.class);
    
    /**
     * 初始化策略映射（基于 Bean 名称自动注册）
     * 
     * @param strategies 所有策略实现（Spring 自动注入）
     */
    public OrchestrationStrategyFactory(List<OrchestrationStrategy> strategies) {
        log.info("[strategy-factory] event=register_start strategyCount={}", strategies.size());
        
        for (OrchestrationStrategy strategy : strategies) {
            OrchestrationStrategyEnum strategyType = strategy.strategyType();
            OrchestrationStrategy existing = strategyMap.put(strategyType, strategy);
            if (existing != null) {
                throw new IllegalStateException("重复策略注册: " + strategyType);
            }
            log.info("[strategy-factory] event=register_success strategy={}", strategyType);
        }
        
        log.info("[strategy-factory] event=register_completed registeredCount={}", strategyMap.size());
    }
    
    /**
     * 根据枚举获取策略
     * 
     * @param strategyEnum 策略枚举
     * @return 对应的策略实现
     */
    public OrchestrationStrategy getStrategy(OrchestrationStrategyEnum strategyEnum) {
        OrchestrationStrategy strategy = strategyMap.get(strategyEnum);
        
        if (strategy == null) {
            log.error("[strategy-factory] strategy not found type={}", strategyEnum);
            throw new IllegalArgumentException("不支持的策略类型: " + strategyEnum);
        }
        
        return strategy;
    }
}

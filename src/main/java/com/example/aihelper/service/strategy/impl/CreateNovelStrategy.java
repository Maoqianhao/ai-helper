package com.example.aihelper.service.strategy.impl;

import com.example.aihelper.agent.IntentClassifierExpert;
import com.example.aihelper.agent.novelagent.NovelCreatorAgent;
import com.example.aihelper.enums.IntentTypeEnum;
import com.example.aihelper.enums.OrchestrationStrategyEnum;
import com.example.aihelper.service.ChatMemoryRuntimeService;
import com.example.aihelper.service.strategy.OrchestrationStrategy;
import com.example.aihelper.utils.TraceIdUtil;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 高质量编排策略实现（小说创作方向）。
 */
@Slf4j
@Component("orchestratorStrategy")
public class CreateNovelStrategy implements OrchestrationStrategy {
    private final NovelCreatorAgent novelCreatorAgent;
    private final IntentClassifierExpert intentClassifierExpert;
    private final ChatMemoryRuntimeService chatMemoryRuntimeService;

    public CreateNovelStrategy(NovelCreatorAgent novelCreatorAgent,
                               IntentClassifierExpert intentClassifierExpert,
                               ChatMemoryRuntimeService chatMemoryRuntimeService) {
        this.novelCreatorAgent = novelCreatorAgent;
        this.intentClassifierExpert = intentClassifierExpert;
        this.chatMemoryRuntimeService = chatMemoryRuntimeService;
    }

    @Override
    public OrchestrationStrategyEnum strategyType() {
        return OrchestrationStrategyEnum.ORCHESTRATOR;
    }

    @Override
    public boolean supports(String userMessage) {
        return intentClassifierExpert.hasIntent(
                userMessage,
                IntentTypeEnum.NOVEL_CREATION.getName(),
                IntentTypeEnum.NOVEL_CREATION.getDefinition()
        );
    }

    @Override
    public Flux<String> execute(int memoryId, String userMessage) {
        String traceId = TraceIdUtil.getTraceId();
        int messageLength = userMessage == null ? 0 : userMessage.length();
        log.info("[traceId={}] [novel-orchestrator] event=start memoryId={} messageLength={}", traceId, memoryId, messageLength);
        log.info("[traceId={}] [novel-orchestrator] event=intent_confirmed intent=novel_creation", traceId);
        AtomicReference<String> fullResponse = new AtomicReference<>("");
        return novelCreatorAgent.createNovelStream(userMessage)
                .doOnNext(chunk -> fullResponse.updateAndGet(prev -> prev + chunk))
                .doOnComplete(() -> {
                    String finalStory = extractFinalStory(fullResponse.get());
                    if (!finalStory.isBlank()) {
                        ChatMemory memory = chatMemoryRuntimeService.getOrCreateMemory(memoryId);
                        memory.add(AiMessage.from(finalStory));
                        log.info("[traceId={}] [novel-orchestrator] event=story_cached memoryId={}", traceId, memoryId);
                    } else {
                        log.warn("[traceId={}] [novel-orchestrator] event=story_missing memoryId={}", traceId, memoryId);
                    }
                })
                .doOnError(error -> log.error("[traceId={}] [novel-orchestrator] event=error memoryId={} errorType={}",
                        traceId, memoryId, error.getClass().getSimpleName()));
    }

    private String extractFinalStory(String fullResponse) {
        if (fullResponse == null || fullResponse.isBlank()) {
            return "";
        }
        int start = fullResponse.lastIndexOf(NovelCreatorAgent.FINAL_STORY_BEGIN_MARKER);
        if (start < 0) {
            return "";
        }
        int contentStart = start + NovelCreatorAgent.FINAL_STORY_BEGIN_MARKER.length();
        int end = fullResponse.indexOf(NovelCreatorAgent.FINAL_STORY_END_MARKER, contentStart);
        if (end < 0) {
            return "";
        }
        return fullResponse.substring(contentStart, end).trim();
    }
}

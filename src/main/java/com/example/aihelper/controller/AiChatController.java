package com.example.aihelper.controller;
import com.example.aihelper.dto.ChatRequest;
import com.example.aihelper.enums.OrchestrationStrategyEnum;
import com.example.aihelper.service.AgentOrchestrationService;
import com.example.aihelper.service.ChatMemoryService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 智能对话控制器。
 *
 * 暴露两种编排模式入口：
 * 1) routing：快速模式
 * 2) orchestrator：高质量模式
 */
@Slf4j
@RestController
@RequestMapping("/ai")
public class AiChatController {
    private static final String EVENT_MESSAGE = "message";
    private static final String EVENT_ERROR = "error";
    private static final int LOG_MESSAGE_MAX_LENGTH = 120;

    private final AgentOrchestrationService agentOrchestrationService;
    private final ChatMemoryService chatMemoryService;

    public AiChatController(AgentOrchestrationService agentOrchestrationService,
                            ChatMemoryService chatMemoryService) {
        this.agentOrchestrationService = agentOrchestrationService;
        this.chatMemoryService = chatMemoryService;
    }
    
    /**
     * 路由策略（快速模式）
     * 适合：简单查询、天气、路线、数据库查询
     */
    @GetMapping(value = "/chat/routing", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> chatWithRouting(
            @RequestParam int memoryId,
            @RequestParam String message) {
        return executeChat(ChatRequest.of(memoryId, message), OrchestrationStrategyEnum.ROUTING, "路由策略");
    }

    /**
     * 编排策略（高质量模式）
     * 适合：小说创作、旅行规划、多步骤推理
     */
    @GetMapping(value = "/chat/orchestrator", produces = "text/event-stream")
    public Flux<ServerSentEvent<String>> chatWithOrchestrator(
            @RequestParam int memoryId,
            @RequestParam String message) {
        return executeChat(ChatRequest.of(memoryId, message), OrchestrationStrategyEnum.ORCHESTRATOR, "编排策略");
    }
    
    /**
     * 执行对话（通用方法）
     */
    private Flux<ServerSentEvent<String>> executeChat(
            ChatRequest request,
            OrchestrationStrategyEnum strategy,
            String strategyName) {
        String traceId = MDC.get("traceId");
        StringBuilder assistantReplyBuffer = new StringBuilder();
        log.info("[traceId={}] [chat] event=start mode={} strategy={} memoryId={} message={}",
                traceId, strategyName, strategy.name(), request.memoryId(), request.messageSummary(LOG_MESSAGE_MAX_LENGTH));

        chatMemoryService.loadAndPrepareMemory(request.memoryId(), request.message());
        log.info("[traceId={}] [chat] event=memory_ready strategy={} memoryId={}",
                traceId, strategyName, request.memoryId());

        return agentOrchestrationService.executeStream(request.memoryId(), request.message(), strategy)
                
                .doOnNext(assistantReplyBuffer::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event(EVENT_MESSAGE)
                        .data(chunk)
                        .build())
                .doOnComplete(() -> {
                    log.info("[traceId={}] [chat] event=reply_completed strategy={} memoryId={}",
                            traceId, strategyName, request.memoryId());
                    chatMemoryService.appendTurnAndPersist(
                            request.memoryId(),
                            request.message(),
                            assistantReplyBuffer.toString()
                    );
                })
                .doFinally(signalType ->
                        log.info("[traceId={}] [chat] event=finished strategy={} signal={} memoryId={}",
                                traceId, strategyName, signalType, request.memoryId())
                )
                .doOnError(error ->
                    log.warn("[traceId={}] [chat] event=error strategy={} memoryId={} errorType={}",
                            traceId, strategyName, request.memoryId(), error.getClass().getSimpleName())
                )
                .onErrorResume(error -> Flux.just(
                        ServerSentEvent.<String>builder()
                                .event(EVENT_ERROR)
                                .data("请求处理失败: " + error.getMessage())
                                .build()
                ));
    }
}

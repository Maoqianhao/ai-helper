package com.example.aihelper.agent.novelagent;

import com.example.aihelper.enums.StateTransition;
import com.example.aihelper.utils.TraceIdUtil;
import com.example.aihelper.enums.NovelCreationStateEnum;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 小说创作主智能体（状态机 + ReAct）。
 */
@Slf4j
@Service
public class NovelCreatorAgent {
    public static final String FINAL_STORY_BEGIN_MARKER = "[[FINAL_STORY_BEGIN]]";
    public static final String FINAL_STORY_END_MARKER = "[[FINAL_STORY_END]]";
    private final NovelToolbox toolbox;

    public NovelCreatorAgent(ChatModel qwenChatModel) {
        this.toolbox = new NovelToolbox(qwenChatModel);
    }

    private static final AtomicInteger WORKER_THREAD_COUNTER = new AtomicInteger(1);
    private static final String DEFAULT_AUDIENCE = "普通读者";
    private static final String DEFAULT_STYLE = "标准风格";
    private final NovelReActLoopExecutor reActLoopExecutor = new NovelReActLoopExecutor();
    
    // 线程池用于异步任务
    private final ExecutorService executorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors(),
        r -> {
            Thread thread = new Thread(r);
            thread.setName("novel-creator-" + WORKER_THREAD_COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    );
    
    /**
     * 优雅关闭线程池
     */
    @PreDestroy
    public void shutdown() {
        log.info("[novel-agent] event=shutdown_start");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                log.warn("[novel-agent] event=shutdown_forced");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("[novel-agent] event=shutdown_completed");
    }
    
    // 配置参数（可从 application.yaml 注入）
    @Value("${novel-agent.max-iterations-per-state:3}")
    private int maxIterationsPerState;
    
    @Value("${novel-agent.max-global-iterations:20}")
    private int maxGlobalIterations;
    
    @Value("${novel-agent.text-truncate-length:500}")
    private int textTruncateLength;
    
    @Value("${novel-agent.reasoning-truncate-length:300}")
    private int reasoningTruncateLength;


    /**
     * 执行小说创作任务（流式输出版本）
     * 从用户消息中自动提取创作信息
     * 
     * @param userMessage 用户的自然语言消息
     * @return SSE流式响应
     */
    public Flux<String> createNovelStream(String userMessage) {
        // 参数验证
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("用户消息不能为空");
        }
        
        String traceId = TraceIdUtil.getTraceId();
        log.info("[traceId={}] [novel-stream] started messageLength={}", traceId, userMessage.length());
        
        // unicast 仅支持单订阅者，和当前 SSE 场景匹配
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        
        // 使用线程池执行异步任务
        CompletableFuture.runAsync(() -> {
            try {
                emitProgress(sink, "🚀 **开始分析创作需求...**\n");
                
                // 从用户消息中提取创作信息
                Map<String, String> extractedInfo = NovelCreationInfoExtractor.extract(
                        userMessage,
                        toolbox.reactThought(),
                        DEFAULT_AUDIENCE,
                        DEFAULT_STYLE
                );
                String topic = extractedInfo.get("topic");
                String audience = extractedInfo.get("audience");
                String style = extractedInfo.get("style");
                
                emitCreationInfo(sink, topic, audience, style);
                
                // 初始化上下文
                NovelCreationContext context = initializeContext(topic, audience, style);
                NovelStateExecutor stateExecutor = buildStateExecutor();
                
                // 状态机主循环
                NovelCreationStateEnum currentState = NovelCreationStateEnum.INIT;
                
                while (currentState != NovelCreationStateEnum.COMPLETED &&
                       currentState != NovelCreationStateEnum.FAILED) {
                    
                    // 检查取消订阅
                    if (sink.currentSubscriberCount() == 0) {
                        log.warn("[traceId={}] [novel-stream] event=client_disconnected", traceId);
                        break;
                    }
                    
                    emitProgress(sink, "\n📍 **当前状态**: " + currentState + "\n");
                    
                    // 检查全局迭代次数
                    int globalIterations = context.iterationCount();
                    if (globalIterations >= maxGlobalIterations) {
                        emitProgress(sink, "❌ 达到全局最大迭代次数(" + maxGlobalIterations + ")\n");
                        currentState = NovelCreationStateEnum.FAILED;
                        break;
                    }
                    
                    // 执行当前状态
                    StateTransition transition = stateExecutor.executeStateWithReAct(currentState, context);
                    
                    // 更新状态
                    currentState = transition.nextState();
                    context.incrementIterationCount();
                    
                    // 流式输出状态转移信息
                    if (transition.success()) {
                        emitProgress(sink, "✅ **状态转移成功**: " + transition.actionName() + "\n");
                    } else {
                        emitProgress(sink, "⚠️ **状态转移失败**: " + transition.actionName() + "\n");
                        if (transition.errorMessage() != null) {
                            emitProgress(sink, "错误详情: " + transition.errorMessage() + "\n");
                        }
                    }
                }
                
                // 处理最终结果
                handleStreamCompletion(sink, currentState, context);
                
            } catch (Exception e) {
                log.error("[traceId={}] [novel-stream] event=error message={}", traceId, e.getMessage(), e);
                sink.tryEmitError(e);
            }
        }, executorService);
        
        // 返回 Flux，并在取消时清理资源
        return sink.asFlux()
            .doOnCancel(() -> {
                log.info("[traceId={}] [novel-stream] event=cancelled", traceId);
            })
            .doOnError(error -> {
                log.error("[traceId={}] [novel-stream] event=flux_error message={}", traceId, error.getMessage(), error);
            });
    }
    
    /**
     * 安全地发送消息到 Sink
     */
    private void emitProgress(Sinks.Many<String> sink, String message) {
        Sinks.EmitResult result = sink.tryEmitNext(message);
        if (result.isFailure()) {
            log.warn("[novel-stream] event=emit_failed emitResult={}", result);
        }
    }

    private void emitCreationInfo(Sinks.Many<String> sink, String topic, String audience, String style) {
        emitProgress(sink, "📝 **提取的创作信息:**\n");
        emitProgress(sink, "- 主题: " + truncate(topic, textTruncateLength) + "\n");
        emitProgress(sink, "- 受众: " + truncate(audience, textTruncateLength) + "\n");
        emitProgress(sink, "- 风格: " + truncate(style, textTruncateLength) + "\n\n");
    }
    
    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
    
    /**
     * 初始化上下文
     */
    private NovelCreationContext initializeContext(String topic, String audience, String style) {
        return NovelCreationContext.of(
                topic,
                Objects.requireNonNullElse(audience, DEFAULT_AUDIENCE),
                Objects.requireNonNullElse(style, DEFAULT_STYLE)
        );
    }

    
    /**
     * 处理流式输出的完成状态
     */
    private void handleStreamCompletion(Sinks.Many<String> sink, 
                                        NovelCreationStateEnum finalState,
                                        NovelCreationContext context) {
        if (finalState == NovelCreationStateEnum.COMPLETED) {
            String finalStory = context.finalStoryOrDefault("（未生成最终内容）");
            emitProgress(sink, "\n🎉 **创作完成!** 最终作品:\n");
            emitProgress(sink, FINAL_STORY_BEGIN_MARKER + "\n");
            emitProgress(sink, finalStory + "\n");
            emitProgress(sink, FINAL_STORY_END_MARKER + "\n");
        } else {
            emitProgress(sink, "\n❌ 创作失败");
        }
        sink.tryEmitComplete();
    }
    private NovelStateExecutor buildStateExecutor() {
        return new NovelStateExecutor(toolbox, reActLoopExecutor, maxIterationsPerState, reasoningTruncateLength);
    }
}

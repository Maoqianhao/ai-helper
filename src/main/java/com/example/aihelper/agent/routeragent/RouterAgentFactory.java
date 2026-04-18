package com.example.aihelper.agent.routeragent;
import com.example.aihelper.agent.workers.AmapExpert;
import com.example.aihelper.agent.workers.InterviewExpert;
import com.example.aihelper.agent.workers.SqlExpert;
import com.example.aihelper.tools.InterviewQuestionTool;
import com.example.aihelper.utils.TraceIdUtil;
import com.example.aihelper.memory.CustomChatMemoryProvider;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 路由服务工厂
 * 负责构建主路由协调器和所有子专家
 */
@Configuration
public class RouterAgentFactory {

    private final ChatModel qwenChatModel;
    private final StreamingChatModel streamingQwenChatModel;
    private final CustomChatMemoryProvider customChatMemoryProvider;
    private final McpToolProvider mcpToolProvider;
    private final SqlExpert sqlExpert;
    private final RouteIntentGuard routeIntentGuard;

    public RouterAgentFactory(ChatModel qwenChatModel,
                              StreamingChatModel streamingQwenChatModel,
                              CustomChatMemoryProvider customChatMemoryProvider,
                              McpToolProvider mcpToolProvider,
                              SqlExpert sqlExpert,
                              RouteIntentGuard routeIntentGuard) {
        this.qwenChatModel = qwenChatModel;
        this.streamingQwenChatModel = streamingQwenChatModel;
        this.customChatMemoryProvider = customChatMemoryProvider;
        this.mcpToolProvider = mcpToolProvider;
        this.sqlExpert = sqlExpert;
        this.routeIntentGuard = routeIntentGuard;
    }
    
    @Bean
    public RouterCoordinator routerCoordinator() {
        AmapExpert amapExpert = buildAmapExpert();
        InterviewExpert interviewExpert = buildInterviewExpert();
        AmapRouteTool amapRouteTool = new AmapRouteTool(amapExpert, routeIntentGuard);
        InterviewRouteTool interviewRouteTool = new InterviewRouteTool(interviewExpert, routeIntentGuard);

        return AiServices
                .builder(RouterCoordinator.class)
                .tools(sqlExpert, amapRouteTool, interviewRouteTool)
                .streamingChatModel(streamingQwenChatModel)
                .chatMemoryProvider(customChatMemoryProvider)
                .build();
    }

    private AmapExpert buildAmapExpert() {
        return AiServices
                .builder(AmapExpert.class)
                .chatModel(qwenChatModel)
                .toolProvider(mcpToolProvider)
                .build();
    }

    private InterviewExpert buildInterviewExpert() {
        return AiServices
                .builder(InterviewExpert.class)
                .chatModel(qwenChatModel)
                .tools(new InterviewQuestionTool())
                .build();
    }

    @Slf4j
    private static class AmapRouteTool {
        private static final String CLARIFY_MESSAGE = "为了保证回答准确，我先确认一下：你是要查询地图/天气/路线，还是在问数据库/面试问题？请补充一下目标。";
        private final AmapExpert amapExpert;
        private final RouteIntentGuard routeIntentGuard;

        private AmapRouteTool(AmapExpert amapExpert, RouteIntentGuard routeIntentGuard) {
            this.amapExpert = amapExpert;
            this.routeIntentGuard = routeIntentGuard;
        }

        @Tool("专门处理天气、地理位置、路线规划、周边搜索类问题")
        public String askAmap(String request) {
            String traceId = TraceIdUtil.getTraceId();
            if (!routeIntentGuard.isHighConfidenceAmap(request)) {
                log.warn("[traceId={}] [route-guard] routeDecision=amap blocked=true reason=low_confidence_or_conflict", traceId);
                return CLARIFY_MESSAGE;
            }
            log.info("[traceId={}] [route] routeDecision=amap", traceId);
            return amapExpert.askAmap(request);
        }
    }

    @Slf4j
    private static class InterviewRouteTool {
        private static final String CLARIFY_MESSAGE = "为了避免误路由，我先确认：你希望我做面试题/技术问答，还是数据库查询/地图路线？请说明具体目标。";
        private final InterviewExpert interviewExpert;
        private final RouteIntentGuard routeIntentGuard;

        private InterviewRouteTool(InterviewExpert interviewExpert, RouteIntentGuard routeIntentGuard) {
            this.interviewExpert = interviewExpert;
            this.routeIntentGuard = routeIntentGuard;
        }

        @Tool("专门处理面试题生成、技术问答、刷题、模拟面试类问题")
        public String process(String request) {
            String traceId = TraceIdUtil.getTraceId();
            if (!routeIntentGuard.isHighConfidenceInterview(request)) {
                log.warn("[traceId={}] [route-guard] routeDecision=interview blocked=true reason=low_confidence_or_conflict", traceId);
                return CLARIFY_MESSAGE;
            }
            log.info("[traceId={}] [route] routeDecision=interview", traceId);
            return interviewExpert.process(request);
        }
    }
}

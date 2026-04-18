package com.example.aihelper.config;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ChatModelListenerConfig {
    @Bean
    ChatModelListener chatModelListener() {
        return new ChatModelListener() {
            @Override
            public void onRequest(ChatModelRequestContext requestContext) {
                String traceId = MDC.get("traceId");
                Object request = requestContext.chatRequest();
                log.info("[traceId={}] [llm-listener] event=request payloadType={}",
                        traceId,
                        request == null ? "unknown" : request.getClass().getSimpleName());
            }
            @Override
            public void onResponse(ChatModelResponseContext responseContext) {
                String traceId = MDC.get("traceId");
                Object response = responseContext.chatResponse();
                log.info("[traceId={}] [llm-listener] event=response payloadType={}",
                        traceId,
                        response == null ? "unknown" : response.getClass().getSimpleName());
            }
            @Override
            public void onError(ChatModelErrorContext errorContext) {
                String traceId = MDC.get("traceId");
                Throwable error = errorContext.error();
                log.warn("[traceId={}] [llm-listener] event=error errorType={} message={}",
                        traceId,
                        error == null ? "unknown" : error.getClass().getSimpleName(),
                        error == null ? "unknown" : error.getMessage());
            }
        };
    }
}
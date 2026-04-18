package com.example.aihelper.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Qwen 聊天模型配置。
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.community.dashscope.chat-model")
@Data
@Slf4j
public class QwenChatModelConfig {

    private String modelName;

    private String apiKey;

    private final ChatModelListener chatModelListener;

    public QwenChatModelConfig(ChatModelListener chatModelListener) {
        this.chatModelListener = chatModelListener;
    }

    @Bean
    public ChatModel myQwenChatModel() {
        log.info("[qwen-chat-model] event=init modelName={} temperature={}", modelName, 0.2F);
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.2F)
                .listeners(List.of(chatModelListener))
                .build();
    }
}

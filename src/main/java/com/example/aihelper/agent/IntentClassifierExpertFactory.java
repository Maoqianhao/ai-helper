package com.example.aihelper.agent;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 意图识别智能体工厂。
 */
@Configuration
public class IntentClassifierExpertFactory {

    private final ChatModel qwenChatModel;

    public IntentClassifierExpertFactory(ChatModel qwenChatModel) {
        this.qwenChatModel = qwenChatModel;
    }

    @Bean
    public IntentClassifierExpert intentClassifier() {
        return AiServices.builder(IntentClassifierExpert.class)
                .chatModel(qwenChatModel)
                .build();
    }
}

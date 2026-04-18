package com.example.aihelper.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 通用意图识别智能体接口。
 */
public interface IntentClassifierExpert {
    /**
     * 判断输入是否满足目标意图定义。
     */
    @SystemMessage(fromResource = "templates/systemmessage/intent_classifier_expert_system_message.txt")
    boolean hasIntent(
        @UserMessage String userMessage,
        @V("intentType") String intentType,
        @V("intentDefinition") String intentDefinition
    );
}

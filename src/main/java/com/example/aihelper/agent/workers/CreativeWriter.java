package com.example.aihelper.agent.workers;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 创意写作专家接口。
 */
public interface CreativeWriter {

    /**
     * 基于主题生成故事草稿。
     */
    @UserMessage(fromResource = "templates/usermessage/creative_writer_user_message.txt")
    @Agent("Generates a story based on the given topic")
    String generateStory(@V("topic") String topic);
}

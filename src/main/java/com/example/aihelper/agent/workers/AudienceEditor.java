package com.example.aihelper.agent.workers;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 受众编辑专家接口。
 */
public interface AudienceEditor {
    /**
     * 按目标受众重写故事文本。
     */
    @UserMessage(fromResource = "templates/usermessage/audience_editor_user_message.txt")
    @Agent("Edits a story to better fit a given audience")
    String editStory(@V("story") String story, @V("audience") String audience);
}

package com.example.aihelper.agent.workers;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 风格编辑专家接口。
 */
public interface StyleEditor {

    /**
     * 按指定风格改写故事文本。
     */
    @UserMessage(fromResource = "templates/usermessage/style_editor_user_message.txt")
    @Agent("Edits a story to match a given style")
    String editStyle(@V("story") String story, @V("style") String style);
}

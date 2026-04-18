package com.example.aihelper.agent.thinkpattern;

import com.example.aihelper.utils.TextUtil;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct 思考范式
 */
@Slf4j
public class ReactThought {

    private final ChatModel chatModel;
    private final int truncateLength;

    public ReactThought(ChatModel chatModel) {
        this(chatModel, 100);
    }

    public ReactThought(ChatModel chatModel, int truncateLength) {
        this.chatModel = chatModel;
        this.truncateLength = truncateLength;
    }

    public String reason(String prompt) {
        log.debug("[react-reason] {}", TextUtil.truncate(prompt.replace("\n", " "), truncateLength));
        return chatModel.chat(prompt);
    }

    public String act(String prompt) {
        log.debug("[react-act] {}", TextUtil.truncate(prompt.replace("\n", " "), truncateLength));
        return chatModel.chat(prompt);
    }
}

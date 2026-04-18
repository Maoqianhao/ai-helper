package com.example.aihelper.agent.novelagent;

import com.example.aihelper.agent.thinkpattern.ReactThought;
import com.example.aihelper.agent.workers.AudienceEditor;
import com.example.aihelper.agent.workers.CreativeWriter;
import com.example.aihelper.agent.workers.StyleEditor;
import dev.langchain4j.model.chat.ChatModel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class NovelToolbox {

    private final ChatModel chatModel;
    private volatile ReactThought reactThought;
    private volatile CreativeWriter creativeWriter;
    private volatile AudienceEditor audienceEditor;
    private volatile StyleEditor styleEditor;

    NovelToolbox(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    ReactThought reactThought() {
        if (reactThought == null) {
            synchronized (this) {
                if (reactThought == null) {
                    reactThought = new ReactThought(chatModel);
                    log.debug("[novel-toolbox] event=init_completed component=ReactThought");
                }
            }
        }
        return reactThought;
    }

    CreativeWriter creativeWriter() {
        if (creativeWriter == null) {
            synchronized (this) {
                if (creativeWriter == null) {
                    creativeWriter = dev.langchain4j.agentic.AgenticServices
                            .agentBuilder(CreativeWriter.class)
                            .chatModel(chatModel)
                            .build();
                    log.debug("[novel-toolbox] event=init_completed component=CreativeWriter");
                }
            }
        }
        return creativeWriter;
    }

    AudienceEditor audienceEditor() {
        if (audienceEditor == null) {
            synchronized (this) {
                if (audienceEditor == null) {
                    audienceEditor = dev.langchain4j.agentic.AgenticServices
                            .agentBuilder(AudienceEditor.class)
                            .chatModel(chatModel)
                            .build();
                    log.debug("[novel-toolbox] event=init_completed component=AudienceEditor");
                }
            }
        }
        return audienceEditor;
    }

    StyleEditor styleEditor() {
        if (styleEditor == null) {
            synchronized (this) {
                if (styleEditor == null) {
                    styleEditor = dev.langchain4j.agentic.AgenticServices
                            .agentBuilder(StyleEditor.class)
                            .chatModel(chatModel)
                            .build();
                    log.debug("[novel-toolbox] event=init_completed component=StyleEditor");
                }
            }
        }
        return styleEditor;
    }
}

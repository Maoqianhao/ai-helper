package com.example.aihelper.agent.novelagent;

import java.util.Objects;

/**
 * 小说创作流程强类型上下文。
 */
final class NovelCreationContext {

    private final String topic;
    private final String audience;
    private final String style;
    private String analysis;
    private String draft;
    private String editedStory;
    private String finalStory;
    private int iterationCount;

    private NovelCreationContext(String topic, String audience, String style) {
        this.topic = requireText(topic, "topic");
        this.audience = requireText(audience, "audience");
        this.style = requireText(style, "style");
        this.iterationCount = 0;
    }

    static NovelCreationContext of(String topic, String audience, String style) {
        return new NovelCreationContext(topic, audience, style);
    }

    String topic() {
        return topic;
    }

    String audience() {
        return audience;
    }

    String style() {
        return style;
    }

    int iterationCount() {
        return iterationCount;
    }

    void incrementIterationCount() {
        this.iterationCount++;
    }

    String analysis() {
        return requireText(analysis, "analysis");
    }

    void analysis(String analysis) {
        this.analysis = requireText(analysis, "analysis");
    }

    String draft() {
        return requireText(draft, "draft");
    }

    void draft(String draft) {
        this.draft = requireText(draft, "draft");
    }

    void clearDraft() {
        this.draft = null;
    }

    String editedStory() {
        return requireText(editedStory, "editedStory");
    }

    void editedStory(String editedStory) {
        this.editedStory = requireText(editedStory, "editedStory");
    }

    void clearEditedStory() {
        this.editedStory = null;
    }

    String finalStoryOrDefault(String fallback) {
        if (!hasText(finalStory)) {
            return fallback;
        }
        return finalStory;
    }

    String finalStory() {
        return requireText(finalStory, "finalStory");
    }

    void finalStory(String finalStory) {
        this.finalStory = requireText(finalStory, "finalStory");
    }

    private static String requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalStateException("上下文中缺少必需字段: " + fieldName);
        }
        return value;
    }

    private static boolean hasText(String value) {
        return Objects.nonNull(value) && !value.trim().isEmpty();
    }
}

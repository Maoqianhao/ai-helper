package com.example.aihelper.prompt;

import java.util.List;

/**
 * Prompt 资产清单，便于统一治理与回归追踪。
 */
public final class PromptRegistry {

    private PromptRegistry() {
    }

    /**
     * 返回系统已纳管的 Prompt 清单。
     */
    public static List<PromptSpec> all() {
        return List.of(
                new PromptSpec("router.system", PromptSource.RESOURCE_FILE, "templates/systemmessage/router_expert_system_message.txt"),
                new PromptSpec("intent.classifier.system", PromptSource.RESOURCE_FILE, "templates/systemmessage/intent_classifier_expert_system_message.txt"),
                new PromptSpec("amap.system", PromptSource.RESOURCE_FILE, "templates/systemmessage/amap_expert_system_message.txt"),
                new PromptSpec("interview.system", PromptSource.RESOURCE_FILE, "templates/systemmessage/interview_expert_system_message.txt"),
                new PromptSpec("creative.writer.user", PromptSource.RESOURCE_FILE, "templates/usermessage/creative_writer_user_message.txt"),
                new PromptSpec("audience.editor.user", PromptSource.RESOURCE_FILE, "templates/usermessage/audience_editor_user_message.txt"),
                new PromptSpec("style.editor.user", PromptSource.RESOURCE_FILE, "templates/usermessage/style_editor_user_message.txt"),
                new PromptSpec("novel.analysis", PromptSource.RESOURCE_FILE, "templates/novel/analysis_prompt.txt"),
                new PromptSpec("novel.draft.evaluation", PromptSource.HARDCODED_TEMPLATE, "NovelPrompts.buildDraftEvaluationPrompt"),
                new PromptSpec("novel.final.approval", PromptSource.RESOURCE_FILE, "templates/novel/final_approval_prompt.txt")
        );
    }

    /**
     * Prompt 描述对象。
     */
    public record PromptSpec(String id, PromptSource source, String location) {
    }

    /**
     * Prompt 来源类型。
     */
    public enum PromptSource {
        RESOURCE_FILE,
        HARDCODED_TEMPLATE
    }
}

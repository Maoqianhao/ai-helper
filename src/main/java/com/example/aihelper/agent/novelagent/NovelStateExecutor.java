package com.example.aihelper.agent.novelagent;

import com.example.aihelper.enums.NovelCreationStateEnum;
import com.example.aihelper.enums.StateTransition;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
final class NovelStateExecutor {

    private final NovelToolbox toolbox;
    private final NovelReActLoopExecutor reActLoopExecutor;
    private final int maxIterationsPerState;
    private final int reasoningTruncateLength;
    private final Map<NovelCreationStateEnum, Function<NovelCreationContext, StateTransition>> stateHandlers;

    NovelStateExecutor(NovelToolbox toolbox,
                       NovelReActLoopExecutor reActLoopExecutor,
                       int maxIterationsPerState,
                       int reasoningTruncateLength) {
        this.toolbox = toolbox;
        this.reActLoopExecutor = reActLoopExecutor;
        this.maxIterationsPerState = maxIterationsPerState;
        this.reasoningTruncateLength = reasoningTruncateLength;
        this.stateHandlers = buildStateHandlers();
    }

    StateTransition executeStateWithReAct(NovelCreationStateEnum currentState, NovelCreationContext context) {
        try {
            Function<NovelCreationContext, StateTransition> handler = stateHandlers.get(currentState);
            if (handler == null) {
                log.error("[novel-state] event=unknown_state state={}", currentState);
                return StateTransition.failure(NovelCreationStateEnum.FAILED, "UNKNOWN_STATE", "未知状态: " + currentState);
            }
            return handler.apply(context);
        } catch (Exception e) {
            log.error("[novel-state] event=state_exception state={} message={}", currentState, e.getMessage(), e);
            return StateTransition.failure(NovelCreationStateEnum.FAILED, currentState.name(), e.getMessage());
        }
    }

    private Map<NovelCreationStateEnum, Function<NovelCreationContext, StateTransition>> buildStateHandlers() {
        Map<NovelCreationStateEnum, Function<NovelCreationContext, StateTransition>> handlers =
                new EnumMap<>(NovelCreationStateEnum.class);
        handlers.put(NovelCreationStateEnum.INIT, this::handleInit);
        handlers.put(NovelCreationStateEnum.ANALYZING, this::handleAnalyzing);
        handlers.put(NovelCreationStateEnum.DRAFTING, this::handleDrafting);
        handlers.put(NovelCreationStateEnum.EVALUATING_DRAFT, this::handleEvaluatingDraft);
        handlers.put(NovelCreationStateEnum.EDITING_AUDIENCE, this::handleEditingAudience);
        handlers.put(NovelCreationStateEnum.EVALUATING_EDIT, this::handleEvaluatingEdit);
        handlers.put(NovelCreationStateEnum.STYLING, this::handleStyling);
        handlers.put(NovelCreationStateEnum.FINALIZING, this::handleFinalizing);
        return Collections.unmodifiableMap(handlers);
    }

    private StateTransition handleInit(NovelCreationContext context) {
        if (context.topic().trim().isEmpty()) {
            log.error("[novel-stream] topic is empty");
            return StateTransition.failure(NovelCreationStateEnum.FAILED, "init", "主题不能为空");
        }
        log.info("[novel-stream] input validation passed");
        return StateTransition.success(NovelCreationStateEnum.ANALYZING, "init", null);
    }

    private StateTransition handleAnalyzing(NovelCreationContext context) {
        return safeExecuteWithReAct(
                () -> {
                    String analysisReasoning = reason(NovelPrompts.buildAnalysisPrompt(
                            context.topic(),
                            context.audience(),
                            context.style()
                    ));
                    context.analysis(analysisReasoning);
                    log.info("[novel-stream] analysis done={}", truncate(analysisReasoning, reasoningTruncateLength));
                    return StateTransition.success(NovelCreationStateEnum.DRAFTING, "analyze", analysisReasoning);
                },
                "需求分析"
        );
    }

    private StateTransition handleDrafting(NovelCreationContext context) {
        String topic = context.topic();
        NovelReActLoopExecutor.ReActLoopResult result = reActLoopExecutor.execute(
                "草稿创作",
                maxIterationsPerState,
                () -> toolbox.creativeWriter().generateStory(topic),
                (currentDraft) -> {
                    String reasoning = reason(NovelPrompts.buildDraftEvaluationPrompt(currentDraft, topic));
                    boolean isComplete = DecisionMatcher.matches(reasoning, NovelPrompts.DRAFT_COMPLETE, NovelPrompts.DRAFT_COMPLETE);
                    return new NovelReActLoopExecutor.EvaluationResult(isComplete, reasoning);
                },
                (currentDraft, evaluation) -> act(NovelPrompts.buildDraftImprovementPrompt(evaluation.reasoning(), currentDraft)),
                context::draft
        );
        return toStateTransition(result, NovelCreationStateEnum.EVALUATING_DRAFT);
    }

    private StateTransition handleEvaluatingDraft(NovelCreationContext context) {
        return safeExecuteWithReAct(
                () -> {
                    String evaluation = reason(NovelPrompts.buildDraftReadinessPrompt(context.draft()));
                    boolean readyForEdit = DecisionMatcher.matches(evaluation, NovelPrompts.EDIT_READY, NovelPrompts.EDIT_READY);
                    if (readyForEdit) {
                        log.info("[novel-stream] draft evaluation passed, move to edit");
                        return StateTransition.success(NovelCreationStateEnum.EDITING_AUDIENCE, "evaluate_draft", evaluation);
                    }
                    log.warn("[novel-stream] draft evaluation failed, regenerate draft");
                    context.clearDraft();
                    return StateTransition.success(NovelCreationStateEnum.DRAFTING, "re_draft", evaluation);
                },
                "草稿评估"
        );
    }

    private StateTransition handleEditingAudience(NovelCreationContext context) {
        String draft = context.draft();
        String audience = context.audience();
        NovelReActLoopExecutor.ReActLoopResult result = reActLoopExecutor.execute(
                "受众编辑",
                maxIterationsPerState,
                () -> toolbox.audienceEditor().editStory(draft, audience),
                (currentStory) -> {
                    String reasoning = reason(NovelPrompts.buildAudienceFitPrompt(currentStory, audience));
                    boolean isReady = DecisionMatcher.matches(reasoning, NovelPrompts.AUDIENCE_READY, NovelPrompts.AUDIENCE_READY);
                    return new NovelReActLoopExecutor.EvaluationResult(isReady, reasoning);
                },
                (currentStory, evaluation) -> act(NovelPrompts.buildAudienceImprovementPrompt(audience, evaluation.reasoning(), currentStory)),
                context::editedStory
        );
        return toStateTransition(result, NovelCreationStateEnum.EVALUATING_EDIT);
    }

    private StateTransition handleEvaluatingEdit(NovelCreationContext context) {
        return safeExecuteWithReAct(
                () -> {
                    String evaluation = reason(NovelPrompts.buildReadinessPrompt(
                            "编辑结果",
                            context.editedStory(),
                            "是否适合目标受众" + context.audience(),
                            NovelPrompts.STYLE_READY
                    ));
                    boolean readyForStyle = DecisionMatcher.matches(evaluation, NovelPrompts.STYLE_READY, NovelPrompts.STYLE_READY);
                    if (readyForStyle) {
                        log.info("[novel-stream] edit evaluation passed, move to styling");
                        return StateTransition.success(NovelCreationStateEnum.STYLING, "evaluate_edit", evaluation);
                    }
                    log.warn("[novel-stream] edit evaluation failed, redo editing");
                    context.clearEditedStory();
                    return StateTransition.success(NovelCreationStateEnum.EDITING_AUDIENCE, "re_edit", evaluation);
                },
                "编辑评估"
        );
    }

    private StateTransition handleStyling(NovelCreationContext context) {
        String editedStory = context.editedStory();
        String style = context.style();
        NovelReActLoopExecutor.ReActLoopResult result = reActLoopExecutor.execute(
                "风格调整",
                maxIterationsPerState,
                () -> toolbox.styleEditor().editStyle(editedStory, style),
                (currentStory) -> {
                    String reasoning = reason(NovelPrompts.buildStyleFitPrompt(currentStory, style));
                    boolean isReady = DecisionMatcher.matches(reasoning, NovelPrompts.FINAL_READY, NovelPrompts.FINAL_READY);
                    return new NovelReActLoopExecutor.EvaluationResult(isReady, reasoning);
                },
                (currentStory, evaluation) -> act(NovelPrompts.buildStyleImprovementPrompt(style, evaluation.reasoning(), currentStory)),
                context::finalStory
        );
        return toStateTransition(result, NovelCreationStateEnum.FINALIZING);
    }

    private StateTransition handleFinalizing(NovelCreationContext context) {
        return safeExecuteWithReAct(
                () -> {
                    String finalStory = context.finalStory();
                    String finalCheck = reason(NovelPrompts.buildFinalApprovalPrompt(finalStory));
                    boolean approved = DecisionMatcher.matches(finalCheck, NovelPrompts.APPROVED, NovelPrompts.APPROVED, "approved", "通过");
                    if (approved) {
                        log.info("[novel-stream] final approval passed");
                        return StateTransition.success(NovelCreationStateEnum.COMPLETED, "finalize", finalStory);
                    }
                    log.warn("[novel-stream] final approval failed, need refinement");
                    NovelCreationStateEnum nextState = determineRevisionStage(finalCheck);
                    return StateTransition.success(nextState, "final_touch", finalCheck);
                },
                "最终审定"
        );
    }

    private NovelCreationStateEnum determineRevisionStage(String feedback) {
        String lowerFeedback = feedback.toLowerCase();
        if (lowerFeedback.contains("风格") || lowerFeedback.contains("style")) {
            return NovelCreationStateEnum.STYLING;
        }
        return NovelCreationStateEnum.EDITING_AUDIENCE;
    }

    private String reason(String prompt) {
        return toolbox.reactThought().reason(prompt);
    }

    private String act(String prompt) {
        return toolbox.reactThought().act(prompt);
    }

    private StateTransition toStateTransition(NovelReActLoopExecutor.ReActLoopResult result,
                                              NovelCreationStateEnum nextStateOnSuccess) {
        if (result.success() || result.partialSuccess()) {
            return StateTransition.success(
                    nextStateOnSuccess,
                    result.success() ? "react_loop" : "react_loop_partial",
                    result.result()
            );
        }
        return StateTransition.failure(NovelCreationStateEnum.FAILED, "react_loop_failed", result.errorMessage());
    }

    private StateTransition safeExecuteWithReAct(Supplier<StateTransition> action, String operationName) {
        try {
            return action.get();
        } catch (IllegalStateException e) {
            log.error("[novel-stream] operation failed name={}, error={}", operationName, e.getMessage());
            return StateTransition.failure(NovelCreationStateEnum.FAILED, operationName, e.getMessage());
        } catch (Exception e) {
            log.error("[novel-stream] operation exception name={}, error={}", operationName, e.getMessage(), e);
            return StateTransition.failure(
                    NovelCreationStateEnum.FAILED,
                    operationName,
                    String.format("%s 执行异常: %s", operationName, e.getMessage())
            );
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }
}

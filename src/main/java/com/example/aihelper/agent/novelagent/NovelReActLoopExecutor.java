package com.example.aihelper.agent.novelagent;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * ReAct 循环执行器
 *
 * 职责：统一处理“初始执行 -> 评估 -> 改进”的迭代流程。
 */
@Slf4j
final class NovelReActLoopExecutor {

    ReActLoopResult execute(
            String loopName,
            int maxIterations,
            Supplier<String> initialAction,
            Function<String, EvaluationResult> evaluator,
            BiFunctionWithException<String, EvaluationResult, String> improver,
            ResultCallback resultCallback) {

        String currentResult = null;
        for (int i = 0; i < maxIterations; i++) {
            log.info("[react-loop] {} iteration={}/{}", loopName, i + 1, maxIterations);
            try {
                if (currentResult == null) {
                    currentResult = initialAction.get();
                }

                EvaluationResult evaluation = evaluator.apply(currentResult);
                resultCallback.onResult(currentResult);
                if (evaluation.complete()) {
                    log.info("[react-loop] {} completed at iteration={}", loopName, i + 1);
                    return ReActLoopResult.success(currentResult);
                }

                if (i == maxIterations - 1) {
                    log.warn("[react-loop] {} reached max iteration without completion", loopName);
                    return ReActLoopResult.partialSuccess(currentResult);
                }
                currentResult = improver.apply(currentResult, evaluation);
            } catch (Exception e) {
                log.error("[react-loop] loop={} event=iteration_error iteration={} message={}",
                        loopName, i + 1, e.getMessage(), e);
            }
        }

        if (currentResult == null) {
            String errorMsg = String.format("loop=%s event=failed_without_result", loopName);
            log.error("[react-loop] {}", errorMsg);
            return ReActLoopResult.failure(errorMsg);
        }

        log.warn("[react-loop] max iterations reached loop={} limit={} with fallback", loopName, maxIterations);
        return ReActLoopResult.partialSuccess(currentResult);
    }

    record EvaluationResult(boolean complete, String reasoning) {
    }

    @FunctionalInterface
    interface BiFunctionWithException<T, U, R> {
        R apply(T t, U u) throws Exception;
    }

    @FunctionalInterface
    interface ResultCallback {
        void onResult(String result);
    }

    record ReActLoopResult(boolean success, boolean partialSuccess, String result, String errorMessage) {
        static ReActLoopResult success(String result) {
            return new ReActLoopResult(true, false, result, null);
        }

        static ReActLoopResult partialSuccess(String result) {
            return new ReActLoopResult(false, true, result, null);
        }

        static ReActLoopResult failure(String errorMessage) {
            return new ReActLoopResult(false, false, null, errorMessage);
        }
    }
}

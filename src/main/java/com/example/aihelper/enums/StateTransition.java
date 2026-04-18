package com.example.aihelper.enums;

/**
 * 状态转移结果。
 *
 * 用于状态机流程记录当前动作的转移结论。
 * 
 * @param nextState 下一个状态
 * @param actionName 执行的动作名称
 * @param result 动作执行结果
 * @param success 是否成功
 * @param errorMessage 错误信息（失败时）
 */
public record StateTransition(
        NovelCreationStateEnum nextState,
        String actionName,
        Object result,
        boolean success,
        String errorMessage
) {
    
    /**
     * 便捷构造方法 - 成功转移（无错误信息）
     */
    public StateTransition(NovelCreationStateEnum nextState, String actionName, Object result, boolean success) {
        this(nextState, actionName, result, success, null);
    }
    
    /**
     * 创建成功的状态转移
     */
    public static StateTransition success(NovelCreationStateEnum nextState, String actionName, Object result) {
        return new StateTransition(nextState, actionName, result, true, null);
    }
    
    /**
     * 创建失败的状态转移
     */
    public static StateTransition failure(NovelCreationStateEnum nextState, String actionName, String errorMessage) {
        return new StateTransition(nextState, actionName, null, false, errorMessage);
    }
}

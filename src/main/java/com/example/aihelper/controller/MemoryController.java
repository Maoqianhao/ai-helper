package com.example.aihelper.controller;

import com.example.aihelper.dto.MemoryHistoryResponse;
import com.example.aihelper.dto.MemoryOperationResponse;
import com.example.aihelper.service.ChatMemoryService;
import com.example.aihelper.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 会话记忆管理控制器。
 */
@Slf4j
@RestController
public class MemoryController {
    private final ChatMemoryService chatMemoryService;

    public MemoryController(ChatMemoryService chatMemoryService) {
        this.chatMemoryService = chatMemoryService;
    }
    
    /**
     * 清除指定会话的记忆
     */
    @DeleteMapping("/memory/clear/{sessionId}")
    public MemoryOperationResponse clearMemory(@PathVariable String sessionId) {
        String traceId = TraceIdUtil.getTraceId();
        log.info("[traceId={}] [memory] event=clear_start sessionId={}", traceId, sessionId);
        chatMemoryService.clearMemories(sessionId);
        log.info("[traceId={}] [memory] event=clear_completed sessionId={}", traceId, sessionId);
        return MemoryOperationResponse.success("会话记忆已清除", sessionId);
    }

    /**
     * 获取指定会话的历史消息
     * 用于前端查看历史对话记录
     * 
     * @param sessionId 会话ID
     * @return 包含 success、messages 字段的 JSON 响应
     */
    @GetMapping("/memory/history/{sessionId}")
    public MemoryHistoryResponse getHistoryMessages(@PathVariable String sessionId) {
        String traceId = TraceIdUtil.getTraceId();
        log.info("[traceId={}] [memory] event=history_start sessionId={}", traceId, sessionId);
        List<Map<String, String>> messages = chatMemoryService.getHistoryMessages(sessionId);
        log.info("[traceId={}] [memory] event=history_completed sessionId={} messageCount={}", traceId, sessionId, messages.size());
        return MemoryHistoryResponse.of(sessionId, messages);
    }
}

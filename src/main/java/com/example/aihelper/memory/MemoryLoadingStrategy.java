package com.example.aihelper.memory;

import com.example.aihelper.utils.MemoryUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 记忆加载策略。
 */
@Slf4j
@Service
public class MemoryLoadingStrategy {
    
    /**
     * 执行记忆加载策略
     * 
     * @param allMatches 从 Chroma 查询到的所有匹配结果
     * @param sessionId 会话ID（用于日志）
     * @return 加载的记忆列表（按时间升序）
     */
    public List<ChatMessage> execute(List<EmbeddingMatch<TextSegment>> allMatches, String sessionId) {
        if (allMatches.isEmpty()) {
            log.info("[memory-loading] event=completed sessionId={} total=0", sessionId);
            return List.of();
        }
        
        List<TextSegment> allSegments = allMatches.stream()
            .map(EmbeddingMatch::embedded)
            .collect(Collectors.toList());
        
        log.debug("[memory-loading] event=raw_loaded sessionId={} rawCount={}", sessionId, allSegments.size());
        
        // 1. 按创建时间降序排序（最新的在前）
        List<TextSegment> sortedByTime = MemoryUtils.sortSegmentsByTimeDesc(allSegments);
        
        // 2. 取最近 20 条（必选）
        int recentCount = Math.min(20, sortedByTime.size());
        List<TextSegment> recentSegments = sortedByTime.subList(0, recentCount);
        
        // 3. 从剩余消息中筛选重要性 >= 0.8 的
        List<TextSegment> remainingSegments = sortedByTime.subList(recentCount, sortedByTime.size());
        List<TextSegment> importantSegments = filterImportantMessages(remainingSegments);
        
        // 4. 合并后按时间升序排序（最旧的在前，确保对话连贯性）
        List<TextSegment> combinedSegments = mergeAndSortByTimeAsc(recentSegments, importantSegments);
        
        // 5. 转换为 ChatMessage
        List<ChatMessage> messages = combinedSegments.stream()
            .map(MemoryUtils::convertToChatMessage)
            .toList();
        
        log.info("[memory-loading] event=completed sessionId={} total={} recent={} importantSupplement={}",
            sessionId, messages.size(), recentCount, importantSegments.size());
        
        return messages;
    }
    
    /**
     * 筛选重要性 >= 0.8 的消息
     */
    private List<TextSegment> filterImportantMessages(List<TextSegment> segments) {
        return segments.stream()
            .filter(segment -> {
                Double score = segment.metadata().getDouble("importance_score");
                return score != null && score >= 0.8;
            })
            .toList();
    }
    
    /**
     * 合并并按时间升序排序（最旧的在前）
     */
    private List<TextSegment> mergeAndSortByTimeAsc(List<TextSegment> recent, List<TextSegment> important) {
        List<TextSegment> combined = new ArrayList<>();
        combined.addAll(recent);
        combined.addAll(important);
        
        return MemoryUtils.sortSegmentsByTimeAsc(combined);
    }
}

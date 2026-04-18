package com.example.aihelper.memory;

import com.example.aihelper.utils.MemoryUtils;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 记忆筛选策略（纯算法）
 *
 * 职责：统一“保留最近 N 条 + 按重要性阈值筛选删除”的规则。
 */
@Component
public class MemorySelectionPolicy {

    /**
     * 计算应删除的记忆列表。
     *
     * @param sessionMatches 会话内全部记忆
     * @param keepRecentCount 保留最近条数
     * @param importanceThreshold 重要性阈值（低于该值将删除）
     * @return 应删除的记忆
     */
    public List<EmbeddingMatch<TextSegment>> selectMessagesToDelete(
            List<EmbeddingMatch<TextSegment>> sessionMatches,
            int keepRecentCount,
            double importanceThreshold) {
        List<EmbeddingMatch<TextSegment>> sortedMatches = MemoryUtils.sortMatchesByTimeDesc(sessionMatches);
        int keepCount = Math.min(keepRecentCount, sortedMatches.size());

        return sortedMatches.subList(keepCount, sortedMatches.size()).stream()
                .filter(match -> {
                    Double importanceScore = match.embedded().metadata().getDouble("importance_score");
                    return importanceScore != null && importanceScore < importanceThreshold;
                })
                .toList();
    }
}

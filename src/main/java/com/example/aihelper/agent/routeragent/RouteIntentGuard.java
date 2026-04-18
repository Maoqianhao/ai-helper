package com.example.aihelper.agent.routeragent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * 路由意图防护器。
 *
 * 目标：在不改变现有路由主流程的前提下，降低明显误路由概率。
 */
@Component
public class RouteIntentGuard {

    private final List<String> sqlKeywords;
    private final List<String> amapKeywords;
    private final List<String> interviewKeywords;

    public RouteIntentGuard(RouteGuardProperties properties) {
        this.sqlKeywords = normalizeKeywords(properties.getSqlKeywords(), List.of("sql", "数据库", "表", "查询", "统计", "报表", "mysql", "count"));
        this.amapKeywords = normalizeKeywords(properties.getAmapKeywords(), List.of("地图", "天气", "路线", "导航", "位置", "附近", "高德", "路况"));
        this.interviewKeywords = normalizeKeywords(properties.getInterviewKeywords(), List.of("面试", "刷题", "题目", "问答", "八股", "模拟面试", "算法题"));
    }

    public boolean isHighConfidenceAmap(String request) {
        String normalized = normalize(request);
        boolean amap = containsAny(normalized, amapKeywords);
        boolean conflict = hasConflict(normalized, sqlKeywords, interviewKeywords);
        return amap && !conflict;
    }

    public boolean isHighConfidenceInterview(String request) {
        String normalized = normalize(request);
        boolean interview = containsAny(normalized, interviewKeywords);
        boolean conflict = hasConflict(normalized, sqlKeywords, amapKeywords);
        return interview && !conflict;
    }

    private static String normalize(String request) {
        if (request == null) {
            return "";
        }
        return request.toLowerCase(Locale.ROOT);
    }

    private static boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    private static boolean hasConflict(String text, List<String> primaryConflicts, List<String> secondaryConflicts) {
        return containsAny(text, primaryConflicts) || containsAny(text, secondaryConflicts);
    }

    private static List<String> normalizeKeywords(List<String> configured, List<String> defaults) {
        if (configured == null || configured.isEmpty()) {
            return defaults;
        }
        List<String> normalized = configured.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
        return normalized.isEmpty() ? defaults : normalized;
    }
}

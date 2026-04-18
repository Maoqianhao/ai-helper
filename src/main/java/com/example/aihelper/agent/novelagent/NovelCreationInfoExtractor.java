package com.example.aihelper.agent.novelagent;

import com.example.aihelper.agent.thinkpattern.ReactThought;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 小说创作信息提取器。
 */
@Slf4j
final class NovelCreationInfoExtractor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private NovelCreationInfoExtractor() {
    }

    static Map<String, String> extract(String userMessage, ReactThought reactThought, String defaultAudience, String defaultStyle) {
        try {
            log.info("[novel-extract] event=start");

            String extractionPrompt = NovelPrompts.buildExtractionPrompt(userMessage);
            String jsonResponse = reactThought.reason(extractionPrompt);

            Map<String, String> result = parseExtractionJson(jsonResponse);
            if (result.get("topic") == null || result.get("topic").trim().isEmpty()) {
                throw new IllegalStateException("无法从用户消息中提取主题信息");
            }

            result.putIfAbsent("audience", defaultAudience);
            result.putIfAbsent("style", defaultStyle);

            log.info("[novel-extract] event=completed topic={} audience={} style={}",
                    result.get("topic"),
                    result.get("audience"),
                    result.get("style"));
            return result;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[novel-extract] event=error message={}", e.getMessage(), e);
            throw new IllegalStateException("信息提取失败: " + e.getMessage(), e);
        }
    }

    private static Map<String, String> parseExtractionJson(String jsonResponse) {
        Map<String, String> result = new HashMap<>();
        try {
            JsonNode root = OBJECT_MAPPER.readTree(jsonResponse);
            putIfTextNode(result, "topic", root.get("topic"));
            putIfTextNode(result, "audience", root.get("audience"));
            putIfTextNode(result, "style", root.get("style"));
        } catch (Exception e) {
            log.warn("[novel-extract] event=json_parse_failed fallback=raw_response message={}", e.getMessage());
            result.put("topic", jsonResponse.trim());
        }
        return result;
    }

    private static void putIfTextNode(Map<String, String> target, String key, JsonNode node) {
        if (node != null && !node.isNull()) {
            String value = node.asText();
            if (value != null && !value.trim().isEmpty()) {
                target.put(key, value.trim());
            }
        }
    }
}

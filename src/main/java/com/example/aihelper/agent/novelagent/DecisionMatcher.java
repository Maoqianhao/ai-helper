package com.example.aihelper.agent.novelagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 决策匹配工具
 *
 * 职责：将模型输出（文本或结构化 JSON）解析为布尔决策。
 */
final class DecisionMatcher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String[] NEGATIVE_TOKENS = {
            "not approved", "notapproved", "disapproved", "rejected",
            "未通过", "不通过", "未批准", "不批准", "否决", "驳回"
    };
    private static final Pattern JSON_CODE_BLOCK_PATTERN = Pattern.compile("(?s)```(?:json)?\\s*(\\{.*?})\\s*```");

    private DecisionMatcher() {
    }

    static boolean matches(String modelOutput, String expectedTag, String... fallbackKeywords) {
        if (modelOutput == null || modelOutput.isBlank()) {
            return false;
        }

        String normalizedExpected = normalizeToken(expectedTag);
        Optional<Boolean> structuredDecision = parseStructuredDecision(modelOutput, normalizedExpected);
        if (structuredDecision.isPresent()) {
            return structuredDecision.get();
        }

        String decisionSnippet = extractDecisionSnippet(modelOutput);
        String loweredSnippet = decisionSnippet.toLowerCase(Locale.ROOT);
        String loweredOutput = modelOutput.toLowerCase(Locale.ROOT);

        String normalizedSnippet = normalizeToken(decisionSnippet);
        if (containsExactToken(loweredSnippet, expectedTag) || normalizedSnippet.equals(normalizedExpected)) {
            return true;
        }

        for (String keyword : fallbackKeywords) {
            if (keyword != null && !keyword.isBlank() &&
                    (containsExactToken(loweredSnippet, keyword) || normalizedSnippet.equals(normalizeToken(keyword)))) {
                return true;
            }
        }

        if (containsNegativeDecision(loweredSnippet, normalizedExpected) ||
                containsNegativeDecision(loweredOutput, normalizedExpected)) {
            return false;
        }

        String normalizedOutput = normalizeToken(modelOutput);
        if (containsExactToken(loweredOutput, expectedTag) || normalizedOutput.equals(normalizedExpected)) {
            return true;
        }
        return false;
    }

    private static Optional<Boolean> parseStructuredDecision(String modelOutput, String normalizedExpected) {
        Optional<Boolean> direct = parseStructuredDecisionInternal(modelOutput, normalizedExpected);
        if (direct.isPresent()) {
            return direct;
        }
        String jsonPayload = extractJsonPayload(modelOutput);
        if (jsonPayload == null) {
            return Optional.empty();
        }
        return parseStructuredDecisionInternal(jsonPayload, normalizedExpected);
    }

    private static Optional<Boolean> parseStructuredDecisionInternal(String payload, String normalizedExpected) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);

            JsonNode approvedNode = root.get("approved");
            if (approvedNode != null && approvedNode.isBoolean()) {
                return Optional.of(approvedNode.asBoolean());
            }

            String decision = extractDecisionToken(root);
            if (decision == null || decision.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(normalizeToken(decision).contains(normalizedExpected));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static String extractJsonPayload(String modelOutput) {
        Matcher matcher = JSON_CODE_BLOCK_PATTERN.matcher(modelOutput);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private static String extractDecisionSnippet(String modelOutput) {
        String[] lines = modelOutput.split("\\R");
        for (String line : lines) {
            if (line != null && !line.isBlank()) {
                return line.trim();
            }
        }
        return modelOutput.trim();
    }

    private static String extractDecisionToken(JsonNode root) {
        String[] keys = {"decision", "status", "result", "label"};
        for (String key : keys) {
            JsonNode node = root.get(key);
            if (node != null && !node.isNull()) {
                String value = node.asText();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }

    private static String normalizeToken(String value) {
        return value == null ? "" : value.toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace("\"", "")
                .replace("'", "")
                .trim();
    }

    private static boolean containsNegativeDecision(String loweredOutput, String normalizedExpected) {
        for (String token : NEGATIVE_TOKENS) {
            if (loweredOutput.contains(token)) {
                return true;
            }
        }
        return "approved".equals(normalizedExpected)
                && (loweredOutput.contains("not approved")
                || loweredOutput.contains("未通过")
                || loweredOutput.contains("不通过"));
    }

    private static boolean containsExactToken(String loweredOutput, String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        String loweredToken = token.toLowerCase();
        if (loweredToken.chars().anyMatch(ch -> Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN)) {
            return loweredOutput.contains(loweredToken);
        }
        return loweredOutput.matches("(?s).*\\b" + java.util.regex.Pattern.quote(loweredToken) + "\\b.*");
    }
}

package com.example.aihelper.tools;

import com.example.aihelper.utils.TraceIdUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 面试题搜索工具
 */
@Slf4j
public class InterviewQuestionTool {
    private static final String TOOL_NAME = "interviewQuestionSearch";
    private static final int KEYWORD_MAX_LENGTH = 80;
    private static final Pattern SAFE_KEYWORD_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\s+\\-_.#]+$");

    /**
     * 从面试鸭网站获取关键词相关的面试题列表
     *
     * @param keyword 搜索关键词（如"redis"、"java多线程"）
     * @return 面试题列表，若失败则返回错误信息
     */
    @Tool(name = "interviewQuestionSearch", value = """
            Retrieves relevant interview questions from mianshiya.com based on a keyword.
            Use this tool when the user asks for interview questions about specific technologies,
            programming concepts, or job-related topics. The input should be a clear search term.
            """
    )
    public String searchInterviewQuestions(@P(value = "the keyword to search") String keyword) {
        String traceId = TraceIdUtil.getTraceId();
        String normalizedKeyword = normalizeKeyword(keyword);
        int argsHash = normalizedKeyword.hashCode();
        log.info("[traceId={}] [tool-call] toolName={} argsHash={}", traceId, TOOL_NAME, argsHash);

        String validationError = validateKeyword(normalizedKeyword);
        if (validationError != null) {
            log.warn("[traceId={}] [tool-guard] blocked toolName={} reason={} argsHash={}",
                    traceId, TOOL_NAME, validationError, argsHash);
            return "请输入有效的面试关键词（1-80字符，仅支持中英文、数字、空格及常见符号）。";
        }

        List<String> questions = new ArrayList<>();
        // 构建搜索URL（编码关键词以支持中文）
        String encodedKeyword = URLEncoder.encode(normalizedKeyword, StandardCharsets.UTF_8);
        String url = "https://www.mianshiya.com/search/all?searchText=" + encodedKeyword;
        // 发送请求并解析页面
        Document doc;
        long start = System.currentTimeMillis();
        try {
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .get();
        } catch (IOException e) {
            log.warn("[traceId={}] [tool-call] toolName={} success=false durationMs={} errorType={} argsHash={}",
                    traceId, TOOL_NAME, System.currentTimeMillis() - start, e.getClass().getSimpleName(), argsHash);
            return "面试题检索暂时失败，请稍后重试。";
        }
        // 提取面试题
        Elements questionElements = doc.select(".ant-table-cell > a");
        questionElements.forEach(el -> questions.add(el.text().trim()));
        log.info("[traceId={}] [tool-call] toolName={} success=true durationMs={} resultCount={} argsHash={}",
                traceId, TOOL_NAME, System.currentTimeMillis() - start, questions.size(), argsHash);
        if (questions.isEmpty()) {
            return "未检索到相关面试题，请换个更具体的关键词试试。";
        }
        return String.join("\n", questions);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private String validateKeyword(String keyword) {
        if (keyword.isEmpty()) {
            return "empty_keyword";
        }
        if (keyword.length() > KEYWORD_MAX_LENGTH) {
            return "keyword_too_long";
        }
        if (!SAFE_KEYWORD_PATTERN.matcher(keyword).matches()) {
            return "keyword_contains_unsafe_chars";
        }
        return null;
    }
}
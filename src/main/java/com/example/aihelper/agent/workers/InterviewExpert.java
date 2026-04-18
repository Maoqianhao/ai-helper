package com.example.aihelper.agent.workers;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;

/**
 * 面试专家接口。
 */
public interface InterviewExpert {
    /**
     * 处理面试问答、刷题与模拟面试请求。
     */
    @SystemMessage(fromResource = "templates/systemmessage/interview_expert_system_message.txt")
    @Tool("专门处理面试题生成、技术问答、刷题、模拟面试类问题")
    String process(String request);
}

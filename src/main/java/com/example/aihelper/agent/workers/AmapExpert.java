package com.example.aihelper.agent.workers;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;

/**
 * 高德地图专家接口。
 */
public interface AmapExpert {
    /**
     * 处理地图、路线与天气类请求。
     */
    @SystemMessage(fromResource = "templates/systemmessage/amap_expert_system_message.txt")
    @Tool("专门处理天气、地理位置、路线规划、周边搜索类问题")
    String askAmap(String request);
}

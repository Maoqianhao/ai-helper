package com.example.aihelper.agent.routeragent;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 路由防误调配置。
 *
 * 通过 application.yaml 的 route-guard 节点覆盖默认关键词。
 */
@Component
@ConfigurationProperties(prefix = "route-guard")
public class RouteGuardProperties {

    private List<String> sqlKeywords = List.of("sql", "数据库", "表", "查询", "统计", "报表", "mysql", "count");
    private List<String> amapKeywords = List.of("地图", "天气", "路线", "导航", "位置", "附近", "高德", "路况");
    private List<String> interviewKeywords = List.of("面试", "刷题", "题目", "问答", "八股", "模拟面试", "算法题");

    public List<String> getSqlKeywords() {
        return sqlKeywords;
    }

    public void setSqlKeywords(List<String> sqlKeywords) {
        this.sqlKeywords = sqlKeywords;
    }

    public List<String> getAmapKeywords() {
        return amapKeywords;
    }

    public void setAmapKeywords(List<String> amapKeywords) {
        this.amapKeywords = amapKeywords;
    }

    public List<String> getInterviewKeywords() {
        return interviewKeywords;
    }

    public void setInterviewKeywords(List<String> interviewKeywords) {
        this.interviewKeywords = interviewKeywords;
    }
}

package com.example.aihelper.agent.workers;

/**
 * SQL 专家接口。
 */
public interface SqlExpert {

    /**
     * 处理数据库查询请求并返回结果文本。
     */
    String queryDatabase(String request);
}

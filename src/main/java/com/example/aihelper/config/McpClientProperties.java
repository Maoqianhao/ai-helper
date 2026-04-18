package com.example.aihelper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MCP 客户端配置属性。
 */
@ConfigurationProperties(prefix = "mcp")
public class McpClientProperties {

    private String nodeHome;
    private String npxExecutable = "npx.cmd";
    private boolean logEvents = false;

    public String getNodeHome() {
        return nodeHome;
    }

    public void setNodeHome(String nodeHome) {
        this.nodeHome = nodeHome;
    }

    public String getNpxExecutable() {
        return npxExecutable;
    }

    public void setNpxExecutable(String npxExecutable) {
        this.npxExecutable = npxExecutable;
    }

    public boolean isLogEvents() {
        return logEvents;
    }

    public void setLogEvents(boolean logEvents) {
        this.logEvents = logEvents;
    }
}

package com.example.aihelper.config;

import dev.langchain4j.mcp.McpToolProvider;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.mcp.client.transport.stdio.StdioMcpTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(McpClientProperties.class)
@Slf4j
public class McpConfig {

    @Value("${bigmodel.api-key}")
    private String zhipuWebApiKey;

    @Value("${gaodemodel.api-key}")
    private String amapMapsApiKey;

    private final McpClientProperties mcpClientProperties;

    public McpConfig(McpClientProperties mcpClientProperties) {
        this.mcpClientProperties = mcpClientProperties;
    }

    /**
     * 统一注册 MCP 工具提供器。
     */
    @Bean
    public McpToolProvider mcpToolProvider() {
        log.info("[mcp-config] event=provider_init_start");
        McpClient zhiPuClient = createZhipuClient();
        McpClient amapClient = createAmapClient();
        McpToolProvider provider = McpToolProvider.builder()
                .mcpClients(zhiPuClient, amapClient)
                .build();
        log.info("[mcp-config] event=provider_init_completed clientCount=2");
        return provider;
    }

    private McpClient createZhipuClient() {
        requireText(zhipuWebApiKey, "bigmodel.api-key");
        log.info("[mcp-config] event=client_init_start client=zhipu-web-search");
        return new DefaultMcpClient.Builder()
                .key("zhipu-web-search")
                .transport(new HttpMcpTransport.Builder()
                        .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=" + zhipuWebApiKey)
                        .build())
                .build();
    }

    public McpClient createAmapClient() {
        String nodeHome = mcpClientProperties.getNodeHome();
        String npxExecutable = mcpClientProperties.getNpxExecutable();
        requireText(amapMapsApiKey, "gaodemodel.api-key");
        requireText(nodeHome, "mcp.node-home");
        requireText(npxExecutable, "mcp.npx-executable");
        log.info("[mcp-config] event=client_init_start client=amap-maps nodeHome={}", nodeHome);

        Map<String, String> env = new HashMap<>();
        env.put("AMAP_MAPS_API_KEY", amapMapsApiKey);
        env.put("PATH", nodeHome + ";" + System.getenv("PATH"));

        List<String> command = List.of(
                "cmd.exe",
                "/c",
                nodeHome + "\\" + npxExecutable,
                "-y",
                "@amap/amap-maps-mcp-server"
        );

        McpTransport transport = new StdioMcpTransport.Builder()
                .command(command)
                .environment(env)
                .logEvents(mcpClientProperties.isLogEvents())
                .build();

        return new DefaultMcpClient.Builder()
                .transport(transport)
                .build();
    }

    private void requireText(String value, String configKey) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("缺少配置项: " + configKey);
        }
    }
}

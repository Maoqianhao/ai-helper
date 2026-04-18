package com.example.aihelper.config;

import dev.langchain4j.experimental.rag.content.retriever.sql.SqlDatabaseContentRetriever;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * SQL RAG 检索器配置。
 */
@Configuration
public class RagSqlConfig {

    private final ChatModel qwenChatModel;
    private final DataSource dataSource;

    public RagSqlConfig(ChatModel qwenChatModel, DataSource dataSource) {
        this.qwenChatModel = qwenChatModel;
        this.dataSource = dataSource;
    }

    @Bean
    public ContentRetriever createSqlAdvancedRag() {
        return SqlDatabaseContentRetriever.builder()
                .dataSource(dataSource)
                .chatModel(qwenChatModel)
                .build();
    }

}

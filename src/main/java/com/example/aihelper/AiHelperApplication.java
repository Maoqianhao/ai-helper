package com.example.aihelper;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动入口。
 */
@SpringBootApplication
@EnableScheduling
@MapperScan({
    "com.example.aihelper.ai.memory",
    "com.example.aihelper.ai.rag.metadata",
    "com.example.aihelper.mapper"
})
public class AiHelperApplication {

    /**
     * 主函数入口。
     */
    public static void main(String[] args) {
        SpringApplication.run(AiHelperApplication.class, args);
    }
}

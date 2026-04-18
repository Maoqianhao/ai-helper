package com.example.aihelper.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.stream.Stream;

/**
 * CORS 跨域配置。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*}")
    private String allowedOriginPatterns;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String[] originPatterns = Stream.of(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);

        registry.addMapping("/**")
                .allowedOriginPatterns(originPatterns.length == 0
                        ? new String[]{"http://localhost:*"}
                        : originPatterns)
                // 允许的请求方法
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                // 允许的请求头
                .allowedHeaders("*")
                // 允许携带凭证（cookies、authorization headers等）
                .allowCredentials(allowCredentials)
                // 预检请求的有效期（秒）
                .maxAge(3600);
    }
}

package com.example.aihelper.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 鉴权相关 WebMvc 配置。
 */
@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 已关闭鉴权拦截（调试模式）
    }
}

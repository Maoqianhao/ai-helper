package com.example.aihelper.prompt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public final class PromptTemplateLoader {

    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private PromptTemplateLoader() {
    }

    public static String load(@NonNull String path, @NonNull String fallbackTemplate) {
        String safePath = Objects.requireNonNull(path, "path");
        String safeFallbackTemplate = Objects.requireNonNull(fallbackTemplate, "fallbackTemplate");
        String cached = CACHE.get(safePath);
        if (cached != null) {
            return cached;
        }
        String loaded = readTemplate(safePath, safeFallbackTemplate);
        CACHE.put(safePath, loaded);
        return loaded;
    }

    private static String readTemplate(@NonNull String path, @NonNull String fallbackTemplate) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("[prompt-template] event=load_failed path={} fallback=true errorType={}",
                    path, e.getClass().getSimpleName());
            return fallbackTemplate;
        }
    }
}

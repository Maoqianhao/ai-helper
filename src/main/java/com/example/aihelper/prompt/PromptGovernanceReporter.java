package com.example.aihelper.prompt;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class PromptGovernanceReporter {

    @PostConstruct
    public void report() {
        List<PromptRegistry.PromptSpec> prompts = PromptRegistry.all();
        long hardcodedCount = prompts.stream()
                .filter(spec -> spec.source() == PromptRegistry.PromptSource.HARDCODED_TEMPLATE)
                .count();
        log.info("[prompt-governance] event=registry_loaded total={} hardcoded={} resource={}",
                prompts.size(),
                hardcodedCount,
                prompts.size() - hardcodedCount);
    }
}

package com.kickdenstoff.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lmstudio")
public record LmStudioProperties(
        String baseUrl,
        String model,
        double temperature,
        int maxTokens
) {
}

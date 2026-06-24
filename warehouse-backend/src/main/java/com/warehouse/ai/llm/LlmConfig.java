package com.warehouse.ai.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmConfig {
    private String baseUrl;
    private String apiKey;
    private String model;
    private Integer maxTokens = 4096;
    private Double temperature = 0.7;
    private String systemPrompt;
}

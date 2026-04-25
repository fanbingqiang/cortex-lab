package com.cortex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cortex.llm")
public class LlmConfig {
    private ModelConfig deepseek;
    private ModelConfig gpt4oMini;
    
    @Data
    public static class ModelConfig {
        private String apiKey;
        private String baseUrl;
        private String model;
    }
}

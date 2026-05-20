package com.cortex.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cortex.llm")
public class LlmConfig {
    // 默认 LLM 配置（apiKey, baseUrl, model）
    private ModelConfig defaultConfig;


    @Data
    public static class ModelConfig {
        private String apiKey;    // API 密钥
        private String baseUrl;   // API 地址
        private String model;     // 模型名称
    }
}

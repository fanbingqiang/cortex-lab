package com.cortex.llm;

import lombok.Data;

import java.util.List;

@Data
public class LlmRequest {
    // LLM API 请求参数
    private String model;           // 模型名称
    private List<Message> messages; // 消息列表
    private Double temperature;     // 采样温度
    private Integer maxTokens;      // 最大 token 数
    private Boolean stream;         // 是否流式输出
    
    @Data
    public static class Message {
        private String role;    // 角色（system/user/assistant）
        private String content; // 消息内容
        
        public Message() {}
        
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
    
    // 创建带 system prompt 的请求
    public static LlmRequest create(String model, String systemPrompt, String userMessage) {
        LlmRequest request = new LlmRequest();
        request.setModel(model);
        request.setMessages(List.of(
            new Message("system", systemPrompt),
            new Message("user", userMessage)
        ));
        request.setTemperature(0.7);
        request.setMaxTokens(4096);
        return request;
    }
    
    // 创建只有 user 消息的请求
    public static LlmRequest create(String model, String userMessage) {
        LlmRequest request = new LlmRequest();
        request.setModel(model);
        request.setMessages(List.of(
            new Message("user", userMessage)
        ));
        request.setTemperature(0.7);
        request.setMaxTokens(4096);
        return request;
    }
}

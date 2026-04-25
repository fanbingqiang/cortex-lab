package com.cortex.llm;

import lombok.Data;

@Data
public class LlmResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private Choice[] choices;
    private Usage usage;
    
    @Data
    public static class Choice {
        private Integer index;
        private Message message;
        private String finishReason;
    }
    
    @Data
    public static class Message {
        private String role;
        private String content;
    }
    
    @Data
    public static class Usage {
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
    }
    
    public String getContent() {
        if (choices != null && choices.length > 0 && choices[0].getMessage() != null) {
            return choices[0].getMessage().getContent();
        }
        return null;
    }
}

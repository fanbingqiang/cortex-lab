package com.cortex.lab.dto;

import lombok.Data;

@Data
public class GlobalChatRequest {
    private String conversationId;
    private String message;
    private String userId;
    private String currentCode;
    private String knowledgePoint;
}

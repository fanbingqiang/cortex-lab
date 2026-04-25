package com.cortex.lab.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;
    private String message;
    private String currentCode;
    private String lastOutput;
}

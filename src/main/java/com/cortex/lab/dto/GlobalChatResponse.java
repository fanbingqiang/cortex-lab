package com.cortex.lab.dto;

import lombok.Data;

import java.util.List;

@Data
public class GlobalChatResponse {
    private String conversationId;
    private String reply;
    private boolean incomplete;
    private String askReason;
    private List<String> suggestions;
}

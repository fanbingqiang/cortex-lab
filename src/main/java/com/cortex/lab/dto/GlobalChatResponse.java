package com.cortex.lab.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GlobalChatResponse {
    private String conversationId;
    private String reply;
    private boolean incomplete;
    private String askReason;
    private List<String> suggestions;
    private Map<String, Object> action;
    private boolean silent; // true=静默执行action，不在聊天框显示回复
}

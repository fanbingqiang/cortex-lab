package com.cortex.lab.dto;

import lombok.Data;

@Data
public class GlobalChatRequest {
    private String conversationId;
    private String message;
    private String userId;
    private String currentCode;
    private String knowledgePoint;
    private String originalCode;      // 原始代码（来自题目的 trapCode）
    private String executionOutput;   // 用户上次运行代码的输出
}

package com.cortex.lab.dto;

import lombok.Data;

@Data
public class TutorExplainRequest {
    private Long questionId;
    private String tip;       // 要解释的知识点标题
    private String userId;
}
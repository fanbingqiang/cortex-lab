package com.cortex.lab.dto;

import lombok.Data;

@Data
public class ExecuteRequest {
    private String sessionId;
    private String code;
}

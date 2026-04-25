package com.cortex.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteResponse {
    private String stdout;
    private String stderr;
    private int exitCode;
    private boolean success;
    private String error;
}

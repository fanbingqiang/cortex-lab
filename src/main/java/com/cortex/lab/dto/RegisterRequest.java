package com.cortex.lab.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    // 注册请求
    private String username;
    private String password;
    private String email;
}

package com.cortex.lab.dto;

import lombok.Data;

@Data
public class LoginRequest {
    // 登录请求
    private String username;
    private String password;
}

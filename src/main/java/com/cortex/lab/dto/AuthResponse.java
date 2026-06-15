package com.cortex.lab.dto;

import lombok.Data;

@Data
public class AuthResponse {
    // 登录/注册响应
    private String token;    // JWT token
    private String userId;   // 用户ID
    private String username; // 用户名
    private String email;    // 邮箱
    private String avatar;   // 头像URL
    private String role;     // 角色
}

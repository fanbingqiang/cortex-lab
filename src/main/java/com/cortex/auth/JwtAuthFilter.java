package com.cortex.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class JwtAuthFilter implements Filter {
    // JWT 认证过滤器，保护 /api/admin/* 和 /api/auth/me 路径

    private final JwtService jwtService;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        // 只需要拦截 /api/auth/me
        boolean needsAuth = "/api/auth/me".equals(path);

        if (needsAuth) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Map<String, String> claims = jwtService.verifyToken(token);
                if (claims != null) {
                    request.setAttribute("userId", claims.get("userId"));
                    request.setAttribute("role", claims.get("role"));
                    request.setAttribute("orgId", claims.get("orgId"));
                    chain.doFilter(request, response);
                    return;
                }
            }
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(401);
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或Token已过期\",\"data\":null}");
            return;
        }

        // 非受保护路径：尝试解析 token，但不强制
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            Map<String, String> claims = jwtService.verifyToken(authHeader.substring(7));
            if (claims != null) {
                request.setAttribute("userId", claims.get("userId"));
                request.setAttribute("role", claims.get("role"));
                request.setAttribute("orgId", claims.get("orgId"));
            }
        }
        chain.doFilter(request, response);
    }
}

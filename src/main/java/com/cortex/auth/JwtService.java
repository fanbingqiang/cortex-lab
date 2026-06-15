package com.cortex.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class JwtService {
    // JWT Token 服务：生成和验证

    @Value("${cortex.auth.jwt-secret:cortex-jwt-secret-key-2024}")
    private String jwtSecret;

    @Value("${cortex.auth.jwt-expire-hours:72}")
    private int expireHours;

    /** 生成 JWT Token */
    public String generateToken(String userId, String role, String orgId) {
        long exp = System.currentTimeMillis() / 1000 + expireHours * 3600;
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64UrlEncode(
            "{\"userId\":\"" + escapeJson(userId) + "\",\"role\":\"" + escapeJson(role) +
            "\",\"orgId\":\"" + escapeJson(orgId) + "\",\"exp\":" + exp + "}"
        );
        String signature = hmacSha256(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    /** 验证并解析 Token，返回 null 表示无效 */
    public Map<String, String> verifyToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            String expectedSig = hmacSha256(parts[0] + "." + parts[1]);
            if (!expectedSig.equals(parts[2])) {
                return null;
            }
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            // 简化的 JSON 解析
            String userId = extractJsonStr(payload, "userId");
            String role = extractJsonStr(payload, "role");
            String orgId = extractJsonStr(payload, "orgId");
            long exp = Long.parseLong(extractJsonNum(payload, "exp"));
            if (System.currentTimeMillis() / 1000 > exp) {
                return null;
            }
            return Map.of("userId", userId, "role", role, "orgId", orgId);
        } catch (Exception e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            return null;
        }
    }

    private String hmacSha256(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64UrlEncode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

    private String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private String base64UrlEncode(String data) {
        return base64UrlEncode(data.getBytes(StandardCharsets.UTF_8));
    }

    private String escapeJson(String s) {
        return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String extractJsonStr(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) {
            return "";
        }
        int colon = json.indexOf(':', idx + key.length() + 2);
        int start = json.indexOf('"', colon + 1);
        int end = json.indexOf('"', start + 1);
        return start < 0 || end < 0 ? "" : json.substring(start + 1, end);
    }

    private String extractJsonNum(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) {
            return "0";
        }
        int colon = json.indexOf(':', idx + key.length() + 2);
        int end = json.indexOf(',', colon + 1);
        if (end < 0) {
            end = json.indexOf('}', colon + 1);
        }
        return end < 0 ? "0" : json.substring(colon + 1, end).trim();
    }
}

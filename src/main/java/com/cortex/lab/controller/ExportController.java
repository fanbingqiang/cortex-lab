package com.cortex.lab.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.cortex.dto.ApiResponse;
import com.cortex.lab.dto.UserInfoDTO;
import com.cortex.lab.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    // 导出控制器：导出用户能力图谱

    private final AuthService authService;

    @GetMapping("/capability")
    public ResponseEntity<byte[]> exportCapabilityMap(
        // 导出用户能力图谱（JSON 格式）
            @RequestParam String userId,
            @RequestParam(defaultValue = "json") String format) {
        try {
            UserInfoDTO userInfo = authService.getUserInfo(userId);

            Map<String, Object> capabilityMap = new LinkedHashMap<>();
            capabilityMap.put("exportTime", new Date().toString());
            Map<String, Object> userMap = new LinkedHashMap<>();
            userMap.put("username", userInfo.getUsername());
            userMap.put("userId", userInfo.getUserId());
            userMap.put("skillLevel", userInfo.getSkillLevel());
            userMap.put("totalQuestionsAnswered", userInfo.getTotalQuestionsAnswered());
            userMap.put("totalCorrect", userInfo.getTotalCorrect());
            userMap.put("accuracy", userInfo.getAccuracy());
            userMap.put("studyStreak", userInfo.getStudyStreak());
            userMap.put("masteredCount", userInfo.getMasteredCount());
            capabilityMap.put("user", userMap);
            capabilityMap.put("weakAreas", userInfo.getWeakAreas());

            if ("json".equalsIgnoreCase(format)) {
                String jsonStr = JSON.toJSONString(capabilityMap, JSONWriter.Feature.PrettyFormat);
                byte[] bytes = jsonStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setContentDispositionFormData("attachment", "capability-map.json");
                return ResponseEntity.ok().headers(headers).body(bytes);
            }

            // Default to JSON
            return exportCapabilityMap(userId, "json");
        } catch (Exception e) {
            log.error("导出能力图谱失败", e);
            throw new RuntimeException("导出失败: " + e.getMessage());
        }
    }
}

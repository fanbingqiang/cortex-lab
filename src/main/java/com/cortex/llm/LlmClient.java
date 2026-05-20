package com.cortex.llm;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.cortex.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class LlmClient {
    // LLM API 调用客户端，支持流式和非流式调用
    
    private final LlmConfig llmConfig;
    
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
    
    // 使用默认配置调用 LLM
    public LlmResponse chat(LlmRequest request) {
        return chat(llmConfig.getDefaultConfig().getBaseUrl(), llmConfig.getDefaultConfig().getApiKey(), request);
    }
    
    public LlmResponse chat(String baseUrl, String apiKey, LlmRequest request) {
        if (StrUtil.isBlank(apiKey) || "your-api-key-here".equals(apiKey)) {
            throw new RuntimeException("请配置有效的API Key");
        }
        
        String url = baseUrl + "/chat/completions";
        String jsonBody = JSON.toJSONString(request);
        
        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();
        
        try (Response response = httpClient.newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("LLM API调用失败: status={}, body={}", response.code(), errorBody);
                throw new RuntimeException("LLM API调用失败: " + response.code());
            }
            
            String responseBody = response.body().string();
            log.debug("LLM API响应: {}", responseBody);
            
            return JSON.parseObject(responseBody, LlmResponse.class);
        } catch (IOException e) {
            log.error("LLM API调用异常", e);
            throw new RuntimeException("LLM API调用异常: " + e.getMessage(), e);
        }
    }
    
    // 简易聊天：带 system prompt 的调用
    public String chatSimple(String systemPrompt, String userMessage) {
        LlmRequest request = LlmRequest.create(llmConfig.getDefaultConfig().getModel(), systemPrompt, userMessage);
        LlmResponse response = chat(request);
        return response.getContent();
    }

    // 简易聊天：只有 user 消息
    public String chatSimple(String userMessage) {
        LlmRequest request = LlmRequest.create(llmConfig.getDefaultConfig().getModel(), userMessage);
        LlmResponse response = chat(request);
        return response.getContent();
    }

    // 使用自定义配置调用，未提供时回退到默认配置
    public String chatSimple(String apiKey, String baseUrl, String model, String systemPrompt, String userMessage) {
        if (apiKey == null || apiKey.isBlank() || "your-api-key-here".equals(apiKey)) {
            apiKey = llmConfig.getDefaultConfig().getApiKey();
        }
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = llmConfig.getDefaultConfig().getBaseUrl();
        }
        if (model == null || model.isBlank()) {
            model = llmConfig.getDefaultConfig().getModel();
        }
        LlmRequest request = LlmRequest.create(model, systemPrompt, userMessage);
        LlmResponse response = chat(baseUrl, apiKey, request);
        return response.getContent();
    }

    // ==================== 流式 SSE 调用 ====================

    // 流式聊天，使用默认配置
    public void chatStream(LlmRequest request, Consumer<String> onChunk, Runnable onComplete, Consumer<Exception> onError) {
        chatStream(llmConfig.getDefaultConfig().getBaseUrl(), llmConfig.getDefaultConfig().getApiKey(), request, onChunk, onComplete, onError);
    }

    // 流式聊天，指定 baseUrl 和 apiKey
    public void chatStream(String baseUrl, String apiKey, LlmRequest request, Consumer<String> onChunk, Runnable onComplete, Consumer<Exception> onError) {
        if (StrUtil.isBlank(apiKey) || "your-api-key-here".equals(apiKey)) {
            onError.accept(new RuntimeException("请配置有效的API Key"));
            return;
        }

        String url = baseUrl + "/chat/completions";
        request.setStream(true);
        String jsonBody = JSON.toJSONString(request);

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try {
            Response response = httpClient.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("LLM API调用失败: status={}, body={}", response.code(), errorBody);
                onError.accept(new RuntimeException("LLM API调用失败: " + response.code()));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    try {
                        com.alibaba.fastjson2.JSONObject json = JSON.parseObject(data);
                        if (json.containsKey("choices")) {
                            var choices = json.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                var choice = choices.getJSONObject(0);
                                if (choice.containsKey("delta")) {
                                    var delta = choice.getJSONObject("delta");
                                    String content = delta.getString("content");
                                    if (content != null) {
                                        onChunk.accept(content);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析SSE消息失败: {}", e.getMessage());
                    }
                }
            }
            reader.close();
            onComplete.run();
        } catch (Exception e) {
            log.error("LLM API流式调用异常", e);
            onError.accept(new RuntimeException("LLM API流式调用异常: " + e.getMessage(), e));
        }
    }

    // 流式聊天，完整参数版本
    public void chatStream(String apiKey, String baseUrl, String model, LlmRequest request, Consumer<String> onChunk, Runnable onComplete, Consumer<Exception> onError) {
        if (StrUtil.isBlank(apiKey) || "your-api-key-here".equals(apiKey)) {
            onError.accept(new RuntimeException("请配置有效的API Key"));
            return;
        }

        String url = baseUrl + "/chat/completions";
        request.setStream(true);
        String jsonBody = JSON.toJSONString(request);

        Request httpRequest = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try {
            Response response = httpClient.newCall(httpRequest).execute();
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("LLM API调用失败: status={}, body={}", response.code(), errorBody);
                onError.accept(new RuntimeException("LLM API调用失败: " + response.code()));
                return;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), "UTF-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        com.alibaba.fastjson2.JSONObject json = JSON.parseObject(data);
                        if (json.containsKey("choices")) {
                            var choices = json.getJSONArray("choices");
                            if (choices != null && !choices.isEmpty()) {
                                var choice = choices.getJSONObject(0);
                                if (choice.containsKey("delta")) {
                                    String content = choice.getJSONObject("delta").getString("content");
                                    if (content != null) onChunk.accept(content);
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析SSE消息失败: {}", e.getMessage());
                    }
                }
            }
            reader.close();
            onComplete.run();
        } catch (Exception e) {
            log.error("LLM API流式调用异常", e);
            onError.accept(new RuntimeException("LLM API流式调用异常: " + e.getMessage(), e));
        }
    }
}

package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cortex.lab.dto.GlobalChatRequest;
import com.cortex.lab.dto.GlobalChatResponse;
import com.cortex.lab.entity.*;
import com.cortex.lab.mapper.*;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {
    // 全局 AI 助手服务：支持对话、代码操作、RAG 检索、自我进化

    private final AssistantConfigMapper configMapper;
    private final AssistantConversationMapper conversationMapper;
    private final AssistantMessageMapper messageMapper;
    private final LlmClient llmClient;
    private final RagService ragService;
    private final EvolutionService evolutionService;
    private final QuestionBankMapper questionBankMapper;

    private static final String SYSTEM_PROMPT = """
你是一个智能编程导师"小C"，运行在代码练习平台 Cortex Lab 中。
你可以回答编程问题，也可以执行平台操作。用户可以要求你修改编辑器中的代码。
需要操作时在 action 中指定；不需要时 action 设为 null。

## 回答原则
- 简洁有针对性，不要啰嗦
- 需要操作时在 action 字段指定；不需要时 action 设为 null
- 用户想打开题目、运行代码等，直接执行不要问"要不要"

## 可用操作
- {"type":"switchTab","payload":"practice|questions|cards"} — 切换标签
- {"type":"selectQuestion","payload":题目ID} — 题库打开题目
- {"type":"loadToEditor","payload":题目ID} — 加载代码到编辑器
- {"type":"runCode"} — 运行代码
- {"type":"resetCode"} — 重置代码

## 代码修改操作
用户可以要求你直接修改编辑器中的代码。修改前先解释要改什么、为什么改，
然后在 action 中返回修改操作。如果修改涉及多处，一次只改一处。

1. modifyCode — 修改指定范围的代码
   先解释修改内容，再用以下格式：
   {"type":"modifyCode","payload":{"startLine":行号,"startCol":列号,"endLine":行号,"endCol":列号,"newText":"替换后的代码"}}
   注意：行号从1开始，列号从1开始

2. insertCode — 在指定位置插入代码
   {"type":"insertCode","payload":{"line":行号,"col":列号,"text":"要插入的代码"}}

3. deleteCode — 删除指定范围的代码
   {"type":"deleteCode","payload":{"startLine":行号,"startCol":列号,"endLine":行号,"endCol":列号}}

## 题库列表
%s

## 用户当前代码
%s

## RAG上下文
%s

## 进化知识库
%s

## 对话历史（最近%s条）
%s

## 输出格式（严格JSON）
{
  "reply": "你的回答",
  "action": {"type": "操作名", "payload": 参数},
  "suggestions": ["建议1", "建议2"]
}
""";

    // AI 对话：构造上下文、调用 LLM、解析返回
    public GlobalChatResponse chat(GlobalChatRequest request) {
        Map<String, String> config = loadConfig();

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            createConversation(conversationId, request.getUserId(), request.getMessage());
        }

        saveMessage(conversationId, "user", request.getMessage(), null);

        String questionList = buildQuestionList();
        String codeContext = buildCodeContext(request);
        String ragContext = buildRagContext(request.getMessage(), config);
        String evolutionContext = buildEvolutionContext(config);
        String history = buildHistory(conversationId, config);

        String systemPrompt = SYSTEM_PROMPT.formatted(
            questionList,
            codeContext,
            ragContext,
            evolutionContext,
            config.getOrDefault("max_history_length", "20"),
            history
        );

        try {
            String userContent = "用户说: " + request.getMessage();
            String apiKey = config.getOrDefault("api_key", "");
            String baseUrl = config.getOrDefault("base_url", "");
            String model = config.getOrDefault("model", "deepseek-chat");
            String result = llmClient.chatSimple(apiKey, baseUrl, model, systemPrompt, userContent);

            GlobalChatResponse response = parseResponse(result);
            response.setConversationId(conversationId);

            saveMessage(conversationId, "assistant", response.getReply(), null);
            updateConversationCount(conversationId);

            if (response.getReply() != null && !response.getReply().isBlank()) {
                evolutionService.extractInsightFromConversation(conversationId,
                    List.of(request.getMessage(), response.getReply()));
                evolutionService.indexKnowledgeCards(conversationId, request.getMessage(), response.getReply());
            }

            return response;

        } catch (Exception e) {
            log.error("AI对话失败", e);

            GlobalChatResponse fallback = new GlobalChatResponse();
            fallback.setConversationId(conversationId);
            fallback.setReply("抱歉，我暂时无法回答。请检查 API Key 是否已配置。");
            fallback.setSuggestions(List.of("配置 API Key", "换个方式描述问题"));
            return fallback;
        }
    }

    // 流式 SSE 聊天，逐步返回 AI 回复
    public SseEmitter chatStream(GlobalChatRequest request) {
        Map<String, String> config = loadConfig();
        SseEmitter emitter = new SseEmitter(120000L);

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            createConversation(conversationId, request.getUserId(), request.getMessage());
        }

        saveMessage(conversationId, "user", request.getMessage(), null);

        String questionList = buildQuestionList();
        String codeContext = buildCodeContext(request);
        String ragContext = buildRagContext(request.getMessage(), config);
        String evolutionContext = buildEvolutionContext(config);
        String history = buildHistory(conversationId, config);

        String systemPrompt = SYSTEM_PROMPT.formatted(
            questionList, codeContext, ragContext, evolutionContext,
            config.getOrDefault("max_history_length", "20"), history
        );

        String apiKey = config.getOrDefault("api_key", "");
        String baseUrl = config.getOrDefault("base_url", "");
        String model = config.getOrDefault("model", "deepseek-chat");

        StringBuilder fullContent = new StringBuilder();
        final String convId = conversationId;

        CompletableFuture.runAsync(() -> {
            try {
                String userContent = "用户说: " + request.getMessage();
                LlmRequest llmReq = new LlmRequest();
                llmReq.setModel(model);
                llmReq.setMessages(List.of(
                    new LlmRequest.Message("system", systemPrompt),
                    new LlmRequest.Message("user", userContent)
                ));
                llmReq.setTemperature(Double.parseDouble(config.getOrDefault("temperature", "0.7")));
                llmReq.setMaxTokens(Integer.parseInt(config.getOrDefault("max_tokens", "2048")));

                llmClient.chatStream(apiKey, baseUrl, model, llmReq,
                    chunk -> {
                        fullContent.append(chunk);
                        try {
                            emitter.send(SseEmitter.event()
                                .name("chunk")
                                .data(chunk));
                        } catch (Exception e) {
                            // client disconnected
                        }
                    },
                    () -> {
                        try {
                            String fullReply = fullContent.toString();
                            // Save the assistant message
                            saveMessage(convId, "assistant", fullReply, null);
                            updateConversationCount(convId);

                            // Try to parse action/suggestions from the full response
                            GlobalChatResponse parsed = parseResponse(fullReply);

                            // Send final metadata event
                            java.util.Map<String, Object> meta = new java.util.HashMap<>();
                            meta.put("conversationId", convId);
                            meta.put("suggestions", parsed.getSuggestions() != null ? parsed.getSuggestions() : new java.util.ArrayList<>());
                            meta.put("action", parsed.getAction());

                            emitter.send(SseEmitter.event()
                                .name("metadata")
                                .data(JSON.toJSONString(meta)));

                            emitter.complete();

                            if (fullReply != null && !fullReply.isBlank()) {
                                evolutionService.extractInsightFromConversation(convId,
                                    List.of(request.getMessage(), fullReply));
                                evolutionService.indexKnowledgeCards(convId, request.getMessage(), fullReply);
                            }
                        } catch (Exception e) {
                            log.warn("流式完成处理异常", e);
                            emitter.complete();
                        }
                    },
                    error -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("error")
                                .data(error.getMessage()));
                        } catch (Exception e2) {
                            // ignore
                        }
                        emitter.completeWithError(error);
                    }
                );
            } catch (Exception e) {
                log.error("流式AI对话失败", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("AI对话失败: " + e.getMessage()));
                } catch (Exception e2) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    // 提交对话反馈，用于自我进化
    public void submitFeedback(String conversationId, Long messageId, String userId, int rating, String comment) {
        evolutionService.recordFeedback(conversationId, messageId, userId, rating, comment);
    }

    // 加载助手配置（api_key, model, temperature 等）
    public Map<String, String> loadConfig() {
        Map<String, String> config = new HashMap<>();
        try {
            List<AssistantConfig> list = configMapper.selectList(null);
            for (AssistantConfig c : list) {
                config.put(c.getConfigKey(), c.getConfigValue());
            }
        } catch (Exception e) {
            log.warn("加载配置失败，使用默认值: {}", e.getMessage());
            config.put("temperature", "0.7");
            config.put("max_tokens", "2048");
            config.put("model", "deepseek-chat");
            config.put("base_url", "https://api.deepseek.com");
            config.put("system_prompt", "你是一个智能编程导师");
            config.put("rag_enabled", "true");
            config.put("history_enabled", "true");
            config.put("evolution_enabled", "true");
            config.put("max_history_length", "20");
        }
        if (!config.containsKey("temperature")) config.put("temperature", "0.7");
        if (!config.containsKey("max_tokens")) config.put("max_tokens", "2048");
        if (!config.containsKey("model")) config.put("model", "deepseek-chat");
        if (!config.containsKey("base_url")) config.put("base_url", "https://api.deepseek.com");
        return config;
    }

    // 更新助手配置
    public Map<String, String> updateConfig(Map<String, String> updates) {
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            List<AssistantConfig> existing = configMapper.selectList(
                new LambdaQueryWrapper<AssistantConfig>()
                    .eq(AssistantConfig::getConfigKey, key)
                    .last("LIMIT 1")
            );

            if (!existing.isEmpty()) {
                AssistantConfig config = existing.get(0);
                config.setConfigValue(value);
                config.setGmtModified(LocalDateTime.now());
                configMapper.updateById(config);
            } else {
                AssistantConfig config = new AssistantConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setConfigType("string");
                config.setGmtCreate(LocalDateTime.now());
                config.setGmtModified(LocalDateTime.now());
                configMapper.insert(config);
            }
        }
        return loadConfig();
    }

    // 获取用户的对话列表
    public List<AssistantConversation> listConversations(String userId) {
        return conversationMapper.selectList(
            new LambdaQueryWrapper<AssistantConversation>()
                .eq(AssistantConversation::getUserId, userId != null ? userId : "anonymous")
                .eq(AssistantConversation::getStatus, "ACTIVE")
                .orderByDesc(AssistantConversation::getGmtModified)
                .last("LIMIT 20")
        );
    }

    // 获取对话的消息列表
    public List<AssistantMessage> getMessages(String conversationId) {
        return messageMapper.selectList(
            new LambdaQueryWrapper<AssistantMessage>()
                .eq(AssistantMessage::getConversationId, conversationId)
                .orderByAsc(AssistantMessage::getGmtCreate)
        );
    }

    private void createConversation(String conversationId, String userId, String firstMessage) {
        AssistantConversation conv = new AssistantConversation();
        conv.setConversationId(conversationId);
        conv.setUserId(userId != null ? userId : "anonymous");
        conv.setTitle(firstMessage.length() > 50 ? firstMessage.substring(0, 50) + "..." : firstMessage);
        conv.setMessageCount(0);
        conv.setStatus("ACTIVE");
        conv.setGmtCreate(LocalDateTime.now());
        conv.setGmtModified(LocalDateTime.now());
        conversationMapper.insert(conv);
    }

    private void saveMessage(String conversationId, String role, String content, String metadata) {
        AssistantMessage msg = new AssistantMessage();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setMetadata(metadata);
        msg.setGmtCreate(LocalDateTime.now());
        messageMapper.insert(msg);
    }

    private void updateConversationCount(String conversationId) {
        try {
            AssistantConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<AssistantConversation>()
                    .eq(AssistantConversation::getConversationId, conversationId)
                    .last("LIMIT 1")
            );
            if (conv != null) {
                conv.setMessageCount((conv.getMessageCount() == null ? 0 : conv.getMessageCount()) + 1);
                conv.setGmtModified(LocalDateTime.now());
                conversationMapper.updateById(conv);
            }
        } catch (Exception e) {
            log.warn("更新对话计数失败: {}", e.getMessage());
        }
    }

    private String buildQuestionList() {
        try {
            List<com.cortex.lab.entity.QuestionBank> list = questionBankMapper.selectList(null);
            if (list.isEmpty()) return "（暂无题目）";
            return list.stream()
                .limit(30)
                .map(q -> "  ID=" + q.getId() + " " + q.getTitle())
                .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            return "（加载题目列表失败）";
        }
    }

    private String buildCodeContext(GlobalChatRequest request) {
        String code = request.getCurrentCode();
        String kp = request.getKnowledgePoint();
        if ((code == null || code.isBlank()) && (kp == null || kp.isBlank())) return "";
        StringBuilder sb = new StringBuilder();
        if (kp != null && !kp.isBlank()) sb.append("当前知识点：").append(kp).append("\n");
        if (code != null && !code.isBlank()) {
            sb.append("当前编辑器中的代码：\n```java\n").append(code).append("\n```");
        }
        return sb.toString();
    }

    private String buildRagContext(String message, Map<String, String> config) {
        if (!"true".equals(config.getOrDefault("rag_enabled", "true"))) {
            return "（RAG已禁用）";
        }

        try {
            List<String> contexts = ragService.searchRelevantContext(message, 5);
            if (contexts.isEmpty()) {
                return "（未检索到相关知识）";
            }
            return String.join("\n---\n", contexts);
        } catch (Exception e) {
            log.warn("RAG检索失败: {}", e.getMessage());
            return "（RAG检索失败）";
        }
    }

    private String buildEvolutionContext(Map<String, String> config) {
        if (!"true".equals(config.getOrDefault("evolution_enabled", "true"))) {
            return "（自我进化已禁用）";
        }
        return evolutionService.buildEvolutionContext();
    }

    private String buildHistory(String conversationId, Map<String, String> config) {
        if (!"true".equals(config.getOrDefault("history_enabled", "true"))) {
            return "（历史已禁用）";
        }

        try {
            int maxHistory = Integer.parseInt(config.getOrDefault("max_history_length", "20"));

            List<AssistantMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AssistantMessage>()
                    .eq(AssistantMessage::getConversationId, conversationId)
                    .orderByDesc(AssistantMessage::getGmtCreate)
                    .last("LIMIT " + maxHistory)
            );

            Collections.reverse(messages);

            if (messages.isEmpty()) return "（无历史对话）";

            StringBuilder sb = new StringBuilder();
            for (AssistantMessage msg : messages) {
                String role = "user".equals(msg.getRole()) ? "用户" : "AI";
                String content = msg.getContent();
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                sb.append(role).append(": ").append(content).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("构建历史失败: {}", e.getMessage());
            return "（加载历史失败）";
        }
    }

    private GlobalChatResponse parseResponse(String result) {
        GlobalChatResponse response = new GlobalChatResponse();
        response.setSuggestions(new ArrayList<>());

        if (result == null || result.isBlank()) {
            response.setReply("我还在思考中，请稍等...");
            response.setIncomplete(true);
            response.setAskReason("需要更多时间来思考你的问题");
            response.setSuggestions(List.of("重新描述问题", "换个角度提问"));
            return response;
        }

        try {
            String jsonStr = result.trim();
            if (jsonStr.startsWith("```")) {
                int start = jsonStr.indexOf('\n');
                int end = jsonStr.lastIndexOf("```");
                if (start > 0 && end > start) {
                    jsonStr = jsonStr.substring(start, end).trim();
                }
            }

            com.alibaba.fastjson2.JSONObject json = JSON.parseObject(jsonStr);
            response.setReply(json.getString("reply"));
            response.setIncomplete(json.getBooleanValue("incomplete"));
            response.setAskReason(json.getString("askReason"));

            if (json.containsKey("action") && json.get("action") instanceof Map) {
                response.setAction(json.getJSONObject("action"));
            }

            if (json.containsKey("suggestions")) {
                List<String> suggestions = json.getList("suggestions", String.class);
                response.setSuggestions(suggestions != null ? suggestions : new ArrayList<>());
            }
        } catch (Exception e) {
            log.warn("JSON解析失败，使用纯文本回复: {}", e.getMessage());
            response.setReply(result);
            response.setIncomplete(false);
        }

        if (response.getReply() == null || response.getReply().isBlank()) {
            response.setReply(result != null ? result : "（AI未能生成有效回复）");
        }

        return response;
    }

    // 软删除对话
    public void deleteConversation(String conversationId) {
        try {
            AssistantConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<AssistantConversation>()
                    .eq(AssistantConversation::getConversationId, conversationId)
                    .last("LIMIT 1")
            );
            if (conv != null) {
                conv.setStatus("DELETED");
                conv.setGmtModified(LocalDateTime.now());
                conversationMapper.updateById(conv);
            }
        } catch (Exception e) {
            log.warn("删除对话失败: {}", e.getMessage());
        }
    }
}

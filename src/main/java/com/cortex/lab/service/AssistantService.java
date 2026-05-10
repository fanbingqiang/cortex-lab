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
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AssistantConfigMapper configMapper;
    private final AssistantConversationMapper conversationMapper;
    private final AssistantMessageMapper messageMapper;
    private final LlmClient llmClient;
    private final RagService ragService;
    private final EvolutionService evolutionService;
    private final QuestionBankMapper questionBankMapper;

    private static final String SYSTEM_PROMPT = """
你是一个智能编程导师"小C"，运行在代码练习平台 Cortex Lab 中。
你可以回答编程问题，也可以执行平台操作。需要操作时在 action 中指定。

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
            String result = llmClient.chatSimple(systemPrompt, userContent);

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

    public void submitFeedback(String conversationId, Long messageId, String userId, int rating, String comment) {
        evolutionService.recordFeedback(conversationId, messageId, userId, rating, comment);
    }

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
            config.put("system_prompt", "你是一个智能编程导师");
            config.put("rag_enabled", "true");
            config.put("history_enabled", "true");
            config.put("evolution_enabled", "true");
            config.put("max_history_length", "20");
        }
        if (!config.containsKey("temperature")) config.put("temperature", "0.7");
        if (!config.containsKey("max_tokens")) config.put("max_tokens", "2048");
        if (!config.containsKey("model")) config.put("model", "deepseek-chat");
        return config;
    }

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

    public List<AssistantConversation> listConversations(String userId) {
        return conversationMapper.selectList(
            new LambdaQueryWrapper<AssistantConversation>()
                .eq(AssistantConversation::getUserId, userId != null ? userId : "anonymous")
                .eq(AssistantConversation::getStatus, "ACTIVE")
                .orderByDesc(AssistantConversation::getGmtModified)
                .last("LIMIT 20")
        );
    }

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

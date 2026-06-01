package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cortex.lab.dto.GlobalChatRequest;
import com.cortex.lab.dto.GlobalChatResponse;
import com.cortex.lab.entity.*;
import com.cortex.lab.mapper.*;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
import com.cortex.lab.entity.UserLearningProfile;
import com.cortex.lab.mapper.UserLearningProfileMapper;
import com.cortex.entity.MistakeRecord;
import com.cortex.mapper.MistakeRecordMapper;
import com.cortex.lab.service.KnowledgeTreeService;
import com.cortex.lab.service.SandboxService;
import com.cortex.lab.dto.ExecuteResponse;
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
    private final UserLearningProfileMapper userLearningProfileMapper;
    private final MistakeRecordMapper mistakeRecordMapper;
    private final KnowledgeTreeService knowledgeTreeService;
    private final SandboxService sandboxService;

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
- {"type":"executeCode","payload":"完整的Java代码（含public class和main方法）"} — 编译并执行Java代码，返回真实输出。当你需要确认代码输出、验证想法时用这个

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

## 用户当前状态
%s

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
  "suggestions": ["建议1", "建议2", "建议3"]
}

重要：suggestions必须根据用户当前状态生成3个具体的、可点击的建议，不要用"换个问题"这种通用建议。
    例如用户HashMap相关错误多，应该建议"生成HashMap陷阱题"、"看看ConcurrentHashMap区别"。
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

        String userContext = buildUserContext(request.getUserId());
        String questionList = buildQuestionList();
        String codeContext = buildCodeContext(request);
        String ragContext = buildRagContext(request.getMessage(), config);
        String evolutionContext = buildEvolutionContext(config);
        String history = buildHistory(conversationId, config);

        String systemPrompt = SYSTEM_PROMPT.formatted(
            userContext,
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

        String userContext = buildUserContext(request.getUserId());
        String questionList = buildQuestionList();
        String codeContext = buildCodeContext(request);
        String ragContext = buildRagContext(request.getMessage(), config);
        String evolutionContext = buildEvolutionContext(config);
        String history = buildHistory(conversationId, config);

        String systemPrompt = SYSTEM_PROMPT.formatted(
            userContext, questionList, codeContext, ragContext, evolutionContext,
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

                            // Handle executeCode action: actually compile and run
                            if (parsed.getAction() != null && "executeCode".equals(parsed.getAction().get("type"))) {
                                String code = (String) parsed.getAction().get("payload");
                                if (code != null && !code.isBlank()) {
                                    String execResult = executeCodeAction(code);
                                    String updatedReply = parsed.getReply() + "\n\n" + execResult;
                                    parsed.setReply(updatedReply);
                                    try {
                                        emitter.send(SseEmitter.event()
                                            .name("chunk")
                                            .data("\n\n" + execResult));
                                    } catch (Exception e) {
                                        log.warn("发送执行结果失败", e);
                                    }
                                }
                            }

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
            sb.append("当前编辑器中的代码：\n```java\n").append(code).append("\n```\n");
        }
        // 原始代码对比（用于分析用户改了什么）
        String original = request.getOriginalCode();
        if (original != null && !original.isBlank() && !original.equals(code)) {
            sb.append("原始代码（用户可能修改了）：\n```java\n").append(original).append("\n```\n");
            // 简单标注差异
            if (original.length() == code.length()) {
                for (int i = 0; i < Math.min(original.length(), code.length()); i++) {
                    if (original.charAt(i) != code.charAt(i)) {
                        sb.append("注意：代码在第").append(i + 1).append("个字符处与原始代码不同\n");
                        break;
                    }
                }
            }
        }
        // 执行输出
        String execOut = request.getExecutionOutput();
        if (execOut != null && !execOut.isBlank()) {
            sb.append("用户上次运行的输出：\n```\n").append(execOut).append("\n```\n");
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

    // 构建用户当前状态上下文，用于生成动态建议
    private String buildUserContext(String userId) {
        if (userId == null || userId.isBlank() || "anonymous".equals(userId)) {
            return "（用户未登录）";
        }
        StringBuilder sb = new StringBuilder();

        // 学习统计
        try {
            UserLearningProfile profile = userLearningProfileMapper.selectOne(
                new LambdaQueryWrapper<UserLearningProfile>()
                    .eq(UserLearningProfile::getUserId, userId)
                    .last("LIMIT 1")
            );
            if (profile != null) {
                int total = profile.getTotalQuestionsAnswered() != null ? profile.getTotalQuestionsAnswered() : 0;
                int correct = profile.getTotalCorrect() != null ? profile.getTotalCorrect() : 0;
                int streak = profile.getStudyStreak() != null ? profile.getStudyStreak() : 0;
                double acc = total > 0 ? (correct * 100.0 / total) : 0;
                sb.append("- 学习统计：共").append(total).append("题，正确率").append(String.format("%.0f", acc)).append("%\n");
                sb.append("- 连续学习：").append(streak).append("天\n");
                if (profile.getWeakAreas() != null && !profile.getWeakAreas().isBlank()) {
                    sb.append("- 薄弱领域：").append(profile.getWeakAreas()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("读取学习画像失败: {}", e.getMessage());
        }

        // 近期错误记录
        try {
            List<MistakeRecord> mistakes = mistakeRecordMapper.selectList(
                new LambdaQueryWrapper<MistakeRecord>()
                    .eq(MistakeRecord::getUserId, userId)
                    .orderByDesc(MistakeRecord::getGmtCreate)
                    .last("LIMIT 5")
            );
            if (!mistakes.isEmpty()) {
                sb.append("- 近期错误：\n");
                for (MistakeRecord m : mistakes) {
                    sb.append("  * ").append(m.getKeyword()).append("：").append(m.getDescription()).append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("读取错误记录失败: {}", e.getMessage());
        }

        // 已掌握的知识节点
        try {
            List<String> mastered = knowledgeTreeService.getMasteredNodeIds(userId);
            if (!mastered.isEmpty()) {
                sb.append("- 已掌握：").append(String.join("、", mastered)).append("\n");
            }
        } catch (Exception e) {
            log.warn("读取掌握节点失败: {}", e.getMessage());
        }

        return sb.isEmpty() ? "（暂无学习数据）" : sb.toString();
    }

    // 执行 executeCode 动作：编译运行 Java 代码
    private String executeCodeAction(String code) {
        try {
            ExecuteResponse result = sandboxService.execute(code);
            StringBuilder sb = new StringBuilder();
            sb.append("【代码执行结果】\n");
            if (result.isSuccess()) {
                sb.append("退出码: ").append(result.getExitCode()).append("\n");
                if (result.getStdout() != null && !result.getStdout().isBlank()) {
                    sb.append("输出:\n").append(result.getStdout());
                }
            } else {
                sb.append("执行失败:\n");
                if (result.getStdout() != null && !result.getStdout().isBlank()) {
                    sb.append(result.getStdout()).append("\n");
                }
                if (result.getStderr() != null && !result.getStderr().isBlank()) {
                    sb.append(result.getStderr()).append("\n");
                }
                if (result.getError() != null) {
                    sb.append(result.getError());
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return "代码执行异常: " + e.getMessage();
        }
    }
}

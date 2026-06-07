package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cortex.lab.dto.GlobalChatRequest;
import com.cortex.lab.dto.GlobalChatResponse;
import com.cortex.lab.dto.CardDto;
import com.cortex.lab.dto.QuestionDto;
import com.cortex.lab.dto.ScenarioDto;
import com.cortex.lab.entity.*;
import com.cortex.lab.mapper.*;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
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
    private final UserMapper userLearningProfileMapper;
    private final MistakeRecordMapper mistakeRecordMapper;
    private final KnowledgeTreeService knowledgeTreeService;
    private final SandboxService sandboxService;
    private final QuestionBankService questionBankService;
    private final KnowledgeCardService knowledgeCardService;

    private static final String SYSTEM_PROMPT = """
你是一个智能编程导师"小C"，运行在代码练习平台 Cortex Lab 中。
你可以回答编程问题，也可以执行平台操作。用户可以要求你修改编辑器中的代码。
需要操作时在 action 中指定；不需要时 action 设为 null。

## 回答原则
- 简洁有针对性，不要啰嗦
- 需要操作时在 action 字段指定；不需要时 action 设为 null
- 用户想打开题目、运行代码等，直接执行不要问"要不要"

## 可用操作
- {"type":"switchTab","payload":"practice|questions|cards|community|progress"} — 切换标签
- {"type":"loadToEditor","payload":"代码文本"} — 将代码文本加载到编辑器
- {"type":"loadQuestion","payload":题目ID} — 从题库加载题目代码到编辑器
- {"type":"runCode"} — 运行编辑器中的代码
- {"type":"resetCode"} — 重置编辑器代码
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

3. deleteCode — 移除指定范围的代码
   {"type":"deleteCode","payload":{"startLine":行号,"startCol":列号,"endLine":行号,"endCol":列号}}

## 题库操作
- {"type":"searchQuestions","payload":"关键词"} — 搜索题目，返回标题和ID
- {"type":"getQuestion","payload":题目ID} — 查看题目详情（含代码）
- {"type":"generateQuestion","payload":"知识点"} — AI生成一道新陷阱题
- {"type":"createQuestion","payload":{"title":"标题","trapCode":"代码","expectedPitfall":"坑点","correctExplanation":"原理解释"}} — 手动创建题目
- {"type":"deleteQuestion","payload":题目ID} — 删除题目
- {"type":"toggleQuestionMastered","payload":{"questionId":ID,"mastered":true}} — 标记掌握/未掌握
- {"type":"getReviewList"} — 查看待复习题目
- {"type":"submitReview","payload":{"questionId":ID,"stillMastered":true}} — 提交复习结果

## 知识树操作
- {"type":"getKnowledgeTree"} — 获取知识树结构
- {"type":"generateScenario","payload":"节点ID"} — 从知识点生成陷阱题场景（会加载到编辑器）
- {"type":"toggleNodeMastered","payload":{"nodeId":"节点ID","mastered":true}} — 切换节点掌握状态

## 知识卡片操作
- {"type":"listCards"} — 列出所有知识卡片
- {"type":"generateCard","payload":题目ID} — 为题目生成知识卡片
- {"type":"deleteCard","payload":卡片ID} — 删除知识卡片

## 输出格式（严格JSON）
{
  "reply": "你的回答（执行操作时不超过10字或空字符串）",
  "action": {"type": "操作名", "payload": 参数},
  "silent": true,
  "suggestions": ["建议1", "建议2", "建议3"]
}

重要：
- 当返回 action 时，如果不需要回复用户，设 silent=true，reply 可为空字符串。前端会静默执行action，不显示在聊天框。
- 当执行 switchTab、loadQuestion 等导航操作时，尽量 silent=true，避免多余文字。
- 根据用户当前状态主动执行有益操作，例如：
  * 用户打开页面时 → 主动告知待复习题数（suggestions）
  * 用户代码运行出错 → 主动分析错误并修正代码
  * 用户查看题目时 → 主动告知是否已掌握
- suggestions必须根据用户当前状态生成3个具体的、可点击的建议，不要用"换个问题"这种通用建议。
    例如用户HashMap相关错误多，应该建议"生成HashMap陷阱题"、"看看ConcurrentHashMap区别"。
""";

    // 主动欢迎：根据用户学习状态生成问候和建议
    public GlobalChatResponse welcome(String userId) {
        GlobalChatResponse resp = new GlobalChatResponse();
        resp.setSuggestions(new ArrayList<>());

        if (userId == null || userId.isBlank() || "anonymous".equals(userId)) {
            resp.setReply("你好！我是小C助手，可以帮你学习编程。有什么想了解的？");
            resp.setSuggestions(List.of("Java 基础有哪些知识点", "帮我生成一道练习题", "怎么学习集合框架"));
            return resp;
        }

        StringBuilder reply = new StringBuilder();
        List<String> suggestions = new ArrayList<>();

        // 加载学习数据
        try {
            User profile = userLearningProfileMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUserId, userId).last("LIMIT 1"));

            if (profile != null && profile.getTotalQuestionsAnswered() != null && profile.getTotalQuestionsAnswered() > 0) {
                int total = profile.getTotalQuestionsAnswered();
                int correct = profile.getTotalCorrect() != null ? profile.getTotalCorrect() : 0;
                int streak = profile.getStudyStreak() != null ? profile.getStudyStreak() : 0;
                double acc = total > 0 ? (correct * 100.0 / total) : 0;

                reply.append("欢迎回来！").append("\n");
                reply.append("你目前共做了 ").append(total).append(" 道题，正确率 ").append(String.format("%.0f", acc)).append("%，已连续学习 ").append(streak).append(" 天。\n");

                // 根据正确率给建议
                if (acc < 50) {
                    reply.append("正确率还有提升空间，建议从基础开始巩固。");
                    suggestions.add("Java 基础语法练习");
                    suggestions.add("常见陷阱题");
                } else if (acc < 80) {
                    reply.append("基础不错，继续加强薄弱环节。");
                } else {
                    reply.append("掌握得很好！可以挑战更难的内容。");
                    suggestions.add("进阶难题");
                    suggestions.add("源码分析");
                }

                // 薄弱领域提示
                if (profile.getWeakAreas() != null && !profile.getWeakAreas().isBlank()) {
                    reply.append("\n你的薄弱领域：").append(profile.getWeakAreas()).append("。");
                    String[] areas = profile.getWeakAreas().split("[,\\s]+");
                    for (String area : areas) {
                        if (!area.isBlank()) {
                            suggestions.add("练习 " + area.trim() + " 题目");
                        }
                    }
                }
            } else {
                reply.append("欢迎来到 Cortex Lab！开始你的编程学习之旅吧。");
                suggestions.add("Java 基础入门");
                suggestions.add("生成第一道练习题");
            }

            // 待复习题目提醒
            try {
                List<QuestionDto> dueQuestions = questionBankService.getReviewList(userId);
                if (dueQuestions != null && !dueQuestions.isEmpty()) {
                    reply.append("\n你有 ").append(dueQuestions.size()).append(" 道题待复习。");
                    suggestions.add("开始复习");
                }
            } catch (Exception e) {
                // ignore
            }

        } catch (Exception e) {
            log.warn("加载学习数据失败: {}", e.getMessage());
            reply.append("你好！我是小C助手。");
        }

        // 去重并限制数量
        suggestions = suggestions.stream().distinct().limit(5).collect(Collectors.toList());
        if (suggestions.isEmpty()) {
            suggestions.addAll(List.of("Java 基础", "生成练习题", "集合框架"));
        }

        resp.setReply(reply.toString().trim());
        resp.setSuggestions(suggestions);
        return resp;
    }

    // 构建多条上下文消息（每条一个独立 user 消息，让 AI 自己理解）
    private List<LlmRequest.Message> buildContextMessages(String userId, GlobalChatRequest request, Map<String, String> config, String conversationId) {
        List<LlmRequest.Message> msgs = new ArrayList<>();
        String profile = buildUserContext(userId);
        if (profile != null && !profile.isBlank() && !profile.equals("（用户未登录）") && !profile.equals("（暂无学习数据）")) {
            msgs.add(new LlmRequest.Message("user", "【用户画像】\n" + profile));
        }
        String questions = buildQuestionList();
        if (questions != null && !questions.isBlank() && !questions.equals("（暂无题目）")) {
            msgs.add(new LlmRequest.Message("user", "【题库列表】\n" + questions));
        }
        String code = buildCodeContext(request);
        if (code != null && !code.isBlank()) {
            msgs.add(new LlmRequest.Message("user", "【当前代码】\n" + code));
        }
        String rag = buildRagContext(request.getMessage(), config);
        if (rag != null && !rag.isBlank() && !rag.contains("未检索到") && !rag.contains("已禁用")) {
            msgs.add(new LlmRequest.Message("user", "【相关知识】\n" + rag));
        }
        String evolution = buildEvolutionContext(config);
        if (evolution != null && !evolution.isBlank() && !evolution.contains("已禁用")) {
            msgs.add(new LlmRequest.Message("user", "【进化知识库】\n" + evolution));
        }
        String history = buildHistory(conversationId, config);
        if (history != null && !history.isBlank() && !history.contains("无历史")) {
            msgs.add(new LlmRequest.Message("user", "【对话历史】\n" + history));
        }
        return msgs;
    }

    // AI 对话：构造上下文、调用 LLM、解析返回
    public GlobalChatResponse chat(GlobalChatRequest request) {
        Map<String, String> config = loadConfig();

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
            createConversation(conversationId, request.getUserId(), request.getMessage());
        }

        saveMessage(conversationId, "user", request.getMessage(), null);

        String apiKey = config.getOrDefault("api_key", "");
        String baseUrl = config.getOrDefault("base_url", "");
        String model = config.getOrDefault("model", "deepseek-chat");

        try {
            List<LlmRequest.Message> messages = new ArrayList<>();
            messages.add(new LlmRequest.Message("system", SYSTEM_PROMPT));
            messages.addAll(buildContextMessages(request.getUserId(), request, config, conversationId));
            messages.add(new LlmRequest.Message("user", request.getMessage()));

            LlmRequest llmReq = new LlmRequest();
            llmReq.setModel(model);
            llmReq.setMessages(messages);
            llmReq.setTemperature(Double.parseDouble(config.getOrDefault("temperature", "0.7")));
            llmReq.setMaxTokens(Integer.parseInt(config.getOrDefault("max_tokens", "2048")));

            String result = llmClient.chat(baseUrl, apiKey, llmReq).getContent();

            GlobalChatResponse response = parseResponse(result);
            response.setConversationId(conversationId);

            // Execute server-side actions and append results
            String actionResult = executeServerAction(response.getAction(), request.getUserId());
            if (actionResult != null) {
                response.setReply(response.getReply() + "\n\n" + actionResult);
            }

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

        String apiKey = config.getOrDefault("api_key", "");
        String baseUrl = config.getOrDefault("base_url", "");
        String model = config.getOrDefault("model", "deepseek-chat");
        List<LlmRequest.Message> ctxMessages = buildContextMessages(request.getUserId(), request, config, conversationId);
        String userMsg = request.getMessage();

        StringBuilder fullContent = new StringBuilder();
        final String convId = conversationId;

        CompletableFuture.runAsync(() -> {
            try {
                List<LlmRequest.Message> messages = new ArrayList<>();
                messages.add(new LlmRequest.Message("system", SYSTEM_PROMPT));
                messages.addAll(ctxMessages);
                messages.add(new LlmRequest.Message("user", userMsg));

                LlmRequest llmReq = new LlmRequest();
                llmReq.setModel(model);
                llmReq.setMessages(messages);
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
                    thinking -> {
                        try {
                            emitter.send(SseEmitter.event()
                                .name("thinking")
                                .data(thinking));
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

                            // If silent=true and has action, remove assistant message from chat display
                            if (parsed.isSilent() && parsed.getAction() != null) {
                                // Don't show this message in chat, but still process action below
                                // The reply will be replaced with empty since it's not visible
                            }

                            // Send final metadata event
                            java.util.Map<String, Object> meta = new java.util.HashMap<>();
                            meta.put("conversationId", convId);
                            meta.put("suggestions", parsed.getSuggestions() != null ? parsed.getSuggestions() : new java.util.ArrayList<>());
                            meta.put("action", parsed.getAction());
                            meta.put("silent", parsed.isSilent());

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

                            // Handle other server-side actions
                            if (parsed.getAction() != null) {
                                String actionType = (String) parsed.getAction().get("type");
                                if (!"executeCode".equals(actionType)) {
                                    String actionResult = executeServerAction(parsed.getAction(), request.getUserId());
                                    if (actionResult != null) {
                                        String updatedReply = parsed.getReply() + "\n\n" + actionResult;
                                        parsed.setReply(updatedReply);
                                        try {
                                            emitter.send(SseEmitter.event()
                                                .name("chunk")
                                                .data("\n\n" + actionResult));
                                        } catch (Exception e) {
                                            log.warn("发送操作结果失败", e);
                                        }
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
            response.setSilent(json.getBooleanValue("silent"));
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
            User profile = userLearningProfileMapper.selectOne(
                new LambdaQueryWrapper<User>()
                    .eq(User::getUserId, userId)
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

    // ========== P0: AI action dispatch ==========

    /** Dispatch and execute server-side actions from AI response */
    private String executeServerAction(Map<String, Object> action, String userId) {
        if (action == null) return null;
        String type = (String) action.get("type");
        if (type == null) return null;
        Object payload = action.get("payload");

        try {
            return switch (type) {
                // ---- Question bank ----
                case "searchQuestions" -> execSearchQuestions(payload != null ? payload.toString() : "", userId);
                case "getQuestion" -> execGetQuestion(toLong(payload), userId);
                case "generateQuestion" -> execGenerateQuestion(payload != null ? payload.toString() : "");
                case "createQuestion" -> execCreateQuestion(mapPayload(payload));
                case "deleteQuestion" -> execDeleteQuestion(toLong(payload));
                case "toggleQuestionMastered" -> execToggleQuestionMastered(mapPayload(payload), userId);
                case "getReviewList" -> execGetReviewList(userId);
                case "submitReview" -> execSubmitReview(mapPayload(payload), userId);
                // ---- Knowledge tree ----
                case "getKnowledgeTree" -> execGetKnowledgeTree();
                case "generateScenario" -> execGenerateScenario(payload != null ? payload.toString() : "");
                case "toggleNodeMastered" -> execToggleNodeMastered(mapPayload(payload), userId);
                // ---- Knowledge cards ----
                case "listCards" -> execListCards();
                case "generateCard" -> execGenerateCard(toLong(payload));
                case "deleteCard" -> execDeleteCard(toLong(payload));
                default -> null;
            };
        } catch (Exception e) {
            log.warn("执行AI操作[{}]失败: {}", type, e.getMessage());
            return "操作失败: " + e.getMessage();
        }
    }

    // ---- Question Bank Handlers ----

    private String execSearchQuestions(String keyword, String userId) {
        List<QuestionDto> results = questionBankService.searchQuestions(keyword);
        if (results.isEmpty()) return "未找到相关题目。";
        StringBuilder sb = new StringBuilder("**搜索结果**（共" + results.size() + "条）：\n");
        for (int i = 0; i < Math.min(results.size(), 15); i++) {
            QuestionDto q = results.get(i);
            sb.append((i + 1) + ". ID:" + q.getId() + " **" + q.getTitle() + "**");
            if (q.getDifficulty() != null && q.getDifficulty() > 0) sb.append(" " + "★".repeat(q.getDifficulty()));
            if (q.getCategory() != null) sb.append(" [" + q.getCategory() + "]");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String execGetQuestion(Long id, String userId) {
        if (id == null) return "题目ID不能为空。";
        QuestionDto q = questionBankService.getById(id, userId);
        if (q == null) return "题目不存在（ID=" + id + "）。";
        StringBuilder sb = new StringBuilder();
        sb.append("**题目：" + q.getTitle() + "**\n");
        if (q.getDescription() != null) sb.append("> " + q.getDescription() + "\n\n");
        if (q.getTrapCode() != null) {
            sb.append("```java\n" + q.getTrapCode() + "\n```\n");
        }
        if (q.getExpectedPitfall() != null) sb.append("**陷阱：**" + q.getExpectedPitfall() + "\n");
        if (q.getCorrectExplanation() != null) sb.append("**解释：**" + q.getCorrectExplanation() + "\n");
        if (q.getHints() != null && !"[]".equals(q.getHints())) sb.append("**提示：**" + q.getHints() + "\n");
        return sb.toString();
    }

    private String execGenerateQuestion(String topic) {
        if (topic == null || topic.isBlank()) return "请提供知识点或主题。";
        QuestionDto q = questionBankService.generateFromQuestion(topic);
        return "**已生成题目：【" + q.getTitle() + "】**\n" + q.getDescription() + "\n```java\n" + q.getTrapCode() + "\n```\n告诉我是否要载入编辑器练习。";
    }

    @SuppressWarnings("unchecked")
    private String execCreateQuestion(Map<String, Object> payload) {
        if (payload == null) return "缺少题目数据。";
        String title = (String) payload.get("title");
        String trapCode = (String) payload.get("trapCode");
        String pitfall = (String) payload.get("expectedPitfall");
        String explanation = (String) payload.get("correctExplanation");
        if (title == null || title.isBlank()) return "题目标题不能为空。";
        QuestionDto q = questionBankService.importQuestion(title, null, trapCode, pitfall, explanation);
        return "**题目已创建**（ID=" + q.getId() + "）：" + q.getTitle();
    }

    private String execDeleteQuestion(Long id) {
        if (id == null) return "题目ID不能为空。";
        questionBankService.deleteQuestion(id);
        return "已删除题目（ID=" + id + "）。";
    }

    @SuppressWarnings("unchecked")
    private String execToggleQuestionMastered(Map<String, Object> payload, String userId) {
        if (payload == null || userId == null) return "缺少参数。";
        Long questionId = toLong(payload.get("questionId"));
        boolean mastered = Boolean.TRUE.equals(payload.get("mastered"));
        if (questionId == null) return "questionId不能为空。";
        questionBankService.setMastered(questionId, userId, mastered);
        return mastered ? "已标记为掌握。" : "已取消掌握标记。";
    }

    private String execGetReviewList(String userId) {
        if (userId == null) return "请先登录。";
        List<QuestionDto> list = questionBankService.getReviewList(userId);
        if (list.isEmpty()) return "暂无待复习题目。";
        StringBuilder sb = new StringBuilder("**待复习题目**（共" + list.size() + "道）：\n");
        for (QuestionDto q : list) {
            sb.append("- ID:" + q.getId() + " " + q.getTitle());
            if (q.getNextReviewTime() != null) sb.append("（逾期）");
            sb.append("\n");
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private String execSubmitReview(Map<String, Object> payload, String userId) {
        if (payload == null || userId == null) return "缺少参数。";
        Long questionId = toLong(payload.get("questionId"));
        boolean stillMastered = Boolean.TRUE.equals(payload.get("stillMastered"));
        if (questionId == null) return "questionId不能为空。";
        questionBankService.updateReview(questionId, userId, stillMastered);
        return stillMastered ? "复习完成，已安排下次复习时间。" : "已取消掌握，将加入复习队列。";
    }

    // ---- Knowledge Tree Handlers ----

    private String execGetKnowledgeTree() {
        var tree = knowledgeTreeService.getTree();
        StringBuilder sb = new StringBuilder("**Java后端知识树**\n");
        for (var category : tree) {
            sb.append("- ").append(category.getName()).append("\n");
            if (category.getChildren() != null) {
                for (var child : category.getChildren()) {
                    sb.append("  - ").append(child.getName());
                    if (child.isLeaf()) sb.append("（可生成题目）");
                    sb.append("  `").append(child.getId()).append("`\n");
                }
            }
        }
        return sb.toString();
    }

    private String execGenerateScenario(String nodeId) {
        if (nodeId == null || nodeId.isBlank()) return "节点ID不能为空。";
        ScenarioDto scenario = knowledgeTreeService.generateForNode(nodeId);
        return "**已生成场景：" + scenario.getKnowledgePoint() + "**\n```java\n" + scenario.getTrapCode() + "\n```\n告诉我是否要载入编辑器练习。";
    }

    @SuppressWarnings("unchecked")
    private String execToggleNodeMastered(Map<String, Object> payload, String userId) {
        if (payload == null || userId == null) return "缺少参数。";
        String nodeId = (String) payload.get("nodeId");
        boolean mastered = Boolean.TRUE.equals(payload.get("mastered"));
        if (nodeId == null) return "nodeId不能为空。";
        knowledgeTreeService.toggleMastered(nodeId, userId, mastered);
        return mastered ? "已标记该节点为掌握。" : "已取消掌握标记。";
    }

    // ---- Knowledge Card Handlers ----

    private String execListCards() {
        List<CardDto> cards = knowledgeCardService.listAll();
        if (cards.isEmpty()) return "暂无知识卡片。";
        StringBuilder sb = new StringBuilder("**知识卡片列表**（共" + cards.size() + "张）：\n");
        for (int i = 0; i < Math.min(cards.size(), 20); i++) {
            CardDto c = cards.get(i);
            sb.append((i + 1) + ". " + (c.getTitle() != null ? c.getTitle() : "未命名"));
            if (c.getQuestionTitle() != null) sb.append("（题目：" + c.getQuestionTitle() + "）");
            sb.append("\n");
        }
        return sb.toString();
    }

    private String execGenerateCard(Long questionId) {
        if (questionId == null) return "题目ID不能为空。";
        CardDto card = knowledgeCardService.generateCard(questionId);
        StringBuilder sb = new StringBuilder();
        sb.append("**已生成知识卡片：【" + (card.getTitle() != null ? card.getTitle() : "未命名") + "】**\n");
        if (card.getKeyPoints() != null) sb.append("**要点：**" + card.getKeyPoints() + "\n");
        if (card.getDetailExplanation() != null) sb.append("**详解：**" + card.getDetailExplanation() + "\n");
        if (card.getCodeSnippet() != null) sb.append("```java\n" + card.getCodeSnippet() + "\n```\n");
        if (card.getCommonPitfalls() != null) sb.append("**常见误区：**" + card.getCommonPitfalls() + "\n");
        return sb.toString();
    }

    private String execDeleteCard(Long id) {
        if (id == null) return "卡片ID不能为空。";
        knowledgeCardService.deleteCard(id);
        return "已删除卡片（ID=" + id + "）。";
    }

    // ---- Helpers ----

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapPayload(Object payload) {
        if (payload instanceof Map) return (Map<String, Object>) payload;
        return null;
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (NumberFormatException e) { return null; }
    }
}

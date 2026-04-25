package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.lab.dto.ChatRequest;
import com.cortex.lab.dto.ChatResponse;
import com.cortex.lab.dto.ScenarioDto;
import com.cortex.lab.entity.LabSession;
import com.cortex.lab.mapper.LabSessionMapper;
import com.cortex.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DialogueService {

    private final LlmClient llmClient;
    private final LabSessionMapper sessionMapper;

    private static final String SOCRATIC_SYSTEM_PROMPT = """
你是一个苏格拉底式编程导师"小 C"。引导学习者发现代码陷阱，**回复控制在 1~3 句话**，简短有力。

## 原则
1. **绝不直接说答案**，用反问或类比
2. **循序渐进**：第1轮"你发现了什么？" → 第2轮"试试改个值？" → 第3轮"回忆一下XX知识"
3. **用类比引导**，保持轻松语气

## 场景
- 知识点：%s
- 预期陷阱：%s
- 正确解释（你不能直接说）：%s

## 对话历史
%s

## 上次输出
%s

返回 JSON：
{
  "reply": "1~3句话",
  "hintLevel": "OBSERVE|HINT|STRONG_HINT|NEAR_ANSWER",
  "enlightened": false,
  "suggestions": ["建议1", "建议2"]
}

学习者准确说出原理才设 enlightened=true。
""";

    public LabSession startSession(Long scenarioId, String userId, ScenarioDto scenario) {
        LabSession session = new LabSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setScenarioId(scenarioId);
        session.setUserId(userId);
        session.setStatus("IN_PROGRESS");
        session.setAttempts(0);
        session.setChatHistory("[]");
        session.setGmtCreate(LocalDateTime.now());
        session.setGmtModified(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    public ChatResponse chat(ChatRequest request, ScenarioDto scenario) {
        LabSession session = sessionMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabSession>()
                .eq(LabSession::getSessionId, request.getSessionId())
        );

        if (session == null) {
            ChatResponse resp = new ChatResponse();
            resp.setReply("会话不存在，请先选择一个场景开始学习。");
            resp.setHintLevel("OBSERVE");
            resp.setEnlightened(false);
            resp.setSuggestions(new ArrayList<>());
            return resp;
        }

        session.setAttempts(session.getAttempts() + 1);
        session.setGmtModified(LocalDateTime.now());

        String systemPrompt = SOCRATIC_SYSTEM_PROMPT.formatted(
            scenario.getKnowledgePoint() != null ? sanitize(scenario.getKnowledgePoint()) : "",
            scenario.getExpectedPitfall() != null ? sanitize(scenario.getExpectedPitfall()) : "",
            scenario.getCorrectExplanation() != null ? sanitize(scenario.getCorrectExplanation()) : "",
            session.getChatHistory() != null ? sanitize(session.getChatHistory()) : "[]",
            request.getLastOutput() != null ? sanitize(request.getLastOutput()) : "尚未运行过代码"
        );

        try {
            String userContent = "用户当前代码:\n```java\n" + (request.getCurrentCode() != null ? request.getCurrentCode() : "") + "\n```\n\n用户说: " + (request.getMessage() != null ? request.getMessage() : "我运行了代码，但还没发现什么特别的。");

            String result = llmClient.chatSimple(systemPrompt, userContent);

            ChatResponse response = JSON.parseObject(result, ChatResponse.class);
            if (response == null) {
                response = new ChatResponse();
                response.setReply(result);
                response.setHintLevel("HINT");
                response.setEnlightened(false);
                response.setSuggestions(new ArrayList<>());
            }

            if (response.isEnlightened()) {
                session.setStatus("ENLIGHTENED");
            }

            List<Object> history = JSON.parseArray(session.getChatHistory(), Object.class);
            if (history == null) history = new ArrayList<>();
            history.add(java.util.Map.of(
                "role", "user",
                "content", request.getMessage(),
                "code", request.getCurrentCode(),
                "output", request.getLastOutput()
            ));
            history.add(java.util.Map.of(
                "role", "assistant",
                "content", response.getReply()
            ));
            session.setChatHistory(JSON.toJSONString(history));

            sessionMapper.updateById(session);

            return response;
        } catch (Exception e) {
            log.error("AI对话失败", e);

            ChatResponse fallback = generateFallbackResponse(scenario, session.getAttempts());
            session.setChatHistory(JSON.toJSONString(List.of(
                java.util.Map.of("role", "user", "content", request.getMessage()),
                java.util.Map.of("role", "assistant", "content", fallback.getReply())
            )));
            sessionMapper.updateById(session);
            return fallback;
        }
    }

    private ChatResponse generateFallbackResponse(ScenarioDto scenario, int attempts) {
        ChatResponse response = new ChatResponse();
        response.setEnlightened(false);
        response.setSuggestions(new ArrayList<>());

        List<String> hints = scenario.getHints();
        if (hints != null && !hints.isEmpty()) {
            int idx = Math.min(attempts - 1, hints.size() - 1);
            response.setReply(hints.get(idx));
            response.setHintLevel(attempts <= 2 ? "OBSERVE" : attempts <= 4 ? "HINT" : "STRONG_HINT");
        } else {
            response.setReply("你刚刚运行了代码，注意到了什么奇怪的地方吗？再仔细看看输出结果 😊");
            response.setHintLevel("OBSERVE");
        }

        return response;
    }

    private String sanitize(String s) {
        return s.replace("%", "%%");
    }
}

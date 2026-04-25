package com.cortex.lab.controller;

import com.cortex.dto.ApiResponse;
import com.cortex.lab.dto.*;
import com.cortex.lab.entity.*;
import com.cortex.lab.mapper.LabSessionMapper;
import com.cortex.lab.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {

    private final ScenarioService scenarioService;
    private final SandboxService sandboxService;
    private final DialogueService dialogueService;
    private final LabSessionMapper sessionMapper;
    private final QuestionBankService questionBankService;
    private final KnowledgeCardService knowledgeCardService;
    private final DiscussionService discussionService;

    // ==================== 场景相关 (原有) ====================

    @GetMapping("/scenarios")
    public ApiResponse<List<ScenarioDto>> listScenarios() {
        try {
            return ApiResponse.success(scenarioService.listAll());
        } catch (Exception e) {
            log.error("获取场景列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<ScenarioDto> getScenario(@PathVariable Long id) {
        try {
            ScenarioDto dto = scenarioService.getById(id);
            if (dto == null) return ApiResponse.error("场景不存在");
            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("获取场景详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/scenarios/generate")
    public ApiResponse<ScenarioDto> generateScenario(@RequestBody Map<String, String> body) {
        try {
            String knowledgePoint = body.get("knowledgePoint");
            String category = body.get("category");
            if (knowledgePoint == null || knowledgePoint.isBlank()) {
                return ApiResponse.error("请输入知识点描述");
            }
            ScenarioDto dto = scenarioService.generateFromKnowledge(knowledgePoint, category);
            return ApiResponse.success("场景生成成功", dto);
        } catch (Exception e) {
            log.error("生成场景失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 代码执行 ====================

    @PostMapping("/execute")
    public ApiResponse<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        try {
            if (request.getCode() == null || request.getCode().isBlank()) {
                return ApiResponse.error("代码不能为空");
            }
            ExecuteResponse result = sandboxService.execute(request.getCode());
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("执行代码失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 学习会话 & AI 对话 ====================

    @PostMapping("/session/start")
    public ApiResponse<Map<String, Object>> startSession(@RequestBody Map<String, Object> body) {
        try {
            Long scenarioId = Long.valueOf(body.get("scenarioId").toString());
            String userId = (String) body.getOrDefault("userId", "anonymous");

            ScenarioDto scenario = scenarioService.getById(scenarioId);
            if (scenario == null) return ApiResponse.error("场景不存在");

            LabSession session = dialogueService.startSession(scenarioId, userId, scenario);

            return ApiResponse.success(Map.of(
                "sessionId", session.getSessionId(),
                "scenarioId", scenarioId,
                "trapCode", scenario.getTrapCode(),
                "knowledgePoint", scenario.getKnowledgePoint()
            ));
        } catch (Exception e) {
            log.error("开始会话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(@RequestBody ChatRequest request) {
        try {
            if (request.getSessionId() == null) {
                return ApiResponse.error("请先开始一个学习会话");
            }

            LabSession session = sessionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabSession>()
                    .eq(LabSession::getSessionId, request.getSessionId())
            );
            if (session == null) return ApiResponse.error("会话不存在");

            ScenarioDto scenario = scenarioService.getById(session.getScenarioId());
            if (scenario == null) return ApiResponse.error("场景不存在");

            ChatResponse response = dialogueService.chat(request, scenario);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("AI对话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 题库系统 ====================

    @GetMapping("/questions")
    public ApiResponse<List<QuestionDto>> listQuestions(@RequestParam(required = false) String userId) {
        try {
            return ApiResponse.success(questionBankService.listAll(userId));
        } catch (Exception e) {
            log.error("获取题库失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/questions/{id}")
    public ApiResponse<QuestionDto> getQuestion(@PathVariable Long id,
                                                 @RequestParam(required = false) String userId) {
        try {
            QuestionDto dto = questionBankService.getById(id, userId);
            if (dto == null) return ApiResponse.error("题目不存在");
            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("获取题目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/generate")
    public ApiResponse<QuestionDto> generateQuestion(@RequestBody Map<String, String> body) {
        try {
            String userQuestion = body.get("question");
            if (userQuestion == null || userQuestion.isBlank()) {
                return ApiResponse.error("请输入你要学习的问题");
            }
            QuestionDto dto = questionBankService.generateFromQuestion(userQuestion);
            return ApiResponse.success("题目生成成功，开始学习吧！", dto);
        } catch (Exception e) {
            log.error("生成题目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/import")
    public ApiResponse<QuestionDto> importQuestion(@RequestBody Map<String, String> body) {
        try {
            String title = body.get("title");
            String description = body.get("description");
            String trapCode = body.get("trapCode");
            String expectedPitfall = body.get("expectedPitfall");
            String correctExplanation = body.get("correctExplanation");
            if (title == null || title.isBlank()) return ApiResponse.error("请输入标题");
            QuestionDto dto = questionBankService.importQuestion(title, description, trapCode, expectedPitfall, correctExplanation);
            return ApiResponse.success("导入成功", dto);
        } catch (Exception e) {
            log.error("导入题目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/batch-import")
    public ApiResponse<Map<String, Object>> batchImport(@RequestBody Map<String, String> body) {
        try {
            String text = body.get("text");
            String category = body.getOrDefault("category", "批量导入");
            if (text == null || text.isBlank()) return ApiResponse.error("请输入题目内容");
            int count = questionBankService.batchImport(text, category);
            return ApiResponse.success(Map.of("count", count));
        } catch (Exception e) {
            log.error("批量导入失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/questions/{id}")
    public ApiResponse<Void> deleteQuestion(@PathVariable Long id) {
        try {
            questionBankService.deleteQuestion(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除题目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 进度管理 ====================

    @PostMapping("/questions/{id}/mastered")
    public ApiResponse<Void> setMastered(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String userId = (String) body.getOrDefault("userId", "anonymous");
            boolean mastered = Boolean.TRUE.equals(body.get("mastered"));
            questionBankService.setMastered(id, userId, mastered);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新状态失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/questions/review/due")
    public ApiResponse<List<QuestionDto>> getDueReviews(@RequestParam String userId) {
        try {
            return ApiResponse.success(questionBankService.getReviewList(userId));
        } catch (Exception e) {
            log.error("获取复习列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/review")
    public ApiResponse<Void> updateReview(@RequestBody Map<String, Object> body) {
        try {
            Long questionId = Long.valueOf(body.get("questionId").toString());
            String userId = (String) body.getOrDefault("userId", "anonymous");
            boolean stillMastered = Boolean.TRUE.equals(body.get("stillMastered"));
            questionBankService.updateReview(questionId, userId, stillMastered);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新复习状态失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 知识卡片 ====================

    @GetMapping("/questions/{id}/card")
    public ApiResponse<CardDto> getCard(@PathVariable Long id) {
        try {
            CardDto card = knowledgeCardService.getByQuestionId(id);
            if (card == null) return ApiResponse.error("知识卡片尚未生成");
            return ApiResponse.success(card);
        } catch (Exception e) {
            log.error("获取知识卡片失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/{id}/card/generate")
    public ApiResponse<CardDto> generateCard(@PathVariable Long id) {
        try {
            CardDto card = knowledgeCardService.generateCard(id);
            return ApiResponse.success("知识卡片生成成功！", card);
        } catch (Exception e) {
            log.error("生成知识卡片失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 讨论区 ====================

    @GetMapping("/questions/{id}/discussions")
    public ApiResponse<List<DiscussionDto>> getDiscussions(@PathVariable Long id) {
        try {
            return ApiResponse.success(discussionService.getByQuestionId(id));
        } catch (Exception e) {
            log.error("获取讨论失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/{id}/discussions")
    public ApiResponse<DiscussionDto> addDiscussion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String content = body.get("content");
            String userId = body.getOrDefault("userId", "anonymous");
            Long parentId = body.containsKey("parentId") && body.get("parentId") != null
                ? Long.valueOf(body.get("parentId")) : null;
            if (content == null || content.isBlank()) return ApiResponse.error("请输入评论内容");
            DiscussionDto dto = discussionService.addComment(id, parentId, userId, content);
            return ApiResponse.success("评论成功", dto);
        } catch (Exception e) {
            log.error("添加讨论失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/discussions/{id}")
    public ApiResponse<Void> deleteDiscussion(@PathVariable Long id) {
        try {
            discussionService.deleteComment(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除讨论失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}

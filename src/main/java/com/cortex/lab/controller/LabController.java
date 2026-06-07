package com.cortex.lab.controller;

import com.cortex.dto.ApiResponse;
import com.cortex.lab.dto.*;
import com.cortex.lab.entity.*;
import com.cortex.lab.mapper.LabSessionMapper;
import com.cortex.lab.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
public class LabController {
    // 实验室主控制器：知识树、场景、代码执行、AI 对话、题库、社区陷阱

    private final ScenarioService scenarioService;
    private final SandboxService sandboxService;
    private final DialogueService dialogueService;
    private final LabSessionMapper sessionMapper;
    private final QuestionBankService questionBankService;
    private final KnowledgeCardService knowledgeCardService;
    private final DiscussionService discussionService;
    private final AssistantService assistantService;
    private final TutorService tutorService;
    private final KnowledgeTreeService knowledgeTreeService;
    private final SpringProjectService springProjectService;
    private final CommunityTrapService communityTrapService;

    // ==================== 知识树 ====================

    @GetMapping("/knowledge-tree")
    public ApiResponse<List<KnowledgeNodeDTO>> getKnowledgeTree() {
        // 获取知识树
        try {
            return ApiResponse.success(knowledgeTreeService.getTree());
        } catch (Exception e) {
            log.error("获取知识树失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/knowledge-tree/generate")
    public ApiResponse<ScenarioDto> generateFromTree(@RequestBody Map<String, String> body) {
        // 根据知识点生成陷阱代码
        try {
            String nodeId = body.get("nodeId");
            if (nodeId == null || nodeId.isBlank()) {
                return ApiResponse.error("请选择知识点");
            }
            ScenarioDto dto = knowledgeTreeService.generateForNode(nodeId);
            return ApiResponse.success("代码生成成功", dto);
        } catch (Exception e) {
            log.error("知识树代码生成失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/knowledge-tree/master")
    public ApiResponse<Void> toggleMasterNode(@RequestBody Map<String, String> body) {
        // 切换知识节点掌握状态
        try {
            String nodeId = body.get("nodeId");
            boolean mastered = Boolean.parseBoolean(body.getOrDefault("mastered", "false"));
            String userId = body.getOrDefault("userId", "anonymous");
            knowledgeTreeService.toggleMastered(nodeId, userId, mastered);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("更新知识节点状态失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/knowledge-tree/project")
    public ApiResponse<ProjectInfoDTO> generateProject(@RequestBody Map<String, String> body) {
        // 根据知识点生成 Spring Boot 项目
        try {
            String nodeId = body.get("nodeId");
            if (nodeId == null || nodeId.isBlank()) {
                return ApiResponse.error("请选择知识点");
            }
            ProjectInfoDTO project = knowledgeTreeService.generateProject(nodeId);
            return ApiResponse.success("项目生成成功", project);
        } catch (Exception e) {
            log.error("生成 Spring Boot 项目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/project/build")
    public ApiResponse<Map<String, Object>> buildProject(@RequestBody Map<String, Object> body) {
        // 构建 Maven 项目
        try {
            String projectDir = (String) body.get("projectDir");
            if (projectDir == null) {
                return ApiResponse.error("项目目录不能为空");
            }
            // Re-build from files in the project directory
            @SuppressWarnings("unchecked")
            List<Map<String, String>> fileMaps = (List<Map<String, String>>) body.get("files");
            if (fileMaps == null || fileMaps.isEmpty()) {
                return ApiResponse.error("项目文件不能为空");
            }
            List<ProjectFileDTO> files = fileMaps.stream()
                .map(m -> new ProjectFileDTO(m.get("path"), m.get("content")))
                .toList();
            var result = sandboxService.buildMavenProject(files);
            return ApiResponse.success(Map.of(
                "success", result.isSuccess(),
                "output", result.getOutput(),
                "exitCode", result.getExitCode(),
                "elapsedMs", result.getElapsedMs(),
                "projectDir", result.getProjectDir()
            ));
        } catch (Exception e) {
            log.error("构建项目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/knowledge-tree/mastered")
    public ApiResponse<List<String>> getMasteredNodeIds(@RequestParam(defaultValue = "anonymous") String userId) {
        // 获取已掌握节点ID列表
        try {
            return ApiResponse.success(knowledgeTreeService.getMasteredNodeIds(userId));
        } catch (Exception e) {
            log.error("获取掌握节点失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 场景相关 (原有) ====================

    @GetMapping("/scenarios")
    public ApiResponse<List<ScenarioDto>> listScenarios() {
        // 获取场景列表
        try {
            return ApiResponse.success(scenarioService.listAll());
        } catch (Exception e) {
            log.error("获取场景列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/scenarios/{id}")
    public ApiResponse<ScenarioDto> getScenario(@PathVariable Long id) {
        // 获取场景详情
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
        // 根据知识点生成新场景
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
        // 执行 Java 代码
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
        // 开始学习会话
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
        // AI 对话（场景模式）
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

    @GetMapping("/questions/search")
    public ApiResponse<List<QuestionDto>> searchQuestions(@RequestParam String keyword) {
        // 搜索题目
        try {
            return ApiResponse.success(questionBankService.searchQuestions(keyword));
        } catch (Exception e) {
            log.error("搜索题目失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/questions")
    public ApiResponse<List<QuestionDto>> listQuestions(@RequestParam(required = false) String userId) {
        // 获取题库列表
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
        // 获取题目详情
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
        // AI 生成题目
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
        // 手动导入题目
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
        // 批量导入题目
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
        // 删除题目
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
        // 标记题目掌握状态
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
        // 获取待复习题目
        try {
            return ApiResponse.success(questionBankService.getReviewList(userId));
        } catch (Exception e) {
            log.error("获取复习列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/review")
    public ApiResponse<Void> updateReview(@RequestBody Map<String, Object> body) {
        // 更新复习状态
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

    @GetMapping("/cards")
    public ApiResponse<List<CardDto>> listCards() {
        // 获取知识卡片列表
        try {
            return ApiResponse.success(knowledgeCardService.listAll());
        } catch (Exception e) {
            log.error("获取知识卡片列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/questions/{id}/card")
    public ApiResponse<CardDto> getCard(@PathVariable Long id) {
        // 获取题目的知识卡片
        try {
            CardDto card = knowledgeCardService.getByQuestionId(id);
            if (card == null) return ApiResponse.error("知识卡片尚未生成");
            return ApiResponse.success(card);
        } catch (Exception e) {
            log.error("获取知识卡片失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/cards/{id}")
    public ApiResponse<Void> deleteCard(@PathVariable Long id) {
        // 删除知识卡片
        try {
            knowledgeCardService.deleteCard(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除知识卡片失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/cards/{id}")
    public ApiResponse<CardDto> updateCard(@PathVariable Long id, @RequestBody CardDto dto) {
        // 更新知识卡片
        try {
            CardDto card = knowledgeCardService.updateCard(id, dto);
            return ApiResponse.success("知识卡片更新成功！", card);
        } catch (Exception e) {
            log.error("更新知识卡片失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/{id}/card/generate")
    public ApiResponse<CardDto> generateCard(@PathVariable Long id) {
        // AI 生成知识卡片
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
        // 获取题目讨论列表
        try {
            return ApiResponse.success(discussionService.getByQuestionId(id));
        } catch (Exception e) {
            log.error("获取讨论失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/questions/{id}/discussions")
    public ApiResponse<DiscussionDto> addDiscussion(@PathVariable Long id, @RequestBody Map<String, String> body) {
        // 添加评论
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
        // 删除评论
        try {
            discussionService.deleteComment(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除讨论失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 全局AI助手 ====================

    @GetMapping("/assistant/welcome")
    public ApiResponse<GlobalChatResponse> assistantWelcome(@RequestParam(defaultValue = "anonymous") String userId) {
        try {
            return ApiResponse.success(assistantService.welcome(userId));
        } catch (Exception e) {
            log.error("获取欢迎信息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/assistant/chat")
    public ApiResponse<GlobalChatResponse> assistantChat(@RequestBody GlobalChatRequest request) {
        // AI 助手对话
        try {
            if (request.getMessage() == null || request.getMessage().isBlank()) {
                return ApiResponse.error("请输入消息");
            }
            GlobalChatResponse response = assistantService.chat(request);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("全局AI对话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/assistant/config")
    public ApiResponse<Map<String, String>> getAssistantConfig() {
        // 获取助手配置
        try {
            return ApiResponse.success(assistantService.loadConfig());
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/assistant/config")
    public ApiResponse<Map<String, String>> updateAssistantConfig(@RequestBody Map<String, String> updates) {
        // 更新助手配置
        try {
            return ApiResponse.success("配置更新成功", assistantService.updateConfig(updates));
        } catch (Exception e) {
            log.error("更新配置失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/assistant/conversations")
    public ApiResponse<List<AssistantConversation>> listConversations(@RequestParam(defaultValue = "anonymous") String userId) {
        // 获取对话列表
        try {
            return ApiResponse.success(assistantService.listConversations(userId));
        } catch (Exception e) {
            log.error("获取对话列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/assistant/conversations/{conversationId}/messages")
    public ApiResponse<List<AssistantMessage>> getConversationMessages(@PathVariable String conversationId) {
        // 获取对话消息
        try {
            return ApiResponse.success(assistantService.getMessages(conversationId));
        } catch (Exception e) {
            log.error("获取消息失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/assistant/conversations/{conversationId}")
    public ApiResponse<Void> deleteConversation(@PathVariable String conversationId) {
        // 删除对话
        try {
            assistantService.deleteConversation(conversationId);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("删除对话失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/assistant/chat/stream")
    public SseEmitter assistantChatStream(@RequestBody GlobalChatRequest request) {
        // AI 助手流式对话（SSE）
        return assistantService.chatStream(request);
    }

    @PostMapping("/assistant/feedback")
    public ApiResponse<Void> submitFeedback(@RequestBody Map<String, Object> body) {
        // 提交对话反馈
        try {
            String conversationId = (String) body.get("conversationId");
            Long messageId = body.get("messageId") != null ? Long.valueOf(body.get("messageId").toString()) : null;
            String userId = (String) body.getOrDefault("userId", "anonymous");
            int rating = Integer.parseInt(body.get("rating").toString());
            String comment = (String) body.get("comment");
            assistantService.submitFeedback(conversationId, messageId, userId, rating, comment);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("提交反馈失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 导师自评 ====================

    @PostMapping("/tutor/assess")
    public ApiResponse<TutorReviewResponse> tutorAssess(@RequestBody TutorReviewRequest request) {
        // 用户自评：答对/答错，AI返回点评+拓展知识点
        try {
            if (request.getQuestionId() == null) {
                return ApiResponse.error("题目ID不能为空");
            }
            TutorReviewResponse resp = tutorService.assess(
                request.getQuestionId(),
                request.getUserId(),
                request.isCorrect()
            );
            return ApiResponse.success(resp);
        } catch (Exception e) {
            log.error("导师点评失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/tutor/explain-tip")
    public ApiResponse<TutorExplainResponse> tutorExplainTip(@RequestBody TutorExplainRequest request) {
        // 解释某个拓展知识点
        try {
            if (request.getQuestionId() == null || request.getTip() == null) {
                return ApiResponse.error("参数不完整");
            }
            TutorExplainResponse resp = tutorService.explainTip(
                request.getQuestionId(),
                request.getTip(),
                request.getUserId()
            );
            return ApiResponse.success(resp);
        } catch (Exception e) {
            log.error("解释知识点失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    // ==================== 社区陷阱众包 ====================

    @PostMapping("/community/submit")
    public ApiResponse<CommunityTrapDto> submitCommunityTrap(@RequestBody CommunityTrapSubmitRequest req) {
        // 提交社区陷阱代码
        try {
            if (req.getTitle() == null || req.getTitle().isBlank()) {
                return ApiResponse.error("请输入标题");
            }
            if (req.getTrapCode() == null || req.getTrapCode().isBlank()) {
                return ApiResponse.error("请输入陷阱代码");
            }
            CommunityTrapDto dto = communityTrapService.submit(req);
            return ApiResponse.success("提交成功，等待审核", dto);
        } catch (Exception e) {
            log.error("提交社区陷阱失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/community/traps")
    public ApiResponse<List<CommunityTrapDto>> listCommunityTraps(
            @RequestParam(required = false, defaultValue = "all") String javaVersion,
            @RequestParam(required = false, defaultValue = "all") String category,
            @RequestParam(required = false, defaultValue = "all") String status) {
        // 获取社区陷阱列表
        try {
            return ApiResponse.success(communityTrapService.listTraps(javaVersion, category, status));
        } catch (Exception e) {
            log.error("获取社区陷阱列表失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/community/traps/{id}")
    public ApiResponse<CommunityTrapDto> getCommunityTrap(@PathVariable Long id) {
        // 获取陷阱详情
        try {
            CommunityTrapDto dto = communityTrapService.getById(id);
            if (dto == null) return ApiResponse.error("陷阱不存在");
            return ApiResponse.success(dto);
        } catch (Exception e) {
            log.error("获取社区陷阱详情失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/community/traps/{id}/vote")
    public ApiResponse<Void> voteCommunityTrap(@PathVariable Long id) {
        // 投票
        try {
            communityTrapService.vote(id);
            return ApiResponse.success(null);
        } catch (Exception e) {
            log.error("投票失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/community/traps/{id}/approve")
    public ApiResponse<CommunityTrapDto> approveCommunityTrap(@PathVariable Long id) {
        // 审核通过
        try {
            return ApiResponse.success("已通过", communityTrapService.approve(id));
        } catch (Exception e) {
            log.error("审核通过失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/community/traps/{id}/reject")
    public ApiResponse<CommunityTrapDto> rejectCommunityTrap(@PathVariable Long id) {
        // 审核拒绝
        try {
            return ApiResponse.success("已拒绝", communityTrapService.reject(id));
        } catch (Exception e) {
            log.error("审核拒绝失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/community/traps/{id}/integrate")
    public ApiResponse<Void> integrateCommunityTrap(@PathVariable Long id) {
        // 导入题库
        try {
            communityTrapService.integrateIntoQuestionBank(id);
            return ApiResponse.success("已导入题库", null);
        } catch (Exception e) {
            log.error("导入题库失败", e);
            return ApiResponse.error(e.getMessage());
        }
    }
}

package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.lab.dto.QuestionDto;
import com.cortex.lab.entity.KnowledgeCard;
import com.cortex.lab.entity.QuestionBank;
import com.cortex.lab.entity.QuestionProgress;
import com.cortex.lab.entity.LabScenario;
import com.cortex.lab.mapper.KnowledgeCardMapper;
import com.cortex.lab.mapper.LabScenarioMapper;
import com.cortex.lab.mapper.QuestionBankMapper;
import com.cortex.lab.mapper.QuestionProgressMapper;
import com.cortex.llm.LlmClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionBankService {

    private final QuestionBankMapper questionBankMapper;
    private final QuestionProgressMapper progressMapper;
    private final LlmClient llmClient;
    private final KnowledgeCardMapper cardMapper;
    private final LabScenarioMapper labScenarioMapper;

    public QuestionBankService(QuestionBankMapper questionBankMapper,
                                QuestionProgressMapper progressMapper,
                                LlmClient llmClient,
                                KnowledgeCardMapper cardMapper,
                                LabScenarioMapper labScenarioMapper) {
        this.questionBankMapper = questionBankMapper;
        this.progressMapper = progressMapper;
        this.llmClient = llmClient;
        this.cardMapper = cardMapper;
        this.labScenarioMapper = labScenarioMapper;
    }

    @PostConstruct
    public void seedBuiltinQuestions() {
        try {
            long count = questionBankMapper.selectCount(null);
            if (count > 0) return;

            List<LabScenario> scenarios = labScenarioMapper.selectList(null);
            if (scenarios == null || scenarios.isEmpty()) return;

            for (LabScenario s : scenarios) {
                QuestionBank q = new QuestionBank();
                q.setTitle(s.getKnowledgePoint());
                q.setDescription(s.getExpectedPitfall());
                q.setTrapCode(s.getTrapCode());
                q.setExpectedPitfall(s.getExpectedPitfall());
                q.setCorrectExplanation(s.getCorrectExplanation());
                q.setHints(s.getHints());
                q.setCategory(s.getCategory() != null ? s.getCategory() : "Java基础");
                q.setDifficulty(s.getDifficulty() != null ? s.getDifficulty() : 1);
                q.setStatus("ACTIVE");
                q.setGmtCreate(LocalDateTime.now());
                q.setGmtModified(LocalDateTime.now());
                questionBankMapper.insert(q);
            }
            log.info("已从内置场景同步 {} 道题目到题库", scenarios.size());
        } catch (Exception e) {
            log.warn("同步内置题目到题库失败（首次启动时可能无数据）: {}", e.getMessage());
        }
    }

    private static final String GENERATE_PROMPT = """
你是一个编程教学专家。用户提出了一个技术问题，请生成一段包含陷阱的 Java 代码和教学材料。
返回严格的 JSON 格式（不要 markdown 标记）：

{
  "title": "题目标题",
  "description": "问题描述，说明这个陷阱考察什么",
  "trapCode": "完整的可编译运行的 Java 代码（必须包含 main 方法），代码表面正常但暗藏陷阱，通过输出让学习者发现异常",
  "expectedPitfall": "预期的意外现象（一句话）",
  "correctExplanation": "正确原理解释（200字以内）",
  "hints": ["提示1（引导性）", "提示2", "提示3"],
  "difficulty": 2
}

用户问题: %s
""";

    private static final String CARD_GENERATE_PROMPT = """
你是一个知识整理专家。请根据以下题目信息，生成一张结构化的知识卡片。
返回严格的 JSON 格式（不要 markdown 标记）：

{
  "title": "知识点标题",
  "keyPoints": "核心要点（分点列举，用 | 分隔）",
  "detailExplanation": "详细原理解释",
  "codeSnippet": "总结性的代码示例（展示正确用法）",
  "commonPitfalls": "常见误区（分点列举，用 | 分隔）"
}

题目: %s
陷阱: %s
正确解释: %s
""";

    public List<QuestionDto> listAll(String userId) {
        List<QuestionBank> questions = questionBankMapper.selectList(null);
        return questions.stream().map(q -> {
            QuestionDto dto = toDto(q);
            if (userId != null) {
                QuestionProgress p = progressMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                        .eq(QuestionProgress::getQuestionId, q.getId())
                        .eq(QuestionProgress::getUserId, userId)
                );
                if (p != null) {
                    dto.setMastered(p.getMastered());
                    dto.setReviewCount(p.getReviewCount());
                    dto.setNextReviewTime(p.getNextReviewTime());
                }
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public QuestionDto getById(Long id, String userId) {
        QuestionBank q = questionBankMapper.selectById(id);
        if (q == null) return null;
        QuestionDto dto = toDto(q);
        if (userId != null) {
            QuestionProgress p = progressMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                    .eq(QuestionProgress::getQuestionId, id)
                    .eq(QuestionProgress::getUserId, userId)
            );
            if (p != null) {
                dto.setMastered(p.getMastered());
                dto.setReviewCount(p.getReviewCount());
                dto.setNextReviewTime(p.getNextReviewTime());
            }
        }
        return dto;
    }

    public QuestionDto generateFromQuestion(String userQuestion) {
        String prompt = GENERATE_PROMPT.formatted(userQuestion);
        try {
            String result = llmClient.chatSimple(prompt);
            result = cleanJson(result);
            QuestionDto dto = JSON.parseObject(result, QuestionDto.class);

            QuestionBank entity = new QuestionBank();
            entity.setTitle(dto.getTitle());
            entity.setDescription(dto.getDescription());
            entity.setTrapCode(dto.getTrapCode());
            entity.setExpectedPitfall(dto.getExpectedPitfall());
            entity.setCorrectExplanation(dto.getCorrectExplanation());
            entity.setHints(dto.getHints() != null ? JSON.toJSONString(dto.getHints()) : "[]");
            entity.setCategory("用户导入");
            entity.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : 2);
            entity.setStatus("ACTIVE");
            entity.setGmtCreate(LocalDateTime.now());
            entity.setGmtModified(LocalDateTime.now());
            questionBankMapper.insert(entity);

            return toDto(entity);
        } catch (Exception e) {
            log.error("AI 生成题目失败", e);
            throw new RuntimeException("AI 生成失败: " + e.getMessage());
        }
    }

    public QuestionDto importQuestion(String title, String description, String trapCode,
                                       String expectedPitfall, String correctExplanation) {
        QuestionBank entity = new QuestionBank();
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setTrapCode(trapCode);
        entity.setExpectedPitfall(expectedPitfall);
        entity.setCorrectExplanation(correctExplanation);
        entity.setCategory("用户自定义");
        entity.setDifficulty(2);
        entity.setStatus("ACTIVE");
        entity.setGmtCreate(LocalDateTime.now());
        entity.setGmtModified(LocalDateTime.now());
        questionBankMapper.insert(entity);
        return toDto(entity);
    }

    public int batchImport(String text, String category) {
        if (text == null || text.isBlank()) return 0;
        String[] blocks = text.split("(?m)^---\\s*$");
        int count = 0;
        for (String block : blocks) {
            String trimmed = block.trim();
            if (trimmed.isEmpty()) continue;
            try {
                String title = "";
                String description = "";
                String trapCode = "";
                String expectedPitfall = "";
                String correctExplanation = "";

                String[] lines = trimmed.split("\n");
                StringBuilder current = new StringBuilder();
                String section = "title";
                for (String line : lines) {
                    String tl = line.trim();
                    if (tl.startsWith("## ")) {
                        switch (section) {
                            case "title": title = current.toString().trim(); break;
                            case "desc": description = current.toString().trim(); break;
                            case "code": trapCode = current.toString().trim(); break;
                            case "pitfall": expectedPitfall = current.toString().trim(); break;
                        }
                        current = new StringBuilder();
                        section = tl.substring(3).toLowerCase().replace(":", "").trim();
                        if (section.equals("题目")) section = "title";
                        else if (section.contains("描述")) section = "desc";
                        else if (section.contains("代码") || section.contains("陷阱")) section = "code";
                        else if (section.contains("预期") || section.contains("现象")) section = "pitfall";
                        else if (section.contains("解释") || section.contains("原理")) section = "explain";
                    } else {
                        current.append(line).append("\n");
                    }
                }
                if (section.equals("explain") || section.equals("code")) {
                    correctExplanation = current.toString().trim();
                }

                if (title.isBlank()) {
                    title = trimmed.contains("\n") ? trimmed.substring(0, trimmed.indexOf('\n')).trim() : trimmed;
                    if (title.length() > 100) title = title.substring(0, 100);
                }

                QuestionBank q = new QuestionBank();
                q.setTitle(title);
                q.setDescription(description);
                q.setTrapCode(trapCode);
                q.setExpectedPitfall(expectedPitfall);
                q.setCorrectExplanation(correctExplanation);
                q.setCategory(category != null ? category : "批量导入");
                q.setDifficulty(2);
                q.setStatus("ACTIVE");
                q.setGmtCreate(LocalDateTime.now());
                q.setGmtModified(LocalDateTime.now());
                questionBankMapper.insert(q);
                count++;
            } catch (Exception e) {
                log.warn("批量导入解析失败: {}", e.getMessage());
            }
        }
        return count;
    }

    public void deleteQuestion(Long id) {
        questionBankMapper.deleteById(id);
        progressMapper.delete(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                .eq(QuestionProgress::getQuestionId, id)
        );
    }

    public void setMastered(Long questionId, String userId, boolean mastered) {
        QuestionProgress p = progressMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                .eq(QuestionProgress::getQuestionId, questionId)
                .eq(QuestionProgress::getUserId, userId)
        );
        if (p == null) {
            p = new QuestionProgress();
            p.setQuestionId(questionId);
            p.setUserId(userId);
            p.setMastered(mastered);
            p.setReviewCount(0);
            p.setGmtCreate(LocalDateTime.now());
            p.setGmtModified(LocalDateTime.now());
            if (mastered) {
                p.setLastReviewTime(LocalDateTime.now());
                p.setNextReviewTime(LocalDateTime.now().plusDays(1));
            }
            progressMapper.insert(p);
        } else {
            p.setMastered(mastered);
            p.setGmtModified(LocalDateTime.now());
            if (mastered) {
                p.setLastReviewTime(LocalDateTime.now());
                p.setNextReviewTime(LocalDateTime.now().plusDays(1));
            }
            progressMapper.updateById(p);
        }
    }

    public List<QuestionDto> getReviewList(String userId) {
        List<QuestionProgress> dueProgresses = progressMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                .eq(QuestionProgress::getUserId, userId)
                .eq(QuestionProgress::getMastered, true)
                .apply("next_review_time IS NOT NULL AND next_review_time <= NOW()")
        );
        return dueProgresses.stream().map(p -> {
            QuestionBank q = questionBankMapper.selectById(p.getQuestionId());
            if (q == null) return null;
            QuestionDto dto = toDto(q);
            dto.setMastered(p.getMastered());
            dto.setReviewCount(p.getReviewCount());
            dto.setNextReviewTime(p.getNextReviewTime());
            return dto;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public void updateReview(Long questionId, String userId, boolean stillMastered) {
        QuestionProgress p = progressMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionProgress>()
                .eq(QuestionProgress::getQuestionId, questionId)
                .eq(QuestionProgress::getUserId, userId)
        );
        if (p != null) {
            if (stillMastered) {
                p.setReviewCount(p.getReviewCount() != null ? p.getReviewCount() + 1 : 1);
                int count = p.getReviewCount();
                int intervalDays = count <= 1 ? 1 : count <= 3 ? 3 : count <= 5 ? 7 : 15;
                p.setNextReviewTime(LocalDateTime.now().plusDays(intervalDays));
                p.setLastReviewTime(LocalDateTime.now());
                p.setMastered(true);
            } else {
                p.setMastered(false);
                p.setReviewCount(0);
                p.setNextReviewTime(null);
            }
            p.setGmtModified(LocalDateTime.now());
            progressMapper.updateById(p);
        }
    }

    public KnowledgeCard generateCard(Long questionId) {
        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) throw new RuntimeException("题目不存在");

        String prompt = CARD_GENERATE_PROMPT.formatted(q.getTitle(), q.getExpectedPitfall(), q.getCorrectExplanation());
        try {
            String result = llmClient.chatSimple(prompt);
            result = cleanJson(result);
            CardGenResponse gen = JSON.parseObject(result, CardGenResponse.class);

            KnowledgeCard card = new KnowledgeCard();
            card.setQuestionId(questionId);
            card.setTitle(gen.title);
            card.setKeyPoints(gen.keyPoints);
            card.setDetailExplanation(gen.detailExplanation);
            card.setCodeSnippet(gen.codeSnippet);
            card.setCommonPitfalls(gen.commonPitfalls);
            card.setGmtCreate(LocalDateTime.now());
            card.setGmtModified(LocalDateTime.now());

            com.cortex.lab.mapper.KnowledgeCardMapper cardMapper = this.cardMapper;
            cardMapper.insert(card);
            return card;
        } catch (Exception e) {
            log.error("生成知识卡片失败", e);
            throw new RuntimeException("知识卡片生成失败: " + e.getMessage());
        }
    }

    @lombok.Data
    static class CardGenResponse {
        private String title;
        private String keyPoints;
        private String detailExplanation;
        private String codeSnippet;
        private String commonPitfalls;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        raw = raw.trim();
        if (raw.startsWith("```")) {
            int start = raw.indexOf('\n');
            int end = raw.lastIndexOf("```");
            if (start > 0 && end > start) {
                raw = raw.substring(start, end).trim();
            }
        }
        return raw;
    }

    private QuestionDto toDto(QuestionBank entity) {
        QuestionDto dto = new QuestionDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setTrapCode(entity.getTrapCode());
        dto.setExpectedPitfall(entity.getExpectedPitfall());
        dto.setCorrectExplanation(entity.getCorrectExplanation());
        dto.setHints(entity.getHints());
        dto.setCategory(entity.getCategory());
        dto.setDifficulty(entity.getDifficulty());
        dto.setStatus(entity.getStatus());
        dto.setGmtCreate(entity.getGmtCreate());
        return dto;
    }
}

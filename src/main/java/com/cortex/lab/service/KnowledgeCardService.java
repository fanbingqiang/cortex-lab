package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.lab.dto.CardDto;
import com.cortex.lab.entity.KnowledgeCard;
import com.cortex.lab.entity.QuestionBank;
import com.cortex.lab.mapper.KnowledgeCardMapper;
import com.cortex.lab.mapper.QuestionBankMapper;
import com.cortex.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeCardService {

    private final KnowledgeCardMapper cardMapper;
    private final QuestionBankMapper questionBankMapper;
    private final LlmClient llmClient;

    private static final String CARD_PROMPT = """
你是一个知识整理专家。请根据以下题目信息，生成一张结构化的知识卡片。
返回严格的 JSON 格式（不要 markdown 标记）：

{
  "title": "知识点标题",
  "keyPoints": "核心要点1|核心要点2|核心要点3",
  "detailExplanation": "详细原理解释（200字以内）",
  "codeSnippet": "正确用法的代码示例（展示正确写法，不要陷阱）",
  "commonPitfalls": "常见误区1|常见误区2|常见误区3"
}

题目: %s
陷阱: %s
正确解释: %s
""";

    public CardDto getByQuestionId(Long questionId) {
        KnowledgeCard card = cardMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<KnowledgeCard>()
                .eq(KnowledgeCard::getQuestionId, questionId)
        );
        return card != null ? toDto(card) : null;
    }

    public CardDto generateCard(Long questionId) {
        // 检查是否已有卡片
        CardDto existing = getByQuestionId(questionId);
        if (existing != null) return existing;

        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) throw new RuntimeException("题目不存在");

        String prompt = CARD_PROMPT.formatted(q.getTitle(), q.getExpectedPitfall(), q.getCorrectExplanation());
        try {
            String result = llmClient.chatSimple(prompt);
            result = cleanJson(result);
            CardGen gen = JSON.parseObject(result, CardGen.class);

            KnowledgeCard card = new KnowledgeCard();
            card.setQuestionId(questionId);
            card.setTitle(gen.title != null ? gen.title : q.getTitle());
            card.setKeyPoints(gen.keyPoints);
            card.setDetailExplanation(gen.detailExplanation);
            card.setCodeSnippet(gen.codeSnippet);
            card.setCommonPitfalls(gen.commonPitfalls);
            card.setGmtCreate(LocalDateTime.now());
            card.setGmtModified(LocalDateTime.now());
            cardMapper.insert(card);

            return toDto(card);
        } catch (Exception e) {
            log.error("生成知识卡片失败", e);
            // 降级：用现有数据生成卡片
            KnowledgeCard card = new KnowledgeCard();
            card.setQuestionId(questionId);
            card.setTitle(q.getTitle());
            card.setKeyPoints("""
1. 这是通过 AI 生成的问题
2. 运行代码观察意外结果
3. 尝试修改代码验证你的猜想""");
            card.setDetailExplanation(q.getCorrectExplanation() != null ? q.getCorrectExplanation() : q.getExpectedPitfall());
            card.setCodeSnippet(q.getTrapCode());
            card.setCommonPitfalls("1. 不要想当然 | 2. 动手运行验证 | 3. 理解原理而非记忆结果");
            card.setGmtCreate(LocalDateTime.now());
            card.setGmtModified(LocalDateTime.now());
            cardMapper.insert(card);
            return toDto(card);
        }
    }

    @lombok.Data
    static class CardGen {
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
            if (start > 0 && end > start) raw = raw.substring(start, end).trim();
        }
        return raw;
    }

    private CardDto toDto(KnowledgeCard card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setQuestionId(card.getQuestionId());
        dto.setTitle(card.getTitle());
        dto.setKeyPoints(card.getKeyPoints());
        dto.setDetailExplanation(card.getDetailExplanation());
        dto.setCodeSnippet(card.getCodeSnippet());
        dto.setCommonPitfalls(card.getCommonPitfalls());
        dto.setGmtCreate(card.getGmtCreate());
        return dto;
    }
}

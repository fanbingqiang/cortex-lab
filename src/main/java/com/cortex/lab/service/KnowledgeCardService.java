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
    你是一个知识整理专家。请根据以下信息生成一张知识卡片。
    返回严格的 JSON 格式（不要 markdown 标记）：
    {
      "title": "知识点标题（不超过50字）",
      "keyPoints": "要点1|要点2|要点3（每条不超过30字）",
      "detailExplanation": "详细解释（200字以内）",
      "codeSnippet": "正确代码示例（10行以内，只展示关键差异）",
      "commonPitfalls": "误区1|误区2（每条不超过30字）"
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

    public List<CardDto> listAll() {
        List<KnowledgeCard> cards = cardMapper.selectList(null);
        return cards.stream().map(c -> {
            CardDto dto = toDto(c);
            QuestionBank q = questionBankMapper.selectById(c.getQuestionId());
            if (q != null) dto.setQuestionTitle(q.getTitle());
            return dto;
        }).collect(Collectors.toList());
    }

    public CardDto generateCard(Long questionId) {
        CardDto existing = getByQuestionId(questionId);
        if (existing != null) return existing;

        QuestionBank q = questionBankMapper.selectById(questionId);
        if (q == null) throw new RuntimeException("题目不存在");

        String prompt = CARD_PROMPT.formatted(q.getTitle(), q.getExpectedPitfall(), q.getCorrectExplanation());
        try {
            String result = llmClient.chatSimple(prompt);
            result = com.cortex.util.JsonUtils.cleanJson(result);
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
            KnowledgeCard card = new KnowledgeCard();
            card.setQuestionId(questionId);
            card.setTitle(q.getTitle());
            card.setKeyPoints("读代码|猜输出|看结果");
            card.setDetailExplanation(q.getCorrectExplanation() != null ? q.getCorrectExplanation() : q.getExpectedPitfall());
            card.setCodeSnippet(q.getTrapCode());
            card.setCommonPitfalls("别想当然|动手验证");
            card.setGmtCreate(LocalDateTime.now());
            card.setGmtModified(LocalDateTime.now());
            cardMapper.insert(card);
            return toDto(card);
        }
    }

    public void deleteCard(Long id) {
        KnowledgeCard card = cardMapper.selectById(id);
        if (card == null) throw new RuntimeException("知识卡片不存在");
        cardMapper.deleteById(id);
    }

    public CardDto updateCard(Long id, CardDto dto) {
        KnowledgeCard card = cardMapper.selectById(id);
        if (card == null) throw new RuntimeException("知识卡片不存在");
        if (dto.getTitle() != null) card.setTitle(dto.getTitle());
        if (dto.getKeyPoints() != null) card.setKeyPoints(dto.getKeyPoints());
        if (dto.getDetailExplanation() != null) card.setDetailExplanation(dto.getDetailExplanation());
        if (dto.getCodeSnippet() != null) card.setCodeSnippet(dto.getCodeSnippet());
        if (dto.getCommonPitfalls() != null) card.setCommonPitfalls(dto.getCommonPitfalls());
        card.setGmtModified(LocalDateTime.now());
        cardMapper.updateById(card);
        return toDto(card);
    }

    @lombok.Data
    static class CardGen {
        private String title;
        private String keyPoints;
        private String detailExplanation;
        private String codeSnippet;
        private String commonPitfalls;
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

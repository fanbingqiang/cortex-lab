package com.cortex.lab.service;

import com.cortex.lab.dto.DiscussionDto;
import com.cortex.lab.entity.Discussion;
import com.cortex.lab.mapper.DiscussionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiscussionService {

    private final DiscussionMapper discussionMapper;

    public List<DiscussionDto> getByQuestionId(Long questionId) {
        List<Discussion> list = discussionMapper.selectList(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Discussion>()
                .eq(Discussion::getQuestionId, questionId)
                .orderByAsc(Discussion::getGmtCreate)
        );
        return list.stream().map(this::toDto).collect(Collectors.toList());
    }

    public DiscussionDto addComment(Long questionId, Long parentId, String userId, String content) {
        Discussion discussion = new Discussion();
        discussion.setQuestionId(questionId);
        discussion.setParentId(parentId);
        discussion.setUserId(userId);
        discussion.setContent(content);
        discussion.setGmtCreate(LocalDateTime.now());
        discussionMapper.insert(discussion);
        return toDto(discussion);
    }

    public void deleteComment(Long id) {
        discussionMapper.deleteById(id);
    }

    private DiscussionDto toDto(Discussion d) {
        DiscussionDto dto = new DiscussionDto();
        dto.setId(d.getId());
        dto.setQuestionId(d.getQuestionId());
        dto.setParentId(d.getParentId());
        dto.setUserId(d.getUserId());
        dto.setContent(d.getContent());
        dto.setGmtCreate(d.getGmtCreate());
        return dto;
    }
}

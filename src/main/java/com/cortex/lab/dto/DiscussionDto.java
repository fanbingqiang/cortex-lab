package com.cortex.lab.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DiscussionDto {
    private Long id;
    private Long questionId;
    private Long parentId;
    private String userId;
    private String content;
    private LocalDateTime gmtCreate;
}

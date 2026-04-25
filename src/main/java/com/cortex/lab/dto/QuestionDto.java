package com.cortex.lab.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class QuestionDto {
    private Long id;
    private String title;
    private String description;
    private String trapCode;
    private String expectedPitfall;
    private String correctExplanation;
    private String hints;
    private String category;
    private Integer difficulty;
    private String status;
    private Boolean mastered;
    private Integer reviewCount;
    private LocalDateTime nextReviewTime;
    private LocalDateTime gmtCreate;
}

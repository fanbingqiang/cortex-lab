package com.cortex.lab.dto;

import lombok.Data;

@Data
public class TutorReviewRequest {
    private Long questionId;
    private String userId;
    private boolean correct; // true=答对了, false=没答对
}
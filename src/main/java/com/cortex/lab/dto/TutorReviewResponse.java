package com.cortex.lab.dto;

import lombok.Data;
import java.util.List;

@Data
public class TutorReviewResponse {
    private String feedback;       // 对错反馈
    private List<String> examples; // 答错时给的例子(引导用)
    private List<String> tips;     // 答对后的拓展知识点
    private boolean hasMoreTip;    // 是否还有更多知识点
}
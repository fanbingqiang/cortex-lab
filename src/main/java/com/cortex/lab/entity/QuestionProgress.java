package com.cortex.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("lab_question_progress")
public class QuestionProgress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private String userId;
    private Boolean mastered;
    private Integer reviewCount;
    private LocalDateTime lastReviewTime;
    private LocalDateTime nextReviewTime;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

package com.cortex.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_learning")
public class DailyLearning {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private LocalDate logDate;
    private Integer questionsAnswered;
    private Integer correctCount;
    private Integer studyMinutes;
    private String knowledgePointsStudied;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
}

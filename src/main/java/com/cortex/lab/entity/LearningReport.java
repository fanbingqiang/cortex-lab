package com.cortex.lab.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("learning_report")
public class LearningReport {
    // 学习报表实体
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String reportType;
    private String reportData;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime gmtCreate;
}

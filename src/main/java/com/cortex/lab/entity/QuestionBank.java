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
@TableName("lab_question_bank")
public class QuestionBank {
    @TableId(type = IdType.AUTO)
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
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

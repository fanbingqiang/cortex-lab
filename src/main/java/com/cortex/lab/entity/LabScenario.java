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
@TableName("lab_scenario")
public class LabScenario {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String knowledgePoint;
    private String category;
    private String trapCode;
    private String expectedPitfall;
    private String correctExplanation;
    private String hints;
    private Integer difficulty;
    private LocalDateTime gmtCreate;
}

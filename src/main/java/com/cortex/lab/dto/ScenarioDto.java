package com.cortex.lab.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioDto {
    private Long id;
    private String knowledgePoint;
    private String category;
    private String trapCode;
    private String expectedPitfall;
    private String correctExplanation;
    private List<String> hints;
    private Integer difficulty;
}

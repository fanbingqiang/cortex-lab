package com.cortex.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNodeDTO {
    private String id;
    private String name;
    private String description;
    private List<KnowledgeNodeDTO> children;
    private boolean leaf;
}
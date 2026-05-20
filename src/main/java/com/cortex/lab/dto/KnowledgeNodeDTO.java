package com.cortex.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeNodeDTO {
    private String id;          // 节点唯一标识
    private String name;        // 节点名称
    private String description; // 节点描述
    private List<KnowledgeNodeDTO> children; // 子节点
    private boolean leaf;       // 是否为叶子节点
    private String type; // null=陷阱代码, "project"=Spring Boot项目
}
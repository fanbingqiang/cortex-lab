package com.cortex.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectInfoDTO {
    // Spring Boot 项目信息
    private String nodeId;          // 知识节点ID
    private String projectName;     // 项目名称
    private String description;     // 项目描述
    private String baseDir;         // 基础目录
    private List<ProjectFileDTO> files; // 项目文件列表
}

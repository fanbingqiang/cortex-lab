package com.cortex.lab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectFileDTO {
    // 项目文件（路径+内容）
    private String path;    // 文件路径
    private String content; // 文件内容
}

package com.cortex.lab.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityTrapDto {
    // 社区陷阱代码 DTO
    private Long id;                // 主键
    private String title;           // 标题
    private String knowledgePoint;  // 知识点
    private String category;        // 分类
    private String javaVersion;     // Java版本
    private String trapCode;        // 陷阱代码
    private String expectedPitfall; // 预期意外现象
    private String correctExplanation; // 正确解释
    private List<String> hints;     // 提示列表
    private Integer difficulty;     // 难度等级
    private String submitter;       // 提交者
    private String status;          // 状态
    private Integer voteCount;      // 投票数
    private LocalDateTime gmtCreate; // 创建时间
}
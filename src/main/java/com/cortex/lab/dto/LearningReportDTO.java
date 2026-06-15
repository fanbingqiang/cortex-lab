package com.cortex.lab.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class LearningReportDTO {
    // 学习报表
    private String reportType;  // 报表类型（WEEKLY/MONTHLY）
    private String periodStart; // 开始日期
    private String periodEnd;   // 结束日期
    // 统计摘要
    private int totalQuestionsAnswered; // 答题总数
    private int totalCorrect;    // 正确数
    private double accuracy;     // 正确率
    private int totalStudyMinutes; // 总学习时长
    private int studyDays;       // 学习天数
    // 细分数据
    private List<Map<String, Object>> dailyStats;        // 每日统计
    private List<Map<String, Object>> categoryBreakdown; // 分类统计
    private List<Map<String, Object>> weakAreasList;     // 薄弱知识点
    private List<Map<String, Object>> progressTrend;     // 进度趋势
    // 建议
    private List<String> recommendations; // 学习建议
    private List<String> focusAreas;      // 重点关注领域
}

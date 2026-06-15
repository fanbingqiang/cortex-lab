package com.cortex.lab.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class UserInfoDTO {
    // 用户信息
    private String userId;
    private String username;
    private String email;
    private String avatar;
    private String role;
    // 学习画像
    private Double totalStudyHours;      // 总学习时长
    private Integer totalQuestionsAnswered; // 总答题数
    private Integer totalCorrect;        // 总正确数
    private Double accuracy;             // 正确率
    private Integer studyStreak;         // 连续学习天数
    private List<String> weakAreas;      // 薄弱领域
    private String preferredDirection;   // 偏好方向
    private String learningGoal;         // 学习目标
    private String skillLevel;           // 技能等级
    // 统计
    private int masteredCount;      // 已掌握题数
    private int pendingReviewCount; // 待复习题数
    private int totalCards;         // 知识卡片总数
}

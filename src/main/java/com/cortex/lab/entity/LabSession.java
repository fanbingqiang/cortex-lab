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
@TableName("lab_session")
public class LabSession {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private Long scenarioId;
    private String userId;
    private String status;
    private Integer attempts;
    private String chatHistory;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

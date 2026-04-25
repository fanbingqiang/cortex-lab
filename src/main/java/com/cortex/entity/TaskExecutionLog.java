package com.cortex.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_execution_log")
public class TaskExecutionLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String nodeId;
    private String agentId;
    private String input;
    private String output;
    private String status;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime gmtCreate;
}

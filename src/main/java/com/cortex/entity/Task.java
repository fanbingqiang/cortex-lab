package com.cortex.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task")
public class Task {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskId;
    private String userId;
    private String title;
    private String description;
    private String status;
    private String taskGraph;
    private String result;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

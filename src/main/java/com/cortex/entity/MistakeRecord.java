package com.cortex.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("mistake_record")
public class MistakeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userId;
    private String keyword;
    private String description;
    private String taskId;
    private LocalDateTime gmtCreate;
}

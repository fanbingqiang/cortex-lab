package com.cortex.lab.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("assistant_conversation")
public class AssistantConversation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private String userId;
    private String title;
    private Integer messageCount;
    private String status;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

package com.cortex.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("agent_metadata")
public class AgentMetadata {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentId;
    private String name;
    private String description;
    private String capabilities;
    private String inputTypes;
    private String outputTypes;
    private Integer priority;
    private Boolean enabled;
    private String promptTemplate;
    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
}

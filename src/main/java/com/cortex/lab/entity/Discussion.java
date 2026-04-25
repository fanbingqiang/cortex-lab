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
@TableName("lab_discussion")
public class Discussion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long questionId;
    private Long parentId;
    private String userId;
    private String content;
    private LocalDateTime gmtCreate;
}

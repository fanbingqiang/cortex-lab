package com.cortex.dto;

import lombok.Data;

import java.util.List;

@Data
public class TaskGraph {
    private String taskId;
    private String title;
    private List<TaskNode> nodes;
}

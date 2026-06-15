package com.cortex.engine;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskExecutionEvent {
    private String taskId;
    private String eventName;
    private Object data;
}

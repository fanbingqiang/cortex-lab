package com.cortex.engine;

import com.alibaba.fastjson2.JSON;
import com.cortex.agent.AgentRegistry;
import com.cortex.dto.TaskGraph;
import com.cortex.dto.TaskNode;
import com.cortex.entity.TaskExecutionLog;
import com.cortex.llm.LlmClient;
import com.cortex.mapper.TaskExecutionLogMapper;
import com.cortex.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CortexTaskExecutor {

    private final AgentRegistry agentRegistry;
    private final LlmClient llmClient;
    private final TaskMapper taskMapper;
    private final TaskExecutionLogMapper executionLogMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> new Thread(r, "task-executor-" + r.hashCode()));

    // 执行任务图，不推事件
    public TaskGraph execute(TaskGraph graph, String userId) {
        return execute(graph, userId, null);
    }

    // 执行任务图，支持事件回调和并行
    public TaskGraph execute(TaskGraph graph, String userId, Consumer<TaskExecutionEvent> eventCallback) {
        Map<String, TaskNode> nodeMap = new ConcurrentHashMap<>();
        graph.getNodes().forEach(node -> nodeMap.put(node.getNodeId(), node));

        // 1. 按依赖层级分组
        Map<Integer, List<TaskNode>> levelGroups = buildLevelGroups(graph);
        List<TaskNode> allNodes = topologicalSort(graph).stream()
                .map(nodeMap::get).collect(Collectors.toList());

        for (TaskNode node : allNodes) {
            node.setStatus("PENDING");
        }

        publishEvent(graph.getTaskId(), "task_started", graph, eventCallback);

        // 2. 逐层执行，同层并行
        for (int level : levelGroups.keySet()) {
            List<TaskNode> levelNodes = levelGroups.get(level);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (TaskNode node : levelNodes) {
                if (!"PENDING".equals(node.getStatus())) continue;

                if (!canExecute(node, nodeMap)) {
                    node.setStatus("SKIPPED");
                    publishEvent(graph.getTaskId(), "node_skipped", node, eventCallback);
                    continue;
                }

                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    executeNode(node, nodeMap, graph, userId, eventCallback);
                }, executor);

                futures.add(future);
            }

            if (!futures.isEmpty()) {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            }

            // 3. 检查本层是否有失败节点，触发重规划
            boolean hasFailed = levelNodes.stream().anyMatch(n -> "FAILED".equals(n.getStatus()));
            if (hasFailed) {
                replanFailedNodes(graph, nodeMap, userId, eventCallback);
            }
        }

        publishEvent(graph.getTaskId(), "task_completed", graph, eventCallback);
        return graph;
    }

    // 执行单个节点
    private void executeNode(TaskNode node, Map<String, TaskNode> nodeMap, TaskGraph graph,
                             String userId, Consumer<TaskExecutionEvent> eventCallback) {
        String input = prepareInput(node, nodeMap);
        node.setInput(input);
        node.setStatus("RUNNING");
        publishEvent(graph.getTaskId(), "node_running", node, eventCallback);

        long startTime = System.currentTimeMillis();
        try {
            String output = agentRegistry.executeAgent(node.getAgentId(), input, userId);
            node.setOutput(output);
            node.setStatus("SUCCESS");
            publishEvent(graph.getTaskId(), "node_completed", node, eventCallback);

            logExecution(graph.getTaskId(), node, input, output, "SUCCESS", null,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            node.setStatus("FAILED");
            node.setErrorMessage(e.getMessage());
            publishEvent(graph.getTaskId(), "node_failed", node, eventCallback);

            log.error("执行任务节点失败: nodeId={}, agentId={}, error={}",
                    node.getNodeId(), node.getAgentId(), e.getMessage());

            logExecution(graph.getTaskId(), node, input, null, "FAILED", e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    // 节点失败后重规划：让LLM决定如何处理
    private void replanFailedNodes(TaskGraph graph, Map<String, TaskNode> nodeMap,
                                   String userId, Consumer<TaskExecutionEvent> eventCallback) {
        List<TaskNode> failedNodes = graph.getNodes().stream()
                .filter(n -> "FAILED".equals(n.getStatus()))
                .collect(Collectors.toList());

        for (TaskNode failed : failedNodes) {
            // 找出依赖失败节点的后续节点
            List<TaskNode> dependents = graph.getNodes().stream()
                    .filter(n -> n.getDependencies() != null && n.getDependencies().contains(failed.getNodeId()))
                    .collect(Collectors.toList());

            // 构建重规划提示
            String prompt = buildReplanPrompt(failed, dependents, graph);

            try {
                String decision = llmClient.chatSimple("你是一个任务规划专家。根据任务执行情况给出处理建议。", prompt);
                log.info("重规划决策: nodeId={}, decision={}", failed.getNodeId(), decision);

                // 解析 LLM 决策：retry / skip / adjust
                if (decision.contains("retry") || decision.contains("重试")) {
                    // 重试：切换 agent 重新执行
                    String newAgentId = extractAgentId(decision);
                    if (newAgentId != null) {
                        failed.setAgentId(newAgentId);
                        failed.setStatus("PENDING");
                        failed.setErrorMessage(null);

                        publishEvent(graph.getTaskId(), "node_retrying", failed, eventCallback);

                        // 重新执行
                        executeNode(failed, nodeMap, graph, userId, eventCallback);
                    } else {
                        markDependentsSkipped(dependents, "上游节点失败");
                    }
                } else {
                    markDependentsSkipped(dependents, "上游节点失败");
                }
            } catch (Exception e) {
                log.warn("重规划失败，跳过依赖节点: {}", e.getMessage());
                markDependentsSkipped(dependents, "重规划失败");
            }
        }
    }

    private String buildReplanPrompt(TaskNode failed, List<TaskNode> dependents, TaskGraph graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("任务执行中节点失败，请给出处理建议。\n\n");
        sb.append("任务总标题：").append(graph.getTitle()).append("\n\n");
        sb.append("失败节点：").append(failed.getTitle()).append("\n");
        sb.append("节点描述：").append(failed.getDescription()).append("\n");
        sb.append("使用的Agent：").append(failed.getAgentId()).append("\n");
        sb.append("错误信息：").append(failed.getErrorMessage()).append("\n\n");

        if (!dependents.isEmpty()) {
            sb.append("依赖此节点的后续任务：\n");
            for (TaskNode dep : dependents) {
                sb.append("- ").append(dep.getTitle()).append(" (").append(dep.getDescription()).append(")\n");
            }
            sb.append("\n");
        }

        sb.append("可用Agent：\n");
        agentRegistry.getAllAgents().forEach(a ->
                sb.append("- ").append(a.getAgentId()).append(": ").append(a.getDescription()).append("\n"));

        sb.append("\n请选择处理方式：\n");
        sb.append("1. retry:<agentId> — 用指定Agent重试失败节点\n");
        sb.append("2. skip — 跳过失败节点及其依赖节点\n");
        sb.append("只输出一行决策，如：retry:code-analyzer 或 skip");
        return sb.toString();
    }

    private void markDependentsSkipped(List<TaskNode> dependents, String reason) {
        for (TaskNode dep : dependents) {
            dep.setStatus("SKIPPED");
            dep.setErrorMessage(reason);
        }
    }

    private String extractAgentId(String decision) {
        String trimmed = decision.trim().toLowerCase();
        if (trimmed.startsWith("retry:")) {
            return trimmed.substring(6).trim();
        }
        if (trimmed.startsWith("retry：")) {
            return trimmed.substring(6).trim();
        }
        return null;
    }

    // 构建依赖层级
    private Map<Integer, List<TaskNode>> buildLevelGroups(TaskGraph graph) {
        Map<String, TaskNode> nodeMap = new HashMap<>();
        graph.getNodes().forEach(node -> nodeMap.put(node.getNodeId(), node));

        Map<String, Integer> levels = new HashMap<>();
        List<String> topoOrder = topologicalSort(graph);

        for (String nodeId : topoOrder) {
            TaskNode node = nodeMap.get(nodeId);
            if (node.getDependencies() == null || node.getDependencies().isEmpty()) {
                levels.put(nodeId, 0);
            } else {
                int maxDepLevel = 0;
                for (String depId : node.getDependencies()) {
                    maxDepLevel = Math.max(maxDepLevel, levels.getOrDefault(depId, -1) + 1);
                }
                levels.put(nodeId, maxDepLevel);
            }
        }

        Map<Integer, List<TaskNode>> groups = new TreeMap<>();
        for (TaskNode node : graph.getNodes()) {
            int level = levels.get(node.getNodeId());
            groups.computeIfAbsent(level, k -> new ArrayList<>()).add(node);
        }
        return groups;
    }

    // 拓扑排序
    private List<String> topologicalSort(TaskGraph graph) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();

        for (TaskNode node : graph.getNodes()) {
            inDegree.putIfAbsent(node.getNodeId(), 0);
            adjacency.putIfAbsent(node.getNodeId(), new ArrayList<>());

            if (node.getDependencies() != null) {
                for (String dep : node.getDependencies()) {
                    adjacency.computeIfAbsent(dep, k -> new ArrayList<>()).add(node.getNodeId());
                    inDegree.merge(node.getNodeId(), 1, Integer::sum);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.offer(entry.getKey());
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);
            for (String neighbor : adjacency.getOrDefault(current, new ArrayList<>())) {
                int newDegree = inDegree.get(neighbor) - 1;
                inDegree.put(neighbor, newDegree);
                if (newDegree == 0) queue.offer(neighbor);
            }
        }
        return result;
    }

    private boolean canExecute(TaskNode node, Map<String, TaskNode> nodeMap) {
        if (node.getDependencies() == null) return true;
        for (String depId : node.getDependencies()) {
            TaskNode depNode = nodeMap.get(depId);
            if (depNode == null || !"SUCCESS".equals(depNode.getStatus())) {
                return false;
            }
        }
        return true;
    }

    private String prepareInput(TaskNode node, Map<String, TaskNode> nodeMap) {
        StringBuilder input = new StringBuilder();
        input.append("任务：").append(node.getTitle()).append("\n");
        input.append("描述：").append(node.getDescription()).append("\n");

        if (node.getDependencies() != null && !node.getDependencies().isEmpty()) {
            input.append("\n前置任务结果：\n");
            for (String depId : node.getDependencies()) {
                TaskNode depNode = nodeMap.get(depId);
                if (depNode != null && depNode.getOutput() != null) {
                    input.append("【").append(depNode.getTitle()).append("】\n");
                    input.append(depNode.getOutput()).append("\n\n");
                }
            }
        }
        return input.toString();
    }

    private void logExecution(String taskId, TaskNode node, String input, String output,
                              String status, String errorMessage, long durationMs) {
        TaskExecutionLog logRecord = new TaskExecutionLog();
        logRecord.setTaskId(taskId);
        logRecord.setNodeId(node.getNodeId());
        logRecord.setAgentId(node.getAgentId());
        logRecord.setInput(input);
        logRecord.setOutput(output);
        logRecord.setStatus(status);
        logRecord.setErrorMessage(errorMessage);
        logRecord.setDurationMs(durationMs);
        logRecord.setGmtCreate(LocalDateTime.now());
        executionLogMapper.insert(logRecord);
    }

    private void publishEvent(String taskId, String eventName, Object data,
                              Consumer<TaskExecutionEvent> callback) {
        if (callback != null) {
            callback.accept(new TaskExecutionEvent(taskId, eventName, data));
        }
    }

    public String aggregateResult(TaskGraph graph) {
        StringBuilder result = new StringBuilder();
        result.append("# ").append(graph.getTitle()).append("\n\n");

        for (TaskNode node : graph.getNodes()) {
            if ("SUCCESS".equals(node.getStatus()) && node.getOutput() != null) {
                result.append("## ").append(node.getTitle()).append("\n");
                result.append(node.getOutput()).append("\n\n");
            }
        }
        return result.toString();
    }
}

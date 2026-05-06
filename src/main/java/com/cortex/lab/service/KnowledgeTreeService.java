package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.lab.dto.KnowledgeNodeDTO;
import com.cortex.lab.dto.ScenarioDto;
import com.cortex.lab.entity.AssistantConfig;
import com.cortex.lab.entity.LabScenario;
import com.cortex.lab.mapper.AssistantConfigMapper;
import com.cortex.lab.mapper.LabScenarioMapper;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeTreeService {

    private final LabScenarioMapper scenarioMapper;
    private final LlmClient llmClient;
    private final AssistantConfigMapper configMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    public List<KnowledgeNodeDTO> getTree() {
        List<KnowledgeNodeDTO> tree = new ArrayList<>();

        // ==================== Java基础 ====================
        tree.add(new KnowledgeNodeDTO("java-basics", "Java基础", "Java核心语法与基础概念", List.of(
            node("java-basics-datatype", "数据类型与包装类", "考察基本类型、自动装箱/拆箱、Integer缓存池机制"),
            node("java-basics-string", "String与常量池", "String不可变性、常量池、new String() vs 字面量"),
            node("java-basics-passing", "值传递与引用传递", "Java方法的参数传递方式，对象引用与值的关系"),
            node("java-basics-equals", "equals与hashCode", "equals和hashCode的约定、重写规则、HashMap key"),
            node("java-basics-final", "final关键字", "final修饰类/方法/变量的含义和限制"),
            node("java-basics-static", "static关键字", "静态变量、静态方法、静态代码块的执行顺序"),
            node("java-basics-inner-class", "内部类", "成员内部类、静态内部类、匿名内部类、Lambda"),
            node("java-basics-interface", "接口与抽象类", "interface与abstract class的区别、default方法")
        ), false));

        // ==================== 面向对象 ====================
        tree.add(new KnowledgeNodeDTO("oop", "面向对象", "面向对象三大特性与设计原则", List.of(
            node("oop-polymorphism", "多态与重载", "运行时多态、编译时多态、方法重写与重载的区别"),
            node("oop-extends", "继承与组合", "继承的优缺点、组合优先于继承、super关键字"),
            node("oop-singleton", "单例模式", "饿汉式、懒汉式、DCL、静态内部类、枚举单例"),
            node("oop-proxy", "代理模式", "静态代理、JDK动态代理、CGLIB代理的原理与区别")
        ), false));

        // ==================== 异常处理 ====================
        tree.add(new KnowledgeNodeDTO("exception", "异常处理", "Java异常机制与最佳实践", List.of(
            node("exception-try-finally", "try-catch-finally", "finally执行时机、return与finally的先后顺序"),
            node("exception-hierarchy", "异常继承体系", "Throwable、Error、Exception、RuntimeException的层次"),
            node("exception-checked", "受检异常与非受检异常", "checked/unchecked异常的区别、处理策略")
        ), false));

        // ==================== 集合框架 ====================
        tree.add(new KnowledgeNodeDTO("collection", "集合框架", "List、Map、Set及并发集合", List.of(
            node("collection-arraylist", "ArrayList与LinkedList", "底层数据结构、扩容机制、增删改查性能对比"),
            node("collection-hashmap", "HashMap原理", "put/get流程、hash计算、扩容树化、1.7 vs 1.8"),
            node("collection-concurrent-hashmap", "ConcurrentHashMap", "分段锁/CAS、并发安全、size()实现"),
            node("collection-treemap", "TreeMap与Comparable", "红黑树结构、自然排序与定制排序、Comparator"),
            node("collection-queue", "Queue与Deque", "BlockingQueue、PriorityQueue、双端队列")
        ), false));

        // ==================== Java并发编程 ====================
        tree.add(new KnowledgeNodeDTO("concurrency", "Java并发编程", "多线程、锁、JUC工具类", List.of(
            node("concurrency-thread", "线程创建与状态", "Thread、Runnable、Callable、线程状态转换"),
            node("concurrency-synchronized", "synchronized原理", "对象头MarkWord、偏向锁、轻量锁、重量锁升级"),
            node("concurrency-volatile", "volatile关键字", "可见性、有序性、禁止指令重排、happens-before"),
            node("concurrency-reentrantlock", "ReentrantLock", "AQS原理、公平/非公平锁、Condition"),
            node("concurrency-threadpool", "线程池", "核心参数、拒绝策略、Executor框架、线程池大小设置"),
            node("concurrency-deadlock", "死锁与活锁", "死锁的必要条件、排查方法、预防策略"),
            node("concurrency-atomic", "Atomic原子类", "CAS原理、ABA问题、AtomicInteger/AtomicReference"),
            node("concurrency-completable-future", "CompletableFuture", "异步编程、thenApply/combine/allOf的用法")
        ), false));

        // ==================== JVM ====================
        tree.add(new KnowledgeNodeDTO("jvm", "JVM", "内存模型、GC、类加载", List.of(
            node("jvm-memory", "JVM内存区域", "堆、栈、方法区、程序计数器、直接内存"),
            node("jvm-gc", "垃圾回收机制", "可达性分析、GC Root、Minor/Major/Full GC"),
            node("jvm-collector", "垃圾收集器", "Serial、Parallel、CMS、G1、ZGC的特点与选择"),
            node("jvm-oom", "OOM分析", "堆溢出、栈溢出、元空间溢出、MAT分析"),
            node("jvm-classloader", "类加载机制", "双亲委派模型、加载/连接/初始化、自定义类加载器"),
            node("jvm-tuning", "JVM调优", "常用参数、GC日志分析、性能监控工具")
        ), false));

        // ==================== Spring框架 ====================
        tree.add(new KnowledgeNodeDTO("spring", "Spring框架", "IoC、AOP、MVC、Boot", List.of(
            node("spring-ioc", "IoC容器", "Bean生命周期、依赖注入方式、循环依赖解决"),
            node("spring-aop", "AOP原理", "JDK代理 vs CGLIB、@Aspect、切面执行顺序"),
            node("spring-transaction", "事务管理", "@Transactional传播行为、隔离级别、失效场景"),
            node("spring-mvc", "Spring MVC流程", "DispatcherServlet、HandlerMapping、拦截器"),
            node("spring-boot", "Spring Boot核心", "自动配置原理、@EnableAutoConfiguration、Starter机制")
        ), false));

        // ==================== 数据库与SQL ====================
        tree.add(new KnowledgeNodeDTO("database", "数据库与SQL", "MySQL、索引、事务、MyBatis", List.of(
            node("database-mysql-arch", "MySQL架构", "Server层与存储引擎、SQL执行流程"),
            node("database-index", "索引优化", "B+树结构、聚簇索引、覆盖索引、最左前缀法则"),
            node("database-transaction", "事务与隔离级别", "ACID、MVCC、脏读/不可重复读/幻读"),
            node("database-lock", "MySQL锁", "行锁、间隙锁、临键锁、意向锁、死锁"),
            node("database-sql-optimize", "SQL优化", "慢查询分析、explain解读、索引下推"),
            node("database-mybatis", "MyBatis", "一级/二级缓存、#{} vs ${}、插件原理")
        ), false));

        // ==================== 设计模式 ====================
        tree.add(new KnowledgeNodeDTO("design-pattern", "设计模式", "常用设计模式与应用场景", List.of(
            node("pattern-factory", "工厂模式", "简单工厂、工厂方法、抽象工厂的区别与使用场景"),
            node("pattern-builder", "建造者模式", "Builder模式、链式调用、与构造器的选择"),
            node("pattern-strategy", "策略模式", "策略模式的结构、与if-else的对比、Spring中的应用"),
            node("pattern-observer", "观察者模式", "发布订阅机制、事件驱动、Spring Event"),
            node("pattern-decorator", "装饰器模式", "增强已有功能、IO流中的装饰器应用")
        ), false));

        // ==================== 网络与IO ====================
        tree.add(new KnowledgeNodeDTO("net-io", "网络与IO", "IO模型、Netty、HTTP", List.of(
            node("io-bio-nio-aio", "BIO/NIO/AIO", "阻塞/非阻塞/异步IO的区别与适用场景"),
            node("io-netty", "Netty核心", "Reactor模型、EventLoop、ChannelPipeline"),
            node("io-zerocopy", "零拷贝", "传统IO vs mmap vs sendfile、Java实现")
        ), false));

        // ==================== 分布式 ====================
        tree.add(new KnowledgeNodeDTO("distributed", "分布式系统", "理论、缓存、消息队列", List.of(
            node("distributed-cap", "CAP与BASE理论", "一致性、可用性、分区容错性的权衡"),
            node("distributed-redis", "Redis", "数据结构、过期策略、持久化、分布式锁、缓存穿透/击穿/雪崩"),
            node("distributed-mq", "消息队列", "RocketMQ/Kafka架构、顺序消息、事务消息、幂等消费"),
            node("distributed-transaction", "分布式事务", "2PC、TCC、Seata、可靠消息最终一致性")
        ), false));

        return tree;
    }

    public void toggleMastered(String nodeId, String userId, boolean mastered) {
        try {
            String key = "tree_mastered_" + userId + "_" + nodeId;
            List<AssistantConfig> existing = configMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssistantConfig>()
                    .eq(AssistantConfig::getConfigKey, key)
                    .last("LIMIT 1")
            );
            if (!existing.isEmpty()) {
                AssistantConfig c = existing.get(0);
                c.setConfigValue(String.valueOf(mastered));
                c.setGmtModified(LocalDateTime.now());
                configMapper.updateById(c);
            } else {
                AssistantConfig c = new AssistantConfig();
                c.setConfigKey(key);
                c.setConfigValue(String.valueOf(mastered));
                c.setConfigType("boolean");
                c.setGmtCreate(LocalDateTime.now());
                c.setGmtModified(LocalDateTime.now());
                configMapper.insert(c);
            }
        } catch (Exception e) {
            log.warn("保存节点掌握状态失败: {}", e.getMessage());
        }
    }

    public List<String> getMasteredNodeIds(String userId) {
        try {
            String prefix = "tree_mastered_" + userId + "_";
            List<AssistantConfig> all = configMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssistantConfig>()
                    .likeRight(AssistantConfig::getConfigKey, prefix)
            );
            return all.stream()
                .filter(c -> "true".equals(c.getConfigValue()))
                .map(c -> c.getConfigKey().substring(prefix.length()))
                .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            log.warn("获取节点掌握状态失败: {}", e.getMessage());
            return List.of();
        }
    }

    private static KnowledgeNodeDTO node(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true);
    }

    private static final Map<String, String> SCENARIO_MAP = Map.ofEntries(
        Map.entry("java-basics-datatype", "为什么 Integer 用 == 比较 100 是 true，200 却是 false？"),
        Map.entry("java-basics-string", "为什么 new String(\"hello\") 用 == 比较结果是 false？"),
        Map.entry("java-basics-passing", "为什么方法里改了 List，外面的 List 也变了？"),
        Map.entry("java-basics-equals", "HashMap 用 Person 做 key，为什么 get 不到值？"),
        Map.entry("exception-try-finally", "为什么 try 里 return 了，finally 还会执行？"),
        Map.entry("exception-hierarchy", "异常捕获：多个 catch 的顺序有讲究吗？")
    );

    public ScenarioDto generateForNode(String nodeId) {
        List<KnowledgeNodeDTO> tree = getTree();
        KnowledgeNodeDTO found = findNode(tree, nodeId);
        if (found == null || !found.isLeaf()) {
            throw new RuntimeException("未找到知识点: " + nodeId);
        }

        String knowledgePoint = found.getName();
        String description = found.getDescription();

        // Try direct mapping to existing scenario
        String mappedKnowledgePoint = SCENARIO_MAP.get(nodeId);
        if (mappedKnowledgePoint != null) {
            LabScenario mapped = scenarioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                    .eq(LabScenario::getKnowledgePoint, mappedKnowledgePoint)
            );
            if (mapped != null) {
                log.info("知识点 [{}] -> 映射到已有场景 [{}]", nodeId, mappedKnowledgePoint);
                return toDto(mapped);
            }
        }

        // Try exact name match
        LabScenario existing = scenarioMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                .eq(LabScenario::getKnowledgePoint, knowledgePoint)
        );
        if (existing != null) {
            log.info("知识点 [{}] 已有缓存场景，直接返回", knowledgePoint);
            return toDto(existing);
        }

        // Try to generate via LLM (user-configured API key or env)
        try {
            String userApiKey = getUserApiKey();
            String result;
            if (userApiKey != null) {
                result = callDeepSeekApi(userApiKey, buildPrompt(knowledgePoint, description));
            } else {
                result = llmClient.chatSimple(buildPrompt(knowledgePoint, description));
            }
            String cleaned = cleanJson(result);
            ScenarioDto dto = JSON.parseObject(cleaned, ScenarioDto.class);

            LabScenario entity = new LabScenario();
            entity.setKnowledgePoint(knowledgePoint);
            entity.setCategory(getCategoryForNode(nodeId));
            entity.setTrapCode(dto.getTrapCode());
            entity.setExpectedPitfall(dto.getExpectedPitfall());
            entity.setCorrectExplanation(dto.getCorrectExplanation());
            entity.setHints(JSON.toJSONString(dto.getHints()));
            entity.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : 2);
            entity.setGmtCreate(LocalDateTime.now());
            scenarioMapper.insert(entity);

            dto.setId(entity.getId());
            return dto;
        } catch (Exception e) {
            log.warn("LLM 生成失败: {}", e.getMessage());
            throw new RuntimeException("AI 生成代码失败，请在右侧面板点击 ⚙️ 配置你的 DeepSeek API Key。" +
                "配置后重新点击知识点即可自动生成带陷阱的代码。");
        }
    }

    private String buildPrompt(String knowledgePoint, String description) {
        return """
    你是一个 Java 编程教学专家。针对以下知识点，生成一段带有陷阱的 Java 代码。

    知识点: %s
    描述: %s

    请返回 JSON 格式（不要 markdown 标记）：
    {
      "trapCode": "完整的可编译运行的 Java 代码（必须包含 public class 和 main 方法），代码表面正常但暗藏陷阱，通过 print 输出让学习者发现异常",
      "expectedPitfall": "预期的意外现象（一句话描述学习者会看到什么）",
      "correctExplanation": "正确原理解释（200字以内，讲清楚为什么）",
      "hints": ["提示1（引导观察）", "提示2（引导思考）", "提示3（暗示方向）"],
      "difficulty": 2
    }

    要求：
    1. 代码必须完整、可编译、可运行，包含 main 方法
    2. 陷阱效果通过 System.out.println 输出体现
    3. 代码看起来正常，初学者看不出问题
    4. 提示要循序渐进，不直接给出答案
    """.formatted(knowledgePoint, description);
    }

    private KnowledgeNodeDTO findNode(List<KnowledgeNodeDTO> nodes, String id) {
        for (KnowledgeNodeDTO node : nodes) {
            if (id.equals(node.getId())) return node;
            if (node.getChildren() != null) {
                KnowledgeNodeDTO found = findNode(node.getChildren(), id);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String getCategoryForNode(String nodeId) {
        for (var cat : getTree()) {
            if (cat.getChildren() != null) {
                for (var child : cat.getChildren()) {
                    if (child.getId().equals(nodeId)) return cat.getName();
                }
            }
        }
        return "Java";
    }

    private String cleanJson(String raw) {
        if (raw == null) return "{}";
        raw = raw.trim();
        if (raw.startsWith("```")) {
            int start = raw.indexOf('\n');
            int end = raw.lastIndexOf("```");
            if (start > 0 && end > start) {
                raw = raw.substring(start, end).trim();
            }
        }
        return raw;
    }

    private ScenarioDto toDto(LabScenario entity) {
        ScenarioDto dto = new ScenarioDto();
        dto.setId(entity.getId());
        dto.setKnowledgePoint(entity.getKnowledgePoint());
        dto.setCategory(entity.getCategory());
        dto.setTrapCode(entity.getTrapCode());
        dto.setExpectedPitfall(entity.getExpectedPitfall());
        dto.setCorrectExplanation(entity.getCorrectExplanation());
        dto.setHints(JSON.parseArray(entity.getHints(), String.class));
        dto.setDifficulty(entity.getDifficulty());
        return dto;
    }

    private String getUserApiKey() {
        try {
            List<AssistantConfig> configs = configMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AssistantConfig>()
                    .eq(AssistantConfig::getConfigKey, "deepseek_api_key")
                    .last("LIMIT 1")
            );
            if (!configs.isEmpty()) {
                String key = configs.get(0).getConfigValue();
                if (key != null && !key.isBlank() && !key.equals("your-api-key-here")) {
                    return key;
                }
            }
        } catch (Exception e) {
            log.warn("读取API Key配置失败: {}", e.getMessage());
        }
        return null;
    }

    private String callDeepSeekApi(String apiKey, String prompt) {
        String url = "https://api.deepseek.com/chat/completions";
        String jsonBody = "{\n" +
            "  \"model\": \"deepseek-chat\",\n" +
            "  \"messages\": [{\"role\": \"user\", \"content\": " + JSON.toJSONString(prompt) + "}],\n" +
            "  \"temperature\": 0.7,\n" +
            "  \"max_tokens\": 4096\n" +
            "}";

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
            .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new RuntimeException("API调用失败: " + response.code() + " - " + errorBody);
            }
            String responseBody = response.body().string();
            var jsonResponse = JSON.parseObject(responseBody);
            return jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content");
        } catch (IOException e) {
            throw new RuntimeException("API调用异常: " + e.getMessage(), e);
        }
    }
}
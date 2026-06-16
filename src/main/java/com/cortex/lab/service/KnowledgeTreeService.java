package com.cortex.lab.service;

import com.alibaba.fastjson2.JSON;
import com.cortex.lab.dto.KnowledgeNodeDTO;
import com.cortex.lab.dto.ProjectInfoDTO;
import com.cortex.lab.dto.ScenarioDto;
import com.cortex.config.LlmConfigResolver;
import com.cortex.lab.entity.AssistantConfig;
import com.cortex.lab.entity.LabScenario;
import com.cortex.lab.entity.QuestionBank;
import com.cortex.lab.mapper.AssistantConfigMapper;
import com.cortex.lab.mapper.LabScenarioMapper;
import com.cortex.lab.mapper.QuestionBankMapper;
import com.cortex.llm.LlmClient;
import com.cortex.llm.LlmRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeTreeService {

    private final LabScenarioMapper scenarioMapper;
    private final LlmClient llmClient;
    private final AssistantConfigMapper configMapper;
    private final LlmConfigResolver llmConfigResolver;
    private final SpringProjectService springProjectService;
    private final QuestionBankMapper questionBankMapper;
    private final KnowledgeCardService knowledgeCardService;

    private final ExecutorService cardGeneratorExecutor = Executors.newSingleThreadExecutor(r ->
        new Thread(r, "card-generator-" + r.hashCode()));

    /** 内存缓存：nodeId → 已生成内容（DB 为持久层） */
    private final ConcurrentHashMap<String, ScenarioDto> scenarioCache = new ConcurrentHashMap<>();

    // ======================== 知识树构建 ========================

    public List<KnowledgeNodeDTO> getTree() {
        List<KnowledgeNodeDTO> tree = new ArrayList<>();

        // ==================== 1. Java基础 ====================
        tree.add(new KnowledgeNodeDTO("java-basics", "Java基础", "Java核心语法与基础概念", List.of(
            node("java-basics-datatype", "数据类型与包装类", "基本类型、自动装箱/拆箱、Integer缓存池"),
            node("java-basics-string", "String与常量池", "不可变性、常量池、intern()、new String vs 字面量"),
            node("java-basics-passing", "值传递与引用传递", "Java只有值传递，对象引用在栈上的行为"),
            node("java-basics-equals", "equals与hashCode", "约定、重写规则、HashMap key必须同时重写"),
            node("java-basics-final", "final关键字", "修饰类/方法/变量/引用的语义和限制"),
            node("java-basics-static", "static关键字", "静态变量/方法/代码块执行顺序、静态导入"),
            node("java-basics-inner-class", "内部类", "成员/静态/匿名/局部内部类、编译后字节码"),
            node("java-basics-interface", "接口与抽象类", "区别、default/static方法、函数式接口@FunctionalInterface"),
            node("java-basics-generics", "泛型与类型擦除", "擦除机制、通配符 extends/super、桥接方法"),
            node("java-basics-reflection", "反射机制", "Class对象、Method/Field/Constructor、setAccessible、性能"),
            node("java-basics-annotation", "注解原理", "元注解、@Retention/@Target、APT、运行时/源码注解"),
            node("java-basics-lambda", "Lambda与函数式接口", "invokedynamic、方法引用::、变量捕获effectively final"),
            node("java-basics-stream", "Stream API", "惰性求值、中间操作/终结操作、并行流、Collectors"),
            node("java-basics-optional", "Optional使用", "正确使用姿势、orElse vs orElseGet、避免isPresent-get"),
            node("java-basics-serial", "序列化机制", "Serializable/serialVersionUID、transient、序列化破坏单例"),
            node("java-basics-io", "IO流体系", "字节流/字符流、装饰器模式在IO中的应用、缓冲流"),
            node("java-basics-enum", "枚举最佳实践", "枚举实现单例、枚举中定义方法、EnumMap/EnumSet"),
            node("java-basics-bigdecimal", "BigDecimal与浮点数", "double精度丢失、BigDecimal构造方式、舍入模式"),
            node("java-basics-datetime", "日期时间API", "Date vs LocalDateTime、SimpleDateFormat线程不安全、时区"),
            node("java-basics-copy", "深拷贝与浅拷贝", "clone()、拷贝构造器、序列化深拷贝、Apache BeanUtils")
        ), false, null));

        // ==================== 2. 面向对象 ====================
        tree.add(new KnowledgeNodeDTO("oop", "面向对象", "OOP三大特性与SOLID设计原则", List.of(
            node("oop-encapsulation", "封装", "访问控制修饰符、getter/setter、数据隐藏"),
            node("oop-extends", "继承", "super调用、继承链方法查找、菱形问题"),
            node("oop-polymorphism", "多态与重载", "运行时多态、编译时多态、重写与重载的区别"),
            node("oop-abstraction", "抽象", "抽象类 vs 接口、面向接口编程"),
            node("oop-composition", "组合优于继承", "组合与委托、装饰器模式对比继承"),
            node("oop-singleton", "单例模式", "饿汉/懒汉/DCL/静态内部类/枚举，反序列化破坏"),
            node("oop-proxy", "代理模式", "静态代理、JDK动态代理（必须接口）、CGLIB继承代理"),
            node("oop-solid-single", "单一职责原则", "类/方法只负责一个职责，判断标准和拆分时机"),
            conceptNode("oop-solid-open", "开闭原则", "对扩展开放、对修改关闭，模板方法+策略实现"),
            conceptNode("oop-solid-liskov", "里氏替换原则", "子类不能削弱父类前置条件、不能增强后置条件"),
            conceptNode("oop-solid-interface-seg", "接口隔离原则", "大接口拆小接口、避免fat interface"),
            conceptNode("oop-solid-di", "依赖反转原则", "依赖抽象而非具体、IoC容器与依赖注入")
        ), false, null));

        // ==================== 3. 异常处理 ====================
        tree.add(new KnowledgeNodeDTO("exception", "异常处理", "Java异常机制与最佳实践", List.of(
            node("exception-hierarchy", "异常继承体系", "Throwable、Error、Exception、RuntimeException层次"),
            node("exception-checked", "受检与非受检异常", "checked/unchecked设计哲学、何时用哪种"),
            node("exception-try-finally", "try-catch-finally", "finally执行时机、return与finally的先后"),
            node("exception-try-with-resources", "try-with-resources", "AutoCloseable接口、资源关闭顺序"),
            node("exception-suppressed", "Suppressed抑制异常", "try-with-resources中多个异常的处理"),
            node("exception-custom", "自定义异常", "继承RuntimeException vs Exception、异常链"),
            node("exception-performance", "异常性能开销", "异常创建的成本、栈帧填充、flow control反模式"),
            node("exception-best-practice", "异常最佳实践", "不要吞异常、精准异常类型、记录上下文")
        ), false, null));

        // ==================== 4. 集合框架 ====================
        tree.add(new KnowledgeNodeDTO("collection", "集合框架", "List/Set/Map及并发集合", List.of(
            node("collection-arraylist", "ArrayList与LinkedList", "底层数组/链表、扩容机制、增删改查性能"),
            node("collection-hashmap", "HashMap原理", "put/get流程、hash扰动、扩容树化、1.7 vs 1.8"),
            node("collection-concurrent-hashmap", "ConcurrentHashMap", "CAS+synchronized分段、size()、弱一致性迭代"),
            node("collection-treemap", "TreeMap与Comparable", "红黑树结构、自然/定制排序、Comparator"),
            node("collection-queue", "Queue与Deque", "BlockingQueue、PriorityQueue、Deque双端"),
            node("collection-hashset", "HashSet与LinkedHashSet", "底层HashMap、插入顺序与访问顺序"),
            node("collection-treeset", "TreeSet", "底层TreeMap、排序一致性和equals关系"),
            node("collection-failfast", "fail-fast机制", "modCount并发修改检测、CopyOnWriteArrayList"),
            node("collection-collections", "Collections工具类", "unmodifiable/synchronized/checked/empty系、sort/binarySearch"),
            node("collection-arrays", "Arrays工具类", "asList陷阱、deepEquals、parallelSort、stream转换"),
            node("collection-sublist", "subList视图陷阱", "SubList是视图而非快照、结构性修改ConcurrentModification"),
            node("collection-aslist", "Arrays.asList陷阱", "返回固定大小列表、不能add/remove、基本类型数组问题"),
            node("collection-iterable", "Iterable与Iterator", "增强for循环语法糖、remove方法、ListIterator"),
            node("collection-enum-map", "EnumMap与EnumSet", "枚举专用集合的性能优势、位向量实现")
        ), false, null));

        // ==================== 5. Java并发编程 ====================
        tree.add(new KnowledgeNodeDTO("concurrency", "Java并发编程", "多线程、锁、JUC工具类", List.of(
            node("concurrency-thread", "线程创建与状态", "Thread/Runnable/Callable、6种状态转换"),
            node("concurrency-synchronized", "synchronized原理", "对象头MarkWord、偏向锁/轻量锁/重量锁升级"),
            node("concurrency-volatile", "volatile关键字", "可见性、有序性、禁止指令重排、happens-before"),
            node("concurrency-reentrantlock", "ReentrantLock", "AQS原理、公平/非公平、Condition条件等待"),
            node("concurrency-readwritelock", "ReentrantReadWriteLock", "读读共享、读写互斥、锁降级"),
            node("concurrency-stampedlock", "StampedLock", "乐观读、悲观读、写锁、锁转换对比"),
            node("concurrency-threadpool", "线程池", "核心参数、拒绝策略、ThreadPoolExecutor、线程池大小设置"),
            node("concurrency-deadlock", "死锁与活锁", "必要条件、jstack排查、预防策略、活锁/饥饿"),
            node("concurrency-atomic", "Atomic原子类", "CAS原理、ABA问题、AtomicInteger/LongAdder"),
            node("concurrency-completable-future", "CompletableFuture", "异步编排、thenApply/combine/allOf/exceptionally"),
            node("concurrency-threadlocal", "ThreadLocal", "线程局部变量、内存泄漏、InheritableThreadLocal"),
            node("concurrency-forkjoin", "Fork/Join框架", "工作窃取算法、ForkJoinPool、RecursiveTask"),
            node("concurrency-locksupport", "LockSupport", "park/unpark、与wait/notify对比、许可机制"),
            node("concurrency-semaphore", "Semaphore同步工具", "Semaphore/CyclicBarrier/CountDownLatch使用场景"),
            node("concurrency-blockingqueue", "BlockingQueue", "Array/Linked/Synchronous/DelayQueue实现"),
            node("concurrency-phaser", "Phaser阶段器", "Reusable barrier、动态注册、onAdvance回调"),
            node("concurrency-false-sharing", "伪共享", "缓存行失效、@Contended注解、padding对齐"),
            node("concurrency-aqs", "AQS抽象队列同步器", "CLH队列变体、独占/共享模式、tryAcquire模板"),
            node("concurrency-cpu-cache", "CPU缓存与内存屏障", "MESI协议、写缓冲、storeLoad屏障、JMM实现")
        ), false, null));

        // ==================== 6. JVM ====================
        tree.add(new KnowledgeNodeDTO("jvm", "JVM", "内存模型、GC、类加载与调优", List.of(
            node("jvm-memory", "JVM内存区域", "堆/栈/方法区/程序计数器/直接内存、JDK8元空间变化"),
            node("jvm-gc", "垃圾回收机制", "可达性分析、GC Root、Minor/Major/Full GC、Stop-the-World"),
            node("jvm-collector", "垃圾收集器", "Serial/Parallel/CMS/G1/ZGC选择与调优"),
            node("jvm-oom", "OOM分析", "堆溢出/栈溢出/元空间溢出/OOM in Direct Buffer"),
            node("jvm-classloader", "类加载机制", "双亲委派模型、加载/连接/初始化、打破双亲委派"),
            node("jvm-tuning", "JVM调优", "常用参数、GC日志分析、G1调优案例"),
            node("jvm-reference", "四种引用类型", "强/软/弱/虚引用、ReferenceQueue、PhantomReference"),
            node("jvm-string-intern", "String.intern池", "常量池位置变化、intern()行为、字符串去重"),
            node("jvm-escape", "逃逸分析", "栈上分配、同步消除、标量替换、-XX:+DoEscapeAnalysis"),
            node("jvm-off-heap", "堆外内存", "DirectByteBuffer、Unsafe.allocateMemory、Netty池化管理"),
            node("jvm-jit", "JIT编译优化", "解释执行 vs 编译执行、C1/C2编译器、分层编译"),
            node("jvm-tool", "JDK诊断工具", "jstack线程栈、jmap堆dump、jstat GC统计、Arthas"),
            node("jvm-bytecode", "字节码结构", "ClassFile格式、常量池、invokevirtual/invokedynamic"),
            node("jvm-compressed-oops", "压缩普通对象指针", "CompressedOops、32位指针对齐、>32GB堆关闭"),
            node("jvm-g1-region", "G1垃圾收集器", "Region划分、RSet、SATB、Mixed GC、Humongous分配")
        ), false, null));

        // ==================== 7. Spring框架 ====================
        tree.add(new KnowledgeNodeDTO("spring", "Spring框架", "IoC/AOP/MVC/Boot/Cloud", List.of(
            projectNode("spring-ioc", "IoC容器", "Bean生命周期、DI注入方式 — Spring Boot Maven 项目"),
            projectNode("spring-aop", "AOP原理", "JDK/CGLIB、@Aspect — Spring Boot AOP 项目"),
            projectNode("spring-mvc", "Spring MVC流程", "DispatcherServlet、HandlerMapping — Spring Boot Web 项目"),
            projectNode("spring-security", "Spring Security", "认证授权流程、JWT整合 — Spring Security 项目"),
            projectNode("spring-jpa", "Spring Data JPA", "实体映射、关联查询、N+1问题 — Spring JPA 项目"),
            projectNode("spring-mybatis-plus", "MyBatis-Plus", "自动映射、Wrapper、分页插件 — MyBatis-Plus 项目"),
            projectNode("spring-cache-redis", "Spring Cache + Redis", "缓存注解、Redis分布式锁 — Cache Redis 项目"),
            projectNode("spring-websocket", "WebSocket", "实时通信、Stomp协议、广播 — WebSocket 项目"),
            projectNode("spring-rabbitmq", "RabbitMQ集成", "消息确认、死信队列、延迟队列 — RabbitMQ 项目"),
            node("spring-transaction", "事务管理", "@Transactional传播行为、隔离级别、失效场景"),
            node("spring-boot", "Spring Boot核心", "自动配置原理、@EnableAutoConfiguration、Starter"),
            node("spring-event", "Spring事件机制", "ApplicationEvent、@EventListener、异步事件"),
            node("spring-async", "@Async异步", "@EnableAsync、ThreadPoolTaskExecutor配置、CompletableFuture返回"),
            node("spring-trap-transaction", "事务失效场景", "@Transactional私有方法/自调用/异常被捕获/传播属性"),
            node("spring-trap-circular", "循环依赖", "构造器注入循环依赖、三级缓存、@Lazy"),
            node("spring-trap-aop", "AOP失效场景", "自调用失效、private/protected不代理、final方法"),
            node("spring-trap-scope", "Bean作用域陷阱", "Singleton注入Prototype失效、@Scope和代理模式"),
            node("spring-interceptor", "拦截器与过滤器", "Filter vs HandlerInterceptor、@ControllerAdvice")
        ), false, null));

        // ==================== 8. 数据库与SQL ====================
        tree.add(new KnowledgeNodeDTO("database", "数据库与SQL", "MySQL、索引、事务、SQL优化", List.of(
            node("database-mysql-arch", "MySQL架构", "Server层/存储引擎、SQL执行流程、连接器/分析器/优化器"),
            node("database-sql-order", "SQL执行顺序", "FROM→ON→JOIN→WHERE→GROUP BY→HAVING→SELECT→ORDER BY→LIMIT"),
            node("database-index", "索引优化", "B+树结构、聚簇/二级索引、覆盖索引、最左前缀"),
            node("database-transaction", "事务与隔离级别", "ACID、MVCC、undo log、RC/RR/可串行化"),
            node("database-lock", "MySQL锁", "行锁/间隙锁/临键锁/意向锁/死锁排查"),
            node("database-sql-optimize", "SQL优化", "慢查询分析、Explain解读、索引下推、ICP"),
            node("database-count", "COUNT查询", "count(*) vs count(1) vs count(列)、MyISAM计数缓存"),
            node("database-join-optimize", "JOIN优化", "Nested Loop Join/Hash Join、驱动表选择、join buffer"),
            node("database-deep-paging", "深分页优化", "limit offset问题、延迟关联、子查询分页、游标分页"),
            node("database-normal-form", "数据库三大范式", "1NF/2NF/3NF/BCNF、反范式设计权衡"),
            node("database-charset", "字符集与乱码", "UTF-8 vs utf8mb4、emoji存储、连接编码配置"),
            node("database-on-duplicate", "主键冲突处理", "ON DUPLICATE KEY UPDATE、REPLACE INTO、INSERT IGNORE"),
            node("database-lock-pess-opti", "悲观锁与乐观锁", "select for update、version/CAS实现乐观锁"),
            node("database-sharding", "分库分表", "垂直/水平拆分、ShardingSphere、全局表/ER表"),
            node("database-read-write", "主从复制与读写分离", "binlog复制、异步/半同步/全同步、延迟问题"),
            node("database-migration", "数据库迁移", "Flyway/Liquibase、Online DDL、大表改结构风险")
        ), false, null));

        // ==================== 9. MyBatis ====================
        tree.add(new KnowledgeNodeDTO("mybatis", "MyBatis", "ORM框架、动态SQL、缓存与插件", List.of(
            node("mybatis-dynamic-sql", "动态SQL", "if/choose/when/foreach/where/set/trim标签"),
            node("mybatis-sql-injection", "#{}与${}注入", "#{}预编译防注入、${}排序字段拼接场景"),
            node("mybatis-cache", "MyBatis缓存", "一级缓存SQLSession、二级缓存namespace、自定义缓存"),
            node("mybatis-plugin", "插件原理", "Interceptor接口、四大对象、PageHelper分页原理"),
            node("mybatis-result", "结果映射", "ResultType vs ResultMap、association/collection嵌套"),
            node("mybatis-nested-query", "嵌套查询与N+1", "延迟加载、N+1问题、lazyLoadingEnabled配置"),
            node("mybatis-generator", "代码生成器", "MyBatis Generator配置、通用Mapper、tk.mybatis")
        ), false, null));

        // ==================== 10. 设计模式 ====================
        tree.add(new KnowledgeNodeDTO("design-pattern", "设计模式", "常用设计模式与应用场景", List.of(
            node("pattern-factory", "工厂模式", "简单工厂/工厂方法/抽象工厂、Spring BeanFactory"),
            node("pattern-builder", "建造者模式", "Builder链式调用、Lombok @Builder"),
            node("pattern-strategy", "策略模式", "策略模式结构、Spring ApplicationContext获取策略"),
            node("pattern-observer", "观察者模式", "发布-订阅、Spring Event、Guava EventBus"),
            node("pattern-decorator", "装饰器模式", "增强已有功能、IO流装饰器"),
            node("pattern-chain", "责任链模式", "FilterChain、Netty Pipeline、Dubbo Filter"),
            node("pattern-template", "模板方法模式", "抽象类定义骨架、子类实现步骤"),
            node("pattern-adapter", "适配器模式", "类适配器/对象适配器、Spring HandlerAdapter"),
            node("pattern-flyweight", "享元模式", "Integer缓存池、String常量池、线程池复用"),
            node("pattern-facade", "门面模式", "统一接口封装、降级复杂度"),
            node("pattern-mediator", "中介者模式", "对象间解耦、MVC中的Controller职责"),
            node("pattern-memento", "备忘录模式", "状态保存与恢复、undo/redo机制")
        ), false, null));

        // ==================== 11. 网络与IO ====================
        tree.add(new KnowledgeNodeDTO("net-io", "网络与IO", "TCP/IP、HTTP、Netty", List.of(
            node("io-bio-nio-aio", "BIO/NIO/AIO", "阻塞/非阻塞/异步、Selector/Channel/ByteBuffer"),
            node("io-netty", "Netty核心", "Reactor模型、EventLoop、ChannelPipeline"),
            node("io-zerocopy", "零拷贝", "传统IO vs mmap vs sendfile、FileChannel.transferTo"),
            conceptNode("net-tcp-handshake", "TCP三次握手与四次挥手", "状态变迁、SYN Flood、半连接/全连接队列"),
            conceptNode("net-timewait", "TIME_WAIT问题", "2MSL、大量TIME_WAIT导致端口耗尽、SO_REUSEADDR"),
            conceptNode("net-keepalive", "HTTP持久连接", "Keep-Alive、Connection:close、HTTP/2多路复用"),
            conceptNode("net-timeout", "连接与读取超时", "connectTimeout/readTimeout、TCP超时参数"),
            node("net-websocket", "WebSocket", "握手升级、帧协议、与HTTP长轮询对比"),
            conceptNode("net-tls", "HTTPS/TLS", "SSL/TLS握手、证书链、RSA/ECDHE"),
            conceptNode("net-dns", "DNS解析", "递归/迭代查询、缓存、DNS劫持")
        ), false, null));

        // ==================== 12. 分布式系统 ====================
        tree.add(new KnowledgeNodeDTO("distributed", "分布式系统", "理论、中间件、高可用", List.of(
            conceptNode("distributed-cap", "CAP与BASE理论", "一致性/可用性/分区容错性权衡、AP vs CP"),
            node("distributed-redis", "Redis", "数据结构/过期/持久化RDB-AOF/分布式锁Redisson"),
            node("distributed-mq", "消息队列", "RocketMQ/Kafka架构、顺序消息/事务消息/幂等消费"),
            node("distributed-transaction", "分布式事务", "2PC/TCC/Seata/可靠消息最终一致性"),
            node("distributed-discovery", "服务注册与发现", "Nacos/Eureka/ZooKeeper、健康检查、CP/AP权衡"),
            node("distributed-config", "配置中心", "Nacos Config/Apollo/Spring Cloud Config、动态刷新"),
            node("distributed-gateway", "API网关", "Spring Cloud Gateway/路由/过滤器/限流"),
            node("distributed-sentinel", "限流熔断降级", "Sentinel/Hystrix、滑动窗口、漏桶/令牌桶"),
            node("distributed-tracing", "链路追踪", "SkyWalking/Sleuth+Zipkin、TraceId/SpanId"),
            node("distributed-es", "ElasticSearch", "倒排索引、分词器、查询DSL、集群分片"),
            node("distributed-id", "分布式ID", "Snowflake算法变种、Leaf/美团/uid-generator"),
            conceptNode("distributed-raft", "Raft一致性算法", "Leader选举、日志复制、脑裂防止"),
            node("distributed-load-balance", "负载均衡", "轮询/加权/最小连接/一致性Hash、Spring Cloud LoadBalancer"),
            node("distributed-session", "分布式Session", "Session同步/粘滞/Redis统一存储/JWT无状态"),
            node("distributed-interface-idempotent", "接口幂等", "唯一键防重/状态机/Token/乐观锁")
        ), false, null));

        // ==================== 13. Linux运维 ====================
        tree.add(new KnowledgeNodeDTO("linux", "Linux运维", "常用命令、系统排查、Java故障诊断", List.of(
            commandNode("linux-command", "常用命令", "find/grep/sed/awk/top/vmstat/du/df/lsof"),
            commandNode("linux-process", "进程管理", "ps/pstree/kill/nohup/systemd、OOM Killer"),
            commandNode("linux-network", "网络排查", "ping/telnet/curl/tcpdump/ss/netstat/mtr"),
            commandNode("linux-shell", "Shell脚本", "变量/循环/函数/条件判断、$?/$@/$*"),
            commandNode("linux-crontab", "定时任务", "crontab语法、日志、systemd timer替代"),
            commandNode("linux-ulimit", "系统资源限制", "ulimit/文件句柄/进程数、dockerd默认限制"),
            commandNode("linux-disk", "磁盘与文件系统", "inode、软/硬链接、df/du/iostat"),
            commandNode("linux-java", "Java问题排查", "CPU飚高定位、内存泄漏排查、jstack分析死锁"),
            commandNode("linux-performance", "性能分析", "perf/flamegraph/火焰图/BCC/eBPF"),
            commandNode("linux-security", "安全基础", "SSH/iptables/firewalld/SELinux")
        ), false, null));

        // ==================== 14. Docker & Kubernetes ====================
        tree.add(new KnowledgeNodeDTO("container", "Docker & Kubernetes", "容器化与编排", List.of(
            commandNode("dockerfile", "Dockerfile编写", "多阶段构建、.dockerignore、层缓存优化"),
            commandNode("docker-compose", "Docker Compose", "服务编排、volumes/network、depends_on"),
            commandNode("docker-network", "Docker网络", "bridge/host/overlay、端口映射、DNS解析"),
            commandNode("docker-volume", "数据持久化", "volume/bind mount/tmpfs、存储驱动"),
            commandNode("k8s-pod", "Pod核心概念", "Pod生命周期、Init Container、健康检查"),
            commandNode("k8s-deployment", "Deployment与扩缩", "ReplicaSet、滚动更新、回滚、HPA"),
            commandNode("k8s-service", "Service网络", "ClusterIP/NodePort/LoadBalancer、DNS"),
            commandNode("k8s-configmap", "ConfigMap与Secret", "环境变量注入、挂载卷、敏感信息加密"),
            commandNode("k8s-helm", "Helm包管理", "Chart结构、模板渲染、Values覆盖"),
            commandNode("k8s-monitor", "K8s监控", "cadvisor/metrics-server/Prometheus/Grafana")
        ), false, null));

        // ==================== 15. Git & 工程化 ====================
        tree.add(new KnowledgeNodeDTO("tooling", "Git & 工程化", "版本控制、构建工具、CI/CD", List.of(
            commandNode("git-branch", "分支策略", "Git Flow/GitHub Flow/Trunk Based、PR/MR"),
            commandNode("git-rebase", "Rebase与Merge", "merge vs rebase vs squash、交互式rebase"),
            commandNode("git-advanced", "Git进阶", "cherry-pick/stash/reset/revert/reflog"),
            commandNode("maven-lifecycle", "Maven生命周期", "clean/default/site、phase/plugin/goal"),
            commandNode("maven-multi-module", "多模块构建", "模块依赖、dependencyManagement、聚合"),
            commandNode("maven-settings", "Maven配置", "settings.xml/仓库镜像/私服/Nexus"),
            commandNode("ci-cd", "CI/CD", "Jenkins/GitHub Actions/GitLab CI流水线"),
            commandNode("code-quality", "代码质量", "CheckStyle/PMD/SpotBugs/SonarQube")
        ), false, null));

        // ==================== 16. 测试 ====================
        tree.add(new KnowledgeNodeDTO("testing", "测试", "单元测试、集成测试、性能测试", List.of(
            conceptNode("test-junit5", "JUnit 5", "生命周期、断言、参数化测试、@Nested"),
            conceptNode("test-mockito", "Mockito", "Mock/Spy、@InjectMocks、Answer/ArgumentCaptor"),
            conceptNode("test-spring", "Spring测试", "@SpringBootTest、@WebMvcTest、TestRestTemplate"),
            conceptNode("test-containers", "Testcontainers", "容器化集成测试、数据库/Redis/MQ容器"),
            conceptNode("test-performance", "性能测试", "JMeter/Gatling、吞吐量/TP99、压测规划"),
            conceptNode("test-tdd", "TDD与BDD", "红-绿-重构、Given-When-Then")
        ), false, null));

        // ==================== 17. 系统设计 ====================
        tree.add(new KnowledgeNodeDTO("system-design", "系统设计", "架构设计、高并发、方案选型", List.of(
            conceptNode("design-arch-evolution", "架构演进", "单体→垂直→SOA→微服务→Serverless"),
            conceptNode("design-flash-sale", "秒杀系统设计", "流量削峰、分层过滤、库存预热、MQ异步"),
            conceptNode("design-short-url", "短链服务设计", "发号器、Base62/62进制、重定向302"),
            conceptNode("design-idempotent", "幂等设计", "唯一键/Token/状态机/去重表/TCC防悬挂"),
            conceptNode("design-rate-limit", "限流算法", "令牌桶/漏桶/计数器/滑动窗口、Guava RateLimiter"),
            conceptNode("design-sharding", "分库分表方案", "范围/哈希/时间分片、跨分片查询、全局主键"),
            conceptNode("design-id-gen", "ID生成方案", "UUID/Snowflake/DB自增/Leaf/美团、趋势递增"),
            conceptNode("design-cap-practice", "CAP实战权衡", "CP vs AP选型、最终一致性实现方案"),
            conceptNode("design-high-concurrency", "高并发设计", "缓存/异步/池化/无锁/水平扩展"),
            conceptNode("design-failover", "高可用与容灾", "冗余/故障转移/降级/熔断/多活")
        ), false, null));

        // ==================== 18. 算法与数据结构 ====================
        tree.add(new KnowledgeNodeDTO("algorithm", "算法与数据结构", "面试高频算法题", List.of(
            algoNode("algo-sort", "排序算法", "快排/归并/堆排时间复杂度、稳定性、手写快排"),
            algoNode("algo-linked-list", "链表操作", "反转/环检测/合并有序链表/K个一组反转"),
            algoNode("algo-lru", "LRU缓存", "LinkedHashMap实现、双向链表+HashMap"),
            algoNode("algo-binary-tree", "二叉树", "前/中/后序、层序、最近公共祖先、序列化"),
            algoNode("algo-topk", "TopK问题", "大顶堆/小顶堆/快排partition/海量数据"),
            algoNode("algo-bfs-dfs", "BFS与DFS", "邻接表/矩阵、最短路径、拓扑排序"),
            algoNode("algo-dp", "动态规划", "背包/最长子序列/斐波那契、状态转移方程"),
            algoNode("algo-two-pointer", "双指针与滑动窗口", "快慢指针、左右指针、窗口伸缩")
        ), false, null));

        return tree;
    }

    // ======================== 掌握状态 ========================

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

    // ======================== 节点工厂 ========================

    private static KnowledgeNodeDTO node(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true, null);
    }

    private static KnowledgeNodeDTO projectNode(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true, "project");
    }

    private static KnowledgeNodeDTO conceptNode(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true, "concept");
    }

    private static KnowledgeNodeDTO commandNode(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true, "command");
    }

    private static KnowledgeNodeDTO algoNode(String id, String name, String description) {
        return new KnowledgeNodeDTO(id, name, description, null, true, "algorithm");
    }

    // ======================== 场景映射: 节点ID → 自然语言问题 ========================

    private static final Map<String, String> SCENARIO_MAP = buildScenarioMap();

    private static Map<String, String> buildScenarioMap() {
        Map<String, String> map = new HashMap<>();
        // --- Java基础 ---
        map.put("java-basics-datatype", "为什么 Integer 用 == 比较 100 是 true，200 却是 false？");
        map.put("java-basics-string", "为什么 new String(\"hello\") 用 == 比较结果是 false？");
        map.put("java-basics-passing", "为什么方法里改了 List，外面的 List 也变了？");
        map.put("java-basics-equals", "HashMap 用 Person 做 key，为什么 get 不到值？");
        map.put("java-basics-generics", "ArrayList<String> 转 ArrayList<Object> 为什么编译报错？");
        map.put("java-basics-reflection", "为什么反射可以访问 private 字段修改 final 变量？");
        map.put("java-basics-annotation", "自定义注解为什么有时候获取不到值？");
        map.put("java-basics-lambda", "Lambda 表达式里用外部变量为什么要 effectively final？");
        map.put("java-basics-stream", "Stream 的 filter 里打印日志为什么没执行？");
        map.put("java-basics-optional", "Optional.of(null) 为什么报 NPE？");
        map.put("java-basics-serial", "为什么反序列化会破坏单例？");
        map.put("java-basics-io", "BufferedReader 为什么需要配合 InputStreamReader 使用？");
        map.put("java-basics-enum", "枚举是否可以延迟加载？为什么枚举单例是安全的？");
        map.put("java-basics-bigdecimal", "BigDecimal 用 double 构造为什么精度不对？");
        map.put("java-basics-datetime", "SimpleDateFormat 为什么线程不安全？");
        map.put("java-basics-copy", "ArrayList 的 clone 是深拷贝还是浅拷贝？");
        map.put("java-basics-static", "静态方法能不能被子类重写？");
        map.put("java-basics-final", "final 修饰的数组内容能修改吗？");
        map.put("java-basics-inner-class", "匿名内部类访问局部变量为什么要 final？");
        map.put("java-basics-interface", "接口的 default 方法和抽象类有什么区别？");
        // --- 面向对象 ---
        map.put("oop-encapsulation", "Java 的访问修饰符 public/protected/default/private 各有什么用？");
        map.put("oop-extends", "为什么 Java 不支持多继承？");
        map.put("oop-polymorphism", "重载和重写有什么区别？哪个是编译时多态？");
        map.put("oop-abstraction", "面向接口编程到底有什么好处？");
        map.put("oop-composition", "为什么说组合优先于继承？你见过哪些继承滥用导致的问题？");
        map.put("oop-singleton", "DCL 单例为什么需要 volatile？");
        map.put("oop-proxy", "JDK 动态代理为什么必须传接口？CGLIB 呢？");
        map.put("oop-solid-single", "一个类既处理订单又发送邮件，违反了哪个原则？");
        map.put("oop-solid-open", "开闭原则怎么理解？如何在不修改代码的情况下扩展功能？");
        map.put("oop-solid-liskov", "子类重写父类方法抛出更多异常违反了哪个原则？");
        map.put("oop-solid-interface-seg", "一个接口有 10 个方法，但实现类只用其中 5 个，哪里不对？");
        map.put("oop-solid-di", "依赖反转和依赖注入是一回事吗？");
        // --- 异常处理 ---
        map.put("exception-try-finally", "为什么 try 里 return 了，finally 还会执行？");
        map.put("exception-try-with-resources", "try-with-resources 关闭资源的顺序是反的还是正的？");
        map.put("exception-hierarchy", "异常捕获：多个 catch 的顺序有讲究吗？");
        map.put("exception-checked", "受检异常和非受检异常怎么选？Spring 为什么偏爱运行时异常？");
        map.put("exception-suppressed", "try-with-resources 如果 close 也抛异常了会怎样？");
        map.put("exception-custom", "自定义异常应该继承 Exception 还是 RuntimeException？");
        map.put("exception-performance", "异常填充栈信息是不是很慢？能禁用吗？");
        map.put("exception-best-practice", "catch 到异常后打了一条日志就吞掉了，这有什么问题？");
        // --- 集合框架 ---
        map.put("collection-arraylist", "ArrayList 的 subList 返回的是新列表还是视图？");
        map.put("collection-hashmap", "HashMap 1.7 和 1.8 有什么区别？为什么 1.8 要引入红黑树？");
        map.put("collection-concurrent-hashmap", "ConcurrentHashMap 的 size() 方法是怎么实现的？");
        map.put("collection-treemap", "TreeMap 里放自定义对象为什么报 ClassCastException？");
        map.put("collection-queue", "BlockingQueue 的 offer、add、put 有什么区别？");
        map.put("collection-hashset", "HashSet 是怎么去重的？重复的元素会被覆盖吗？");
        map.put("collection-treeset", "TreeSet 判断重复的依据是 equals 还是 Comparable？");
        map.put("collection-failfast", "在遍历集合时删除元素为什么会抛 ConcurrentModificationException？");
        map.put("collection-collections", "Collections.unmodifiableList 返回的列表真的不可修改吗？");
        map.put("collection-arrays", "Arrays.asList(1, 2, 3).add(4) 为什么报错？");
        map.put("collection-sublist", "ArrayList.subList 的修改会影响原列表吗？");
        map.put("collection-aslist", "Arrays.asList(new int[]{1,2,3}).size() 结果是多少？");
        map.put("collection-iterable", "for-each 循环里能不能删除元素？");
        map.put("collection-enum-map", "EnumMap 相比 HashMap 有什么性能优势？");
        // --- 并发编程 ---
        map.put("concurrency-thread", "线程的 start() 和 run() 有什么区别？");
        map.put("concurrency-synchronized", "synchronized 加到静态方法和实例方法锁的是什么？");
        map.put("concurrency-volatile", "volatile 能保证原子性吗？为什么 i++ 不是安全的？");
        map.put("concurrency-reentrantlock", "ReentrantLock 和 synchronized 怎么选？");
        map.put("concurrency-readwritelock", "读多写少的场景用 ReadWriteLock 一定能提升性能吗？");
        map.put("concurrency-stampedlock", "StampedLock 的乐观读和悲观读分别怎么用？");
        map.put("concurrency-threadpool", "线程池的核心线程数怎么设置？为什么 IO 密集型和 CPU 密集型不同？");
        map.put("concurrency-deadlock", "死锁怎么排查？jstack 看到死锁是什么样子的？");
        map.put("concurrency-atomic", "AtomicInteger 的 incrementAndGet 是原子的吗？底层原理？");
        map.put("concurrency-completable-future", "CompletableFuture 的 thenApply、thenCompose、thenCombine 有什么区别？");
        map.put("concurrency-threadlocal", "ThreadLocal 为什么会内存泄漏？remove 了还会吗？");
        map.put("concurrency-forkjoin", "ForkJoinPool 的工作窃取是怎么实现的？");
        map.put("concurrency-locksupport", "LockSupport.park 和 Object.wait 有什么区别？");
        map.put("concurrency-semaphore", "CountDownLatch、CyclicBarrier、Semaphore 各自的应用场景？");
        map.put("concurrency-blockingqueue", "SynchronousQueue 是没有容量的队列？那 put 和 take 怎么工作？");
        map.put("concurrency-phaser", "Phaser 相比 CyclicBarrier 多了什么能力？");
        map.put("concurrency-false-sharing", "为什么多线程修改相邻变量性能会急剧下降？");
        map.put("concurrency-aqs", "AQS 的共享模式和独占模式分别怎么用？");
        map.put("concurrency-cpu-cache", "内存屏障是什么？volatile 底层怎么用内存屏障？");
        // --- JVM ---
        map.put("jvm-memory", "JDK8 的方法区去哪了？元空间和永久代有什么区别？");
        map.put("jvm-gc", "哪些对象可以作为 GC Root？");
        map.put("jvm-collector", "CMS 垃圾收集器为什么会有并发失败？");
        map.put("jvm-oom", "java.lang.OutOfMemoryError: Java heap space 和 Direct buffer memory 有什么区别？");
        map.put("jvm-classloader", "双亲委派模型怎么打破？Tomcat 和 JDBC 分别是怎么做的？");
        map.put("jvm-tuning", "JVM 调优时一般看哪些指标？怎么判断 GC 频率是否正常？");
        map.put("jvm-reference", "软引用在内存不足时会被回收，那引用队列是干什么的？");
        map.put("jvm-string-intern", "String.intern() 在 JDK6 和 JDK7+ 的行为有什么不同？");
        map.put("jvm-escape", "什么是逃逸分析？栈上分配真的会发生吗？");
        map.put("jvm-off-heap", "堆外内存怎么释放？DirectByteBuffer 的回收机制是什么？");
        map.put("jvm-jit", "什么是 JIT 编译？什么情况下会触发分层编译？");
        map.put("jvm-tool", "CPU 飚到 100% 了，怎么用 jstack 找到问题线程？");
        map.put("jvm-bytecode", "Java 的 invokedynamic 指令是干什么用的？");
        map.put("jvm-compressed-oops", "为什么 JVM 默认开启压缩指针？超过多少 GB 会自动关闭？");
        map.put("jvm-g1-region", "G1 的 SATB 和 CMS 的增量更新有什么区别？");
        // --- Spring 陷阱 ---
        map.put("spring-trap-transaction", "@Transactional 失效：私有方法调用");
        map.put("spring-trap-circular", "循环依赖：构造器注入报错");
        map.put("spring-trap-aop", "AOP 失效：方法自调用");
        map.put("spring-trap-scope", "Singleton Bean 注入 Prototype Bean 失效");
        // --- Spring 理论节点 ---
        map.put("spring-transaction", "同一个类里 A 方法调 B 方法，B 的 @Transactional 会生效吗？");
        map.put("spring-boot", "Spring Boot 的 @EnableAutoConfiguration 是怎么扫描到第三方 Starter 的？");
        map.put("spring-event", "Spring 的 @EventListener 是同步还是异步的？怎么改成异步？");
        map.put("spring-async", "@Async 注解为什么有时候不生效？");
        map.put("spring-interceptor", "Filter 和 Interceptor 的执行顺序是什么？谁先谁后？");
        // --- 数据库 ---
        map.put("database-mysql-arch", "MySQL 执行一条 SELECT 语句的完整流程是什么？");
        map.put("database-sql-order", "SQL 中 WHERE 和 HAVING 执行顺序哪个先？");
        map.put("database-index", "为什么 MySQL 用 B+ 树而不是 B 树或红黑树？");
        map.put("database-transaction", "MySQL RR 级别下能避免幻读吗？");
        map.put("database-lock", "MySQL 的间隙锁在什么情况下会生效？");
        map.put("database-sql-optimize", "一个 SQL 查询很慢，怎么用 EXPLAIN 分析？");
        map.put("database-count", "count(*) 和 count(1) 哪个更快？");
        map.put("database-join-optimize", "为什么大表 JOIN 查询很慢？怎么优化？");
        map.put("database-deep-paging", "LIMIT 1000000, 10 为什么越来越慢？怎么优化？");
        map.put("database-normal-form", "数据库设计时一定要满足第三范式吗？");
        map.put("database-charset", "MySQL 存 emoji 为什么变成了问号？");
        map.put("database-on-duplicate", "INSERT ... ON DUPLICATE KEY UPDATE 会有死锁问题吗？");
        map.put("database-lock-pess-opti", "乐观锁在并发高的时候真的比悲观锁好吗？");
        map.put("database-sharding", "分表后跨多个分片的查询怎么处理？");
        map.put("database-read-write", "主从复制的延迟怎么解决？");
        map.put("database-migration", "大表 DDL 怎么做不影响线上？");
        // --- MyBatis ---
        map.put("mybatis-dynamic-sql", "MyBatis 的 foreach 标签批量插入性能怎么样？");
        map.put("mybatis-sql-injection", "MyBatis 里 #{} 和 ${} 有什么区别？哪个能防注入？");
        map.put("mybatis-cache", "MyBatis 二级缓存为什么会有脏数据？");
        map.put("mybatis-plugin", "PageHelper 分页插件是怎么拦截 SQL 的？");
        map.put("mybatis-result", "MyBatis 的 ResultMap 和 ResultType 怎么选？");
        map.put("mybatis-nested-query", "OneToMany 关联查询为什么会产生 N+1 问题？");
        map.put("mybatis-generator", "MyBatis Generator 生成的 Example 类有什么用？");
        // --- 设计模式 ---
        map.put("pattern-factory", "简单工厂和策略模式看起来很像，区别是什么？");
        map.put("pattern-builder", "Builder 模式一定比构造器更好吗？");
        map.put("pattern-strategy", "策略模式和 if-else 比好在哪里？");
        map.put("pattern-observer", "Guava EventBus 和 Spring Event 的区别？");
        map.put("pattern-decorator", "装饰器模式和代理模式看起来好像，怎么区分？");
        map.put("pattern-chain", "Netty 的 Pipeline 是责任链模式吗？链怎么断开的？");
        map.put("pattern-template", "模板方法模式和策略模式有什么本质区别？");
        map.put("pattern-adapter", "适配器模式和门面模式有什么不同？");
        map.put("pattern-flyweight", "Integer 缓存和 String 常量池是享元模式的应用吗？");
        map.put("pattern-facade", "门面模式会不会变成上帝类？");
        map.put("pattern-mediator", "中介者模式和观察者模式怎么选？");
        map.put("pattern-memento", "备忘录模式在 undo/redo 中怎么应用？");
        // --- 网络与IO ---
        map.put("io-bio-nio-aio", "BIO、NIO、AIO 各自的适用场景是什么？");
        map.put("io-netty", "Netty 的 Reactor 模型有几种线程模型？");
        map.put("io-zerocopy", "Kafka 为什么用零拷贝？");
        map.put("net-tcp-handshake", "TCP 三次握手，最后一次握手的 ACK 丢了会怎样？");
        map.put("net-timewait", "服务器上大量 TIME_WAIT 是什么原因？怎么解决？");
        map.put("net-keepalive", "HTTP/1.1 的 Keep-Alive 和 HTTP/2 的多路复用有什么区别？");
        map.put("net-timeout", "connect timeout 和 read timeout 分别在哪里配置？");
        map.put("net-websocket", "WebSocket 和 HTTP 是什么关系？");
        map.put("net-tls", "HTTPS 的 TLS 握手过程中，客户端怎么验证服务器证书的？");
        map.put("net-dns", "DNS 解析过程是怎样的？浏览器 DNS 缓存多久？");
        // --- 分布式 ---
        map.put("distributed-cap", "注册中心选型，为什么有人用 Eureka（AP）有人用 ZooKeeper（CP）？");
        map.put("distributed-redis", "Redis 的分布式锁用 setnx 还是 Redisson？主从切换时锁会丢吗？");
        map.put("distributed-mq", "RocketMQ 的事务消息是怎么实现最终一致性的？");
        map.put("distributed-transaction", "Seata AT 模式和 TCC 模式各自适合什么场景？");
        map.put("distributed-discovery", "Nacos 是 AP 还是 CP？能同时支持吗？");
        map.put("distributed-config", "配置中心的热更新是怎么通知到应用的？");
        map.put("distributed-gateway", "网关层做限流和业务层做限流有什么区别？");
        map.put("distributed-sentinel", "Sentinel 的滑动窗口限流是怎么统计 QPS 的？");
        map.put("distributed-tracing", "SkyWalking 怎么做到无侵入的链路追踪？");
        map.put("distributed-es", "ES 的倒排索引是怎么加速搜索的？");
        map.put("distributed-id", "雪花算法的时钟回拨问题怎么解决？");
        map.put("distributed-raft", "Raft 的 Leader 选举节点数量必须是奇数吗？");
        map.put("distributed-load-balance", "一致性哈希在扩容时能保证多少数据不迁移？");
        map.put("distributed-session", "JWT 无状态 token 注销了怎么办？");
        map.put("distributed-interface-idempotent", "接口幂等性怎么设计？MQ 重复消费怎么处理？");
        // --- Linux ---
        map.put("linux-command", "lsof -i:8080 看到的是什么信息？");
        map.put("linux-process", "服务器 OOM 了，怎么找到被 Kill 的进程？");
        map.put("linux-network", "两个服务连不上，怎么排查是防火墙、端口还是网络的问题？");
        map.put("linux-shell", "Shell 脚本中 $? 表示什么？怎么判断上一条命令是否成功？");
        map.put("linux-crontab", "crontab 的任务没执行，怎么排查？");
        map.put("linux-ulimit", "too many open files 怎么解决？");
        map.put("linux-disk", "磁盘空间满了但找不到大文件，可能是哪里占了？");
        map.put("linux-java", "Java 进程 CPU 100% 怎么定位到代码行？");
        map.put("linux-performance", "火焰图怎么看？怎么生成 Java 火焰图？");
        map.put("linux-security", "SSH 暴力破解怎么防护？");
        // --- Docker & K8s ---
        map.put("dockerfile", "Docker 镜像层缓存什么时候会失效？");
        map.put("docker-compose", "docker-compose 的 depends_on 真的能确保服务就绪吗？");
        map.put("docker-network", "bridge 和 host 网络模式怎么选？");
        map.put("docker-volume", "Docker 容器的数据存在宿主机哪里？容器删了数据还在吗？");
        map.put("k8s-pod", "Pod 中的容器都挂了，Pod 会怎么样？");
        map.put("k8s-deployment", "Deployment 滚动更新策略 maxSurge 和 maxUnavailable 怎么配？");
        map.put("k8s-service", "ClusterIP 和 NodePort 的访问路径是什么？");
        map.put("k8s-configmap", "ConfigMap 更新后 Pod 里的配置会热更新吗？");
        map.put("k8s-helm", "Helm 的 values.yaml 覆盖层级是怎么工作的？");
        map.put("k8s-monitor", "K8s 集群 Pod 内存持续上涨，怎么定位？");
        // --- Git & 工程化 ---
        map.put("git-branch", "Git Flow 和 Trunk Based 各自适合什么团队？");
        map.put("git-rebase", "rebase 和 merge 哪个更好？在公共分支上 rebase 有什么风险？");
        map.put("git-advanced", "git reset、revert、rebase 有什么区别？");
        map.put("maven-lifecycle", "mvn clean install 执行了哪些 phase？");
        map.put("maven-multi-module", "多模块 Maven 项目怎么解决循环依赖？");
        map.put("maven-settings", "Maven 依赖冲突时怎么排除？");
        map.put("ci-cd", "CI/CD 流水线中怎么做自动化测试和部署？");
        map.put("code-quality", "SonarQube 检测出的严重问题有哪些？");
        // --- 测试 ---
        map.put("test-junit5", "JUnit 5 的 @ParameterizedTest 怎么用？");
        map.put("test-mockito", "Mockito 的 when 和 verify 有什么区别？");
        map.put("test-spring", "@SpringBootTest 和 @WebMvcTest 有什么区别？哪个更快？");
        map.put("test-containers", "Testcontainers 启动的 MySQL 容器怎么让其他测试共用？");
        map.put("test-performance", "性能测试的 TP95、TP99 是什么意思？");
        map.put("test-tdd", "TDD 的红-绿-重构三个步骤分别做什么？");
        // --- 系统设计 ---
        map.put("design-arch-evolution", "从单体到微服务，第一步应该拆什么？");
        map.put("design-flash-sale", "秒杀系统怎么防止超卖？怎么防止黄牛？");
        map.put("design-short-url", "短链服务的跳转用 301 还是 302？为什么？");
        map.put("design-idempotent", "支付接口的幂等性怎么设计？重试会重复扣款吗？");
        map.put("design-rate-limit", "令牌桶和漏桶算法有什么区别？Guava RateLimiter 是哪种？");
        map.put("design-sharding", "用户表分片用 user_id hash 还是按时间？");
        map.put("design-id-gen", "分布式 ID 一定要全局递增吗？Snowflake 有什么缺点？");
        map.put("design-cap-practice", "你做的系统选的是 CP 还是 AP？怎么保证最终一致性？");
        map.put("design-high-concurrency", "高并发场景下数据库扛不住怎么办？");
        map.put("design-failover", "多活架构中写冲突怎么处理？");
        // --- 算法 ---
        map.put("algo-sort", "手写快排，时间复杂度是多少？什么情况下会退化？");
        map.put("algo-linked-list", "判断链表有没有环？快慢指针为什么一定相遇？");
        map.put("algo-lru", "LRU 缓存怎么实现？为什么用双向链表？");
        map.put("algo-binary-tree", "二叉树的层序遍历怎么写？");
        map.put("algo-topk", "海量数据中找 TopK，内存不够怎么办？");
        map.put("algo-bfs-dfs", "二维矩阵的岛屿数量怎么数？");
        map.put("algo-dp", "0-1 背包的状态转移方程是什么？");
        map.put("algo-two-pointer", "有序数组的两数之和，怎么用 O(n) 时间解决？");
        return map;
    }

    // ======================== 项目节点判断与生成 ========================

    public boolean isProjectNode(String nodeId) {
        KnowledgeNodeDTO found = findNode(getTree(), nodeId);
        return found != null && "project".equals(found.getType());
    }

    public ProjectInfoDTO generateProject(String nodeId) {
        if (!isProjectNode(nodeId)) {
            throw new RuntimeException("不是项目类型节点: " + nodeId);
        }
        return springProjectService.generateProject(nodeId);
    }

    // ======================== 多类型内容生成 ========================

    public ScenarioDto generateForNode(String nodeId) {
        List<KnowledgeNodeDTO> tree = getTree();
        KnowledgeNodeDTO found = findNode(tree, nodeId);
        if (found == null || !found.isLeaf()) {
            throw new RuntimeException("未找到知识点: " + nodeId);
        }

        ScenarioDto cached = getCachedScenario(nodeId);
        if (cached != null) {
            log.debug("命中缓存 nodeId={}", nodeId);
            return cached;
        }

        String knowledgePoint = found.getName();
        String description = found.getDescription();
        String type = found.getType();

        // 非陷阱类型 → 使用对应的生成器（不持久化）
        if ("concept".equals(type)) {
            return generateNonTrapContent("concept", buildConceptPrompt(knowledgePoint, description), nodeId);
        }
        if ("command".equals(type)) {
            return generateNonTrapContent("command", buildCommandPrompt(knowledgePoint, description), nodeId);
        }
        if ("algorithm".equals(type)) {
            return generateNonTrapContent("algorithm", buildAlgorithmPrompt(knowledgePoint, description), nodeId);
        }

        // 陷阱代码类型（type == null）→ 原有流程
        // Try direct mapping to existing scenario
        String mappedKnowledgePoint = SCENARIO_MAP.get(nodeId);
        if (mappedKnowledgePoint != null) {
            LabScenario mapped = scenarioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                    .eq(LabScenario::getKnowledgePoint, mappedKnowledgePoint)
            );
            if (mapped != null) {
                if (isFallbackScenario(mapped)) {
                    scenarioMapper.deleteById(mapped.getId());
                    log.info("删除旧 fallback 记录 [{}]（SCENARIO_MAP），将重新生成", mapped.getId());
                } else {
                    log.info("知识点 [{}] -> 映射到已有场景 [{}]", nodeId, mappedKnowledgePoint);
                    return attachNodeIdAndCache(nodeId, mapped);
                }
            }
        }

        // Try exact name match
        LabScenario existing = scenarioMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                .eq(LabScenario::getKnowledgePoint, knowledgePoint)
                .last("LIMIT 1")
        );
        if (existing != null) {
            if (isFallbackScenario(existing)) {
                scenarioMapper.deleteById(existing.getId());
                log.info("删除旧 fallback 记录 [{}]（精确匹配），将重新生成", existing.getId());
            } else {
                log.info("知识点 [{}] 已有 DB 缓存，直接返回", knowledgePoint);
                return attachNodeIdAndCache(nodeId, existing);
            }
        }

        // Try to generate via LLM (trap code)
        try {
            Map<String, String> llmConfig = llmConfigResolver.resolve();
            String result = llmClient.chatSimple(
                    llmConfig.get("api_key"),
                    llmConfig.get("base_url"),
                    llmConfig.get("model"),
                    null,
                    buildPrompt(knowledgePoint, description));
            String cleaned = com.cortex.util.JsonUtils.cleanJson(result);
            ScenarioDto dto = JSON.parseObject(cleaned, ScenarioDto.class);

            LabScenario entity = new LabScenario();
            entity.setNodeId(nodeId);
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
            cacheScenario(nodeId, entity);

            syncToQuestionBankAndGenerateCard(entity);

            return dto;
        } catch (Exception e) {
            log.warn("LLM 生成陷阱代码失败，使用概念讲解代替: {}", e.getMessage());
            // 不持久化/缓存 fallback，确保配置 API Key 后能重新生成
            String fallback = buildFallbackContent("concept", knowledgePoint, description);
            return new ScenarioDto(null, knowledgePoint, getCategoryForNode(nodeId),
                fallback, description, knowledgePoint + "\n\n" + description,
                null, 1, null, fallback);
        }
    }

    // 非陷阱类型通用生成器：查缓存 → LLM 生成 → 持久化 → 返回
    private ScenarioDto generateNonTrapContent(String contentType, String prompt, String nodeId) {
        ScenarioDto cached = getCachedScenario(nodeId);
        if (cached != null) {
            return cached;
        }

        KnowledgeNodeDTO node = findNode(getTree(), nodeId);
        String knowledgePoint = node.getName();
        String description = node.getDescription();

        // 查缓存：按 type + knowledgePoint 查找
        try {
            LabScenario dbRow = scenarioMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                    .eq(LabScenario::getType, contentType)
                    .eq(LabScenario::getKnowledgePoint, knowledgePoint)
                    .last("LIMIT 1")
            );
            if (dbRow != null) {
                if (isFallbackScenario(dbRow)) {
                    scenarioMapper.deleteById(dbRow.getId());
                    log.info("删除旧 fallback 记录 [{}]（类型缓存），将重新生成", dbRow.getId());
                } else {
                    return attachNodeIdAndCache(nodeId, dbRow);
                }
            }
        } catch (Exception e) {
            log.warn("查询缓存失败: {}", e.getMessage());
        }

        // 尝试 LLM 生成
        try {
            Map<String, String> llmConfig = llmConfigResolver.resolve();
            String result = llmClient.chatSimple(
                    llmConfig.get("api_key"),
                    llmConfig.get("base_url"),
                    llmConfig.get("model"),
                    null,
                    prompt);

            LabScenario entity = new LabScenario();
            entity.setNodeId(nodeId);
            entity.setKnowledgePoint(knowledgePoint);
            entity.setCategory(getCategoryForNode(nodeId));
            entity.setType(contentType);
            entity.setGeneratedContent(result);
            entity.setGmtCreate(LocalDateTime.now());
            scenarioMapper.insert(entity);

            ScenarioDto dto = new ScenarioDto();
            dto.setId(entity.getId());
            dto.setType(contentType);
            dto.setGeneratedContent(result);
            dto.setTrapCode(result);
            dto.setKnowledgePoint(knowledgePoint);
            cacheScenario(nodeId, entity);
            return dto;
        } catch (Exception e) {
            log.warn("{} 内容生成失败，使用备用内容: {}", contentType, e.getMessage());
            // 不持久化/缓存，确保配置 API Key 后能重新生成
            String fallback = buildFallbackContent(contentType, knowledgePoint, description);
            return new ScenarioDto(null, knowledgePoint, getCategoryForNode(nodeId),
                fallback, null, null, null, null, contentType, fallback);
        }
    }

    // LLM 不可用时的备用内容
    private String buildFallbackContent(String contentType, String name, String desc) {
        return switch (contentType) {
            case "concept" -> """
## 概念
%s 是 Java 开发中的核心知识点。%s

## 为什么重要
掌握这个概念对理解 Java 底层机制和写出高质量代码至关重要。

## 代码示例
```java
// 请配置 API Key 后 AI 自动生成示例代码
// 当前为概念说明模式，查看下方说明理解原理
```
""".formatted(name, desc != null ? desc : "");
            case "command" -> """
## 概述
%s — %s

## 常用操作
请先在小C助手的「配置」面板中填写 API Key。

## 学习建议
1. 在开发环境中实际操作
2. 结合项目实践加深理解
3. 查阅官方文档获取完整参考
""".formatted(name, desc != null ? desc : "");
            case "algorithm" -> """
## 问题描述
%s

## 思路分析
%s

## 复杂度分析
请先在小C助手的「配置」面板中填写 API Key。

## 学习建议
1. 先理解算法思路
2. 在白板上画出示意图
3. 在 LeetCode 上找相关题目练习
""".formatted(name, desc != null ? desc : "");
            default -> desc != null ? desc : name;
        };
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

    // 概念讲解 prompt
    private String buildConceptPrompt(String knowledgePoint, String description) {
        return """
    你是一个 Java 技术教学专家。请针对以下知识点，生成一篇简短的概念讲解。

    知识点: %s
    描述: %s

    请按以下结构生成纯文本（不要 JSON，不要 markdown 代码块标记）：
    ## 概念
    [用 100 字左右解释这个知识点]

    ## 为什么重要
    [50 字说明这个知识点在实际工作/面试中的意义]

    ## 代码示例
    [一段简洁的 Java 代码演示这个知识点，加上注释说明关键点]

    ## 常见误区
    [2-3 个常见错误理解或使用方式]

    ## 相关命令/配置（如果有）
    [如果是工具类知识点，列出相关命令或配置]
    """.formatted(knowledgePoint, description);
    }

    // 命令行演示 prompt
    private String buildCommandPrompt(String knowledgePoint, String description) {
        return """
    你是一个 DevOps/工具链教学专家。请针对以下知识点，生成一段命令行演示。

    知识点: %s
    描述: %s

    请按以下结构生成纯文本（不要 JSON，不要 markdown 代码块标记）：
    ## 概述
    [50 字说明这个知识点]

    ## 常用命令/操作
    [列出 3-5 个最常用的命令或操作，每个带简要说明和示例]

    ## 实战场景
    [1 个真实工作场景，展示如何组合使用这些命令解决问题]

    ## 常见问题
    [2-3 个常见错误和排查方法]
    """.formatted(knowledgePoint, description);
    }

    // 算法 prompt
    private String buildAlgorithmPrompt(String knowledgePoint, String description) {
        return """
    你是一个算法教学专家。请针对以下知识点，生成一段算法教学。

    知识点: %s
    描述: %s

    请按以下结构生成纯文本（不要 JSON，不要 markdown 代码块标记）：
    ## 问题描述
    [一句话说明要解决什么问题]

    ## 思路分析
    [100 字以内讲清楚算法思路]

    ## 复杂度分析
    [时间复杂度、空间复杂度]

    ## Java 代码实现
    [完整的 Java 代码，包含 main 方法测试，有注释]

    ## 边界情况
    [需要注意的边界条件和处理方式]

    ## 变体问题
    [1 个相关变体问题]
    """.formatted(knowledgePoint, description);
    }

    private KnowledgeNodeDTO findNode(List<KnowledgeNodeDTO> nodes, String id) {
        for (KnowledgeNodeDTO node : nodes) {
            if (id.equals(node.getId())) {
                return node;
            }
            if (node.getChildren() != null) {
                KnowledgeNodeDTO found = findNode(node.getChildren(), id);
                if (found != null) {
                    return found;
                }
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

    private ScenarioDto getCachedScenario(String nodeId) {
        ScenarioDto mem = scenarioCache.get(nodeId);
        if (mem != null) {
            // 内存中若为旧 fallback，清除后重新生成（用户可能刚配置了 API Key）
            if (isFallbackScenario(mem)) {
                log.debug("内存缓存为 fallback，跳过并重新生成 nodeId={}", nodeId);
                scenarioCache.remove(nodeId);
            } else {
                return mem;
            }
        }
        LabScenario db = scenarioMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LabScenario>()
                .eq(LabScenario::getNodeId, nodeId)
                .last("LIMIT 1")
        );
        if (db != null) {
            // 清除 DB 中旧 fallback 记录，确保配置 API Key 后能重新生成
            if (isFallbackScenario(db)) {
                log.info("清除节点 [{}] 的旧 fallback 缓存，将重新生成", nodeId);
                scenarioMapper.deleteById(db.getId());
                return null;
            }
            ScenarioDto dto = toDto(db);
            scenarioCache.put(nodeId, dto);
            return dto;
        }
        return null;
    }

    /** 判断是否为无 API Key 时的 fallback 内容 */
    private boolean isFallbackScenario(LabScenario e) {
        return e.getDifficulty() != null && e.getDifficulty() == 1
            && e.getTrapCode() != null && e.getTrapCode().contains("请配置");
    }

    private boolean isFallbackScenario(ScenarioDto dto) {
        return dto.getDifficulty() != null && dto.getDifficulty() == 1
            && dto.getTrapCode() != null && dto.getTrapCode().contains("请配置");
    }

    private ScenarioDto attachNodeIdAndCache(String nodeId, LabScenario entity) {
        if (entity.getNodeId() == null || entity.getNodeId().isBlank()) {
            entity.setNodeId(nodeId);
            scenarioMapper.updateById(entity);
        }
        ScenarioDto dto = toDto(entity);
        scenarioCache.put(nodeId, dto);
        return dto;
    }

    private void cacheScenario(String nodeId, LabScenario entity) {
        scenarioCache.put(nodeId, toDto(entity));
    }

    private void syncToQuestionBankAndGenerateCard(LabScenario scenario) {
        try {
            long exists = questionBankMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<QuestionBank>()
                    .eq(QuestionBank::getTitle, scenario.getKnowledgePoint())
            );
            if (exists > 0) {
                return;
            }
            QuestionBank qb = new QuestionBank();
            qb.setTitle(scenario.getKnowledgePoint());
            qb.setDescription(scenario.getExpectedPitfall());
            qb.setTrapCode(scenario.getTrapCode());
            qb.setExpectedPitfall(scenario.getExpectedPitfall());
            qb.setCorrectExplanation(scenario.getCorrectExplanation());
            qb.setHints(scenario.getHints());
            qb.setCategory(scenario.getCategory() != null ? scenario.getCategory() : "Java基础");
            qb.setDifficulty(scenario.getDifficulty() != null ? scenario.getDifficulty() : 2);
            qb.setStatus("ACTIVE");
            qb.setGmtCreate(LocalDateTime.now());
            qb.setGmtModified(LocalDateTime.now());
            questionBankMapper.insert(qb);

            Long questionId = qb.getId();
            log.info("已同步知识点 [{}] 到题库, questionId={}", scenario.getKnowledgePoint(), questionId);

            cardGeneratorExecutor.execute(() -> {
                try {
                    knowledgeCardService.generateCard(questionId);
                    log.info("知识卡片生成完成, questionId={}", questionId);
                } catch (Exception e) {
                    log.warn("知识卡片生成失败, questionId={}: {}", questionId, e.getMessage());
                }
            });
        } catch (Exception e) {
            log.warn("同步到题库失败: {}", e.getMessage());
        }
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
        dto.setType(entity.getType());
        dto.setGeneratedContent(entity.getGeneratedContent());
        return dto;
    }
}
-- ============================================================================
-- Cortex2 演示用户数据（完整版）
-- 用法: mysql -u root -p cortex2 < seed-demo-user.sql
-- 密码: 123456 (SHA-256)
-- ============================================================================

-- ==================== 1. 用户 ====================
INSERT IGNORE INTO `user` (`user_id`, `username`, `password_hash`, `email`, `role`, `status`,
    `total_study_hours`, `total_questions_answered`, `total_correct`, `study_streak`,
    `last_study_date`, `weak_areas`, `preferred_direction`, `learning_goal`, `skill_level`,
    `gmt_create`, `gmt_modified`)
VALUES ('demo001', 'demo', '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92',
    'demo@cortex.com', 'USER', 'ACTIVE',
    86.5, 327, 218, 15,
    CURDATE(), 'JVM调优,分布式事务,设计模式', 'Java后端',
    '半年内达到高级工程师水平，重点突破JVM调优和分布式系统', 'INTERMEDIATE',
    '2026-03-01 10:00:00', NOW());

-- ==================== 2. 日常学习记录（近30天） ====================
INSERT IGNORE INTO `daily_learning` (`user_id`, `log_date`, `questions_answered`, `correct_count`, `study_minutes`, `knowledge_points_studied`, `gmt_create`)
VALUES
('demo001', '2026-05-16', 8, 6, 60, '数据类型,Integer缓存池,自动拆装箱', '2026-05-16 12:00:00'),
('demo001', '2026-05-17', 10, 8, 75, 'String常量池,intern方法,字符串拼接', '2026-05-17 12:00:00'),
('demo001', '2026-05-18', 6, 5, 45, '值传递,引用传递,参数传递方式', '2026-05-18 12:00:00'),
('demo001', '2026-05-19', 12, 9, 90, 'equals,hashCode重写,HashMap的key', '2026-05-19 12:00:00'),
('demo001', '2026-05-20', 7, 5, 50, 'final关键字,static关键字,执行顺序', '2026-05-20 12:00:00'),
('demo001', '2026-05-21', 0, 0, 0, '休息日', '2026-05-21 12:00:00'),
('demo001', '2026-05-22', 9, 7, 65, '内部类,匿名类,Lambda表达式,函数式接口', '2026-05-22 12:00:00'),
('demo001', '2026-05-23', 11, 8, 80, 'ArrayList扩容,LinkedList对比,Vector', '2026-05-23 12:00:00'),
('demo001', '2026-05-24', 14, 10, 100, 'HashMap1.7 vs 1.8,红黑树,扩容树化', '2026-05-24 12:00:00'),
('demo001', '2026-05-25', 8, 6, 55, 'ConcurrentHashMap,CAS分段锁,size实现', '2026-05-25 12:00:00'),
('demo001', '2026-05-26', 10, 7, 70, 'TreeMap红黑树,Comparable,Comparator', '2026-05-26 12:00:00'),
('demo001', '2026-05-27', 6, 5, 40, 'HashSet,LinkedHashSet,TreeSet区别', '2026-05-27 12:00:00'),
('demo001', '2026-05-28', 12, 9, 85, 'fail-fast机制,CopyOnWriteArrayList,Queue', '2026-05-28 12:00:00'),
('demo001', '2026-05-29', 0, 0, 0, '休息日', '2026-05-29 12:00:00'),
('demo001', '2026-05-30', 15, 11, 110, '线程6种状态,Thread,Runnable,Callable', '2026-05-30 12:00:00'),
('demo001', '2026-05-31', 13, 9, 95, 'synchronized原理,偏向锁,轻量锁,重量锁', '2026-05-31 12:00:00'),
('demo001', '2026-06-01', 10, 8, 70, 'volatile,可见性,指令重排,happens-before', '2026-06-01 12:00:00'),
('demo001', '2026-06-02', 16, 12, 120, 'ReentrantLock,AQS,公平锁,Condition', '2026-06-02 12:00:00'),
('demo001', '2026-06-03', 12, 9, 80, '线程池核心参数,拒绝策略,Executors框架', '2026-06-03 12:00:00'),
('demo001', '2026-06-04', 8, 6, 60, '死锁排查,jstack分析,ThreadLocal内存泄漏', '2026-06-04 12:00:00'),
('demo001', '2026-06-05', 14, 10, 100, 'CAS原理,ABA问题,Atomic类,LongAdder', '2026-06-05 12:00:00'),
('demo001', '2026-06-06', 0, 0, 0, '休息日', '2026-06-06 12:00:00'),
('demo001', '2026-06-07', 11, 8, 85, 'JVM内存区域,堆栈方法区,元空间', '2026-06-07 12:00:00'),
('demo001', '2026-06-08', 13, 9, 95, 'GC机制,可达性分析,GC Root,Minor/Major GC', '2026-06-08 12:00:00'),
('demo001', '2026-06-09', 9, 7, 65, 'CMS,G1,ZGC,垃圾收集器对比', '2026-06-09 12:00:00'),
('demo001', '2026-06-10', 10, 8, 75, 'OOM分析,堆栈溢出,MAT分析,逃逸分析', '2026-06-10 12:00:00'),
('demo001', '2026-06-11', 15, 11, 110, '类加载机制,双亲委派,打破双亲委派', '2026-06-11 12:00:00'),
('demo001', '2026-06-12', 12, 9, 90, 'JVM调优常用参数,GC日志分析', '2026-06-12 12:00:00'),
('demo001', '2026-06-13', 18, 14, 130, 'Spring IoC,Bean生命周期,DI注入方式', '2026-06-13 12:00:00'),
('demo001', '2026-06-14', 20, 15, 150, 'Spring AOP,JDK代理,CGLIB,@Aspect', '2026-06-14 12:00:00'),
('demo001', '2026-06-15', 10, 8, 70, 'Spring MVC,DispatcherServlet,拦截器', '2026-06-15 10:00:00'),
('demo001', '2026-06-16', 8, 6, 60, '事务传播行为,失效场景,隔离级别', '2026-06-16 10:00:00'),
('demo001', '2026-06-17', 6, 5, 45, '循环依赖,三级缓存,@Lazy', '2026-06-17 10:00:00'),
('demo001', '2026-06-18', 7, 5, 50, '数据库索引优化,B+树,覆盖索引', '2026-06-18 10:00:00'),
('demo001', '2026-06-19', 9, 7, 65, 'MySQL锁,行锁,间隙锁,死锁排查', '2026-06-19 10:00:00'),
('demo001', '2026-06-20', 5, 4, 35, 'Redis数据结构,过期策略,分布式锁', '2026-06-20 10:00:00');

-- ==================== 3. 知识树掌握节点（45个已掌握 + 5个学习中） ====================
INSERT IGNORE INTO `lab_tree_progress` (`node_id`, `user_id`, `mastered`, `gmt_create`, `gmt_modified`)
VALUES
('java-basics-datatype', 'demo001', 1, '2026-05-16 12:00:00', '2026-05-16 12:00:00'),
('java-basics-string', 'demo001', 1, '2026-05-17 12:00:00', '2026-05-17 12:00:00'),
('java-basics-passing', 'demo001', 1, '2026-05-18 12:00:00', '2026-05-18 12:00:00'),
('java-basics-equals', 'demo001', 1, '2026-05-19 12:00:00', '2026-05-19 12:00:00'),
('java-basics-final', 'demo001', 1, '2026-05-20 12:00:00', '2026-05-20 12:00:00'),
('java-basics-static', 'demo001', 1, '2026-05-20 12:00:00', '2026-05-20 12:00:00'),
('java-basics-inner-class', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('java-basics-interface', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('java-basics-generics', 'demo001', 1, '2026-05-24 12:00:00', '2026-05-24 12:00:00'),
('oop-encapsulation', 'demo001', 1, '2026-05-20 12:00:00', '2026-05-20 12:00:00'),
('oop-extends', 'demo001', 1, '2026-05-20 12:00:00', '2026-05-20 12:00:00'),
('oop-polymorphism', 'demo001', 1, '2026-05-21 12:00:00', '2026-05-21 12:00:00'),
('oop-abstraction', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('oop-composition', 'demo001', 1, '2026-05-23 12:00:00', '2026-05-23 12:00:00'),
('oop-singleton', 'demo001', 1, '2026-05-25 12:00:00', '2026-05-25 12:00:00'),
('oop-proxy', 'demo001', 1, '2026-06-14 12:00:00', '2026-06-14 12:00:00'),
('exception-hierarchy', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('exception-checked', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('exception-try-finally', 'demo001', 1, '2026-05-22 12:00:00', '2026-05-22 12:00:00'),
('exception-try-with-resources', 'demo001', 1, '2026-05-23 12:00:00', '2026-05-23 12:00:00'),
('collection-arraylist', 'demo001', 1, '2026-05-23 12:00:00', '2026-05-23 12:00:00'),
('collection-hashmap', 'demo001', 1, '2026-05-24 12:00:00', '2026-05-24 12:00:00'),
('collection-concurrent-hashmap', 'demo001', 1, '2026-05-25 12:00:00', '2026-05-25 12:00:00'),
('collection-treemap', 'demo001', 1, '2026-05-26 12:00:00', '2026-05-26 12:00:00'),
('collection-hashset', 'demo001', 1, '2026-05-27 12:00:00', '2026-05-27 12:00:00'),
('collection-failfast', 'demo001', 1, '2026-05-28 12:00:00', '2026-05-28 12:00:00'),
('concurrency-thread', 'demo001', 1, '2026-05-30 12:00:00', '2026-05-30 12:00:00'),
('concurrency-synchronized', 'demo001', 1, '2026-05-31 12:00:00', '2026-05-31 12:00:00'),
('concurrency-volatile', 'demo001', 1, '2026-06-01 12:00:00', '2026-06-01 12:00:00'),
('concurrency-reentrantlock', 'demo001', 1, '2026-06-02 12:00:00', '2026-06-02 12:00:00'),
('concurrency-threadpool', 'demo001', 1, '2026-06-03 12:00:00', '2026-06-03 12:00:00'),
('concurrency-threadlocal', 'demo001', 1, '2026-06-04 12:00:00', '2026-06-04 12:00:00'),
('concurrency-atomic', 'demo001', 1, '2026-06-05 12:00:00', '2026-06-05 12:00:00'),
('jvm-memory', 'demo001', 1, '2026-06-07 12:00:00', '2026-06-07 12:00:00'),
('jvm-gc', 'demo001', 1, '2026-06-08 12:00:00', '2026-06-08 12:00:00'),
('jvm-collector', 'demo001', 1, '2026-06-09 12:00:00', '2026-06-09 12:00:00'),
('jvm-oom', 'demo001', 1, '2026-06-10 12:00:00', '2026-06-10 12:00:00'),
('jvm-classloader', 'demo001', 1, '2026-06-11 12:00:00', '2026-06-11 12:00:00'),
('jvm-tuning', 'demo001', 1, '2026-06-12 12:00:00', '2026-06-12 12:00:00'),
('spring-ioc', 'demo001', 1, '2026-06-13 12:00:00', '2026-06-13 12:00:00'),
('spring-aop', 'demo001', 1, '2026-06-14 12:00:00', '2026-06-14 12:00:00'),
('spring-mvc', 'demo001', 1, '2026-06-15 12:00:00', '2026-06-15 12:00:00'),
('spring-transaction', 'demo001', 1, '2026-06-16 12:00:00', '2026-06-16 12:00:00'),
('spring-boot', 'demo001', 1, '2026-06-17 12:00:00', '2026-06-17 12:00:00'),
('database-mysql-arch', 'demo001', 1, '2026-06-16 12:00:00', '2026-06-16 12:00:00'),
('database-index', 'demo001', 1, '2026-06-18 12:00:00', '2026-06-18 12:00:00'),
('database-transaction', 'demo001', 1, '2026-06-18 12:00:00', '2026-06-18 12:00:00'),
('database-lock', 'demo001', 1, '2026-06-19 12:00:00', '2026-06-19 12:00:00'),
('concurrency-completable-future', 'demo001', 0, '2026-06-05 12:00:00', '2026-06-05 12:00:00'),
('concurrency-forkjoin', 'demo001', 0, '2026-06-06 12:00:00', '2026-06-06 12:00:00'),
('database-sql-optimize', 'demo001', 0, '2026-06-19 12:00:00', '2026-06-19 12:00:00'),
('distributed-redis', 'demo001', 0, '2026-06-20 12:00:00', '2026-06-20 12:00:00'),
('jvm-reference', 'demo001', 0, '2026-06-10 12:00:00', '2026-06-10 12:00:00');

-- ==================== 4. 作答进度 ====================
INSERT IGNORE INTO `lab_question_progress` (`question_id`, `user_id`, `mastered`, `review_count`, `last_review_time`, `next_review_time`, `gmt_create`, `gmt_modified`)
VALUES
(1, 'demo001', 1, 4, '2026-06-18 10:00:00', '2026-06-25 10:00:00', '2026-05-16 12:00:00', '2026-06-18 10:00:00'),
(2, 'demo001', 1, 3, '2026-06-17 10:00:00', '2026-06-24 10:00:00', '2026-05-17 12:00:00', '2026-06-17 10:00:00'),
(3, 'demo001', 1, 5, '2026-06-19 10:00:00', '2026-06-26 10:00:00', '2026-05-18 12:00:00', '2026-06-19 10:00:00'),
(4, 'demo001', 1, 3, '2026-06-16 10:00:00', '2026-06-23 10:00:00', '2026-05-19 12:00:00', '2026-06-16 10:00:00'),
(5, 'demo001', 0, 2, '2026-06-20 10:00:00', '2026-06-22 10:00:00', '2026-05-30 12:00:00', '2026-06-20 10:00:00'),
(6, 'demo001', 1, 2, '2026-06-15 10:00:00', '2026-06-22 10:00:00', '2026-06-01 12:00:00', '2026-06-15 10:00:00'),
(7, 'demo001', 1, 1, '2026-06-13 10:00:00', '2026-06-20 10:00:00', '2026-06-07 12:00:00', '2026-06-13 10:00:00'),
(8, 'demo001', 0, 1, '2026-06-19 10:00:00', '2026-06-21 10:00:00', '2026-06-14 12:00:00', '2026-06-19 10:00:00'),
(9, 'demo001', 1, 2, '2026-06-14 10:00:00', '2026-06-21 10:00:00', '2026-06-08 12:00:00', '2026-06-14 10:00:00'),
(10, 'demo001', 1, 1, '2026-06-20 10:00:00', '2026-06-27 10:00:00', '2026-06-18 12:00:00', '2026-06-20 10:00:00');

-- ==================== 5. AI对话历史 ====================
INSERT IGNORE INTO `assistant_conversation` (`conversation_id`, `user_id`, `title`, `message_count`, `status`, `gmt_create`, `gmt_modified`)
VALUES
('conv-demo-001', 'demo001', '深入了解ConcurrentHashMap原理', 8, 'ACTIVE', '2026-05-25 14:00:00', '2026-05-25 15:30:00'),
('conv-demo-002', 'demo001', '线程池参数调优疑问', 12, 'ACTIVE', '2026-06-03 09:00:00', '2026-06-03 10:45:00'),
('conv-demo-003', 'demo001', 'JVM的GC选择：CMS还是G1？', 6, 'ACTIVE', '2026-06-09 16:00:00', '2026-06-09 17:00:00'),
('conv-demo-004', 'demo001', 'Spring循环依赖三级缓存原理', 10, 'ACTIVE', '2026-06-17 11:00:00', '2026-06-17 12:20:00'),
('conv-demo-005', 'demo001', 'MySQL索引优化explain解读', 15, 'ACTIVE', '2026-06-18 14:00:00', '2026-06-18 16:30:00');

INSERT IGNORE INTO `assistant_message` (`conversation_id`, `role`, `content`, `gmt_create`)
VALUES
('conv-demo-001', 'user', '能帮我讲讲ConcurrentHashMap在JDK8中的实现吗？为什么JDK7到JDK8要改？', '2026-05-25 14:00:00'),
('conv-demo-001', 'assistant', 'JDK8的ConcurrentHashMap采用了CAS+synchronized的方式，抛弃了JDK7的分段锁（Segment）。主要改变有：\n1. 使用CAS初始化数组\n2. 使用synchronized锁住链表头节点\n3. 引入红黑树优化查询\n\n这样做的好处是锁粒度更细，从Segment级别降到了链表头节点级别，并发性能更好。', '2026-05-25 14:01:00'),
('conv-demo-001', 'user', '好的，那put流程具体是怎样的？', '2026-05-25 14:05:00'),
('conv-demo-001', 'assistant', 'put流程大致步骤：\n1. 计算key的hash\n2. 如果数组为空，CAS初始化\n3. 如果目标位置为空，CAS直接放入\n4. 如果正在扩容，帮助扩容\n5. 否则synchronized锁住头节点，遍历链表/红黑树插入\n6. 链表长度超过8，转为红黑树\n7. 检查是否需要扩容\n\n关键优化：插入时锁的只是当前桶的头节点，其他桶的插入完全不受影响。', '2026-05-25 14:10:00'),
('conv-demo-001', 'user', '明白了，那size()方法呢？', '2026-05-25 14:20:00'),
('conv-demo-001', 'assistant', 'size()方法在JDK8中使用sumCount()，维护了一个CounterCell数组来分散计数。\n\n大致思路：\n- 低并发时直接用CAS更新baseCount\n- 高并发CAS失败时，使用CounterCell数组分散写入\n- sumCount()时汇总baseCount + CounterCell[]所有值\n\n这是一种"分段计数"的思想，和高并发下对一个计数字段的争抢。这和LongAdder的思路是一样的。', '2026-05-25 14:25:00'),
('conv-demo-002', 'user', '线程池的核心线程数怎么设比较合理？', '2026-06-03 09:00:00'),
('conv-demo-002', 'assistant', '核心线程数的设置取决于任务类型：\n\nCPU密集型：CPU核心数+1（+1是为了补偿页缺失）\nIO密集型：CPU核心数 * (1 + IO耗时/CPU耗时)\n\n比如你的服务80%时间在等待数据库查询，20%在计算，4核CPU下：\n核心线程数 = 4 * (1 + 80/20) = 20\n\n不过实际建议压测确定，公式只是起点。', '2026-06-03 09:02:00'),
('conv-demo-002', 'user', '我的服务主要是处理HTTP请求，中间会查数据库和调外部API，算是IO密集型吧？', '2026-06-03 09:10:00'),
('conv-demo-002', 'assistant', '对，属于IO密集型。如果数据库查询和外部API调用的耗时占比高，核心线程数可以设大一些。\n\n但要注意几个限制：\n1. 数据库连接池大小——线程数不能超过连接池上限\n2. 外部API的QPS限制——别把别人打挂了\n3. 上下文切换开销——线程太多反而变慢\n\n经验值：4核机器可以先设20~30，压测观察CPU和TP99再调。', '2026-06-03 09:15:00'),
('conv-demo-002', 'user', '队列选有界还是无界？', '2026-06-03 09:30:00'),
('conv-demo-002', 'assistant', '强烈建议用有界队列！无界队列可能导致内存溢出。\n\n常见的组合：\n- 有界队列 + 合理的拒绝策略\n- 默认AbortPolicy会抛异常，可以换成CallerRunsPolicy（让提交任务的线程自己跑）或自定义策略\n- 队列大小根据系统的吞吐量和响应时间要求来定', '2026-06-03 09:35:00'),
('conv-demo-003', 'user', '我现在有个应用，堆内存16G，要求GC停顿不超过200ms，用CMS还是G1？', '2026-06-09 16:00:00'),
('conv-demo-003', 'assistant', '16G堆且要求200ms内，建议选G1。原因：\n\n1. CMS在堆大于8G时，并发标记和重新标记阶段耗时明显增加\n2. CMS有并发失败（Concurrent Mode Failure）的风险，一旦触发就退化为Serial Old，停顿秒级\n3. G1通过Region划分和可预测的停顿时间模型，更容易控制停顿\n\n建议配置：\n-XX:+UseG1GC\n-XX:MaxGCPauseMillis=150\n-XX:InitiatingHeapOccupancyPercent=45', '2026-06-09 16:05:00'),
('conv-demo-003', 'user', 'G1的停顿时间设置得越短越好吗？', '2026-06-09 16:30:00'),
('conv-demo-003', 'assistant', '不是越短越好！\n\n-XX:MaxGCPauseMillis 只是一个"目标值"，G1会尽量达到，但不是硬保证。\n\n如果设得太小（比如50ms），G1会每次只回收很少的Region，导致：\n1. GC频率变高，总暂停时间反而增加\n2. 吞吐量下降（因为频繁做GC上下文切换）\n3. 可能跟不上对象分配速度，导致Full GC\n\n建议从150ms开始调，观察后逐步调整。', '2026-06-09 16:35:00'),
('conv-demo-005', 'user', '我有个SQL很慢，explain显示type=ALL，Extra=Using filesort，怎么优化？', '2026-06-18 14:00:00'),
('conv-demo-005', 'assistant', 'type=ALL是全表扫描，Using filesort是文件排序（不是在内存中完成的排序）。\n\n两个问题需要一起看：\n\n1. 全表扫描 → 检查WHERE条件，看是否能加索引\n2. 文件排序 → 检查ORDER BY字段，确保有合适的索引覆盖排序\n\n如果ORDER BY和WHERE条件中的字段能建联合索引，可以同时解决两个问题——因为B+树索引本身是有序的。', '2026-06-18 14:05:00'),
('conv-demo-005', 'user', 'SELECT * FROM orders WHERE status = 1 ORDER BY create_time DESC LIMIT 100', '2026-06-18 14:10:00'),
('conv-demo-005', 'assistant', '这个SQL可以建一个联合索引：\n\nALTER TABLE orders ADD INDEX idx_status_create (status, create_time);\n\n为什么生效：\n1. status条件走索引过滤\n2. create_time利用B+树的有序性，避免filesort\n3. 索引下推（ICP）还能进一步过滤\n\n额外建议：\n- 不要用SELECT *，只查需要的字段\n- 如果数据量大且offset大，需要"延迟关联"优化', '2026-06-18 14:15:00');

-- ==================== 6. 学习报告 ====================
INSERT IGNORE INTO `learning_report` (`user_id`, `report_type`, `report_data`, `period_start`, `period_end`, `gmt_create`)
VALUES
('demo001', 'WEEKLY', '{"totalQuestions":48,"correctCount":32,"accuracy":66.7,"studyHours":12.5,"streak":7,"topicsCovered":["Java基础","集合框架","并发编程"],"weakAreas":["JVM调优"],"strongAreas":["集合框架","Java基础"],"trend":"up"}', '2026-05-16', '2026-05-22', '2026-05-22 23:59:59'),
('demo001', 'WEEKLY', '{"totalQuestions":61,"correctCount":45,"accuracy":73.8,"studyHours":14.0,"streak":7,"topicsCovered":["集合框架","并发编程"],"weakAreas":["ConcurrentHashMap"],"strongAreas":["集合框架"],"trend":"up"}', '2026-05-23', '2026-05-29', '2026-05-29 23:59:59'),
('demo001', 'WEEKLY', '{"totalQuestions":88,"correctCount":65,"accuracy":73.9,"studyHours":18.5,"streak":7,"topicsCovered":["并发编程","JVM"],"weakAreas":["AQS原理"],"strongAreas":["线程池","synchronized"],"trend":"stable"}', '2026-05-30', '2026-06-05', '2026-06-05 23:59:59'),
('demo001', 'WEEKLY', '{"totalQuestions":61,"correctCount":45,"accuracy":73.8,"studyHours":16.5,"streak":7,"topicsCovered":["JVM","Spring"],"weakAreas":["G1调优参数"],"strongAreas":["类加载机制","内存区域"],"trend":"up"}', '2026-06-06', '2026-06-12', '2026-06-12 23:59:59'),
('demo001', 'WEEKLY', '{"totalQuestions":69,"correctCount":49,"accuracy":71.0,"studyHours":20.0,"streak":7,"topicsCovered":["Spring","数据库"],"weakAreas":["事务传播行为"],"strongAreas":["IoC容器","AOP"],"trend":"stable"}', '2026-06-13', '2026-06-19', '2026-06-19 23:59:59');

-- ==================== 7. 通知配置 ====================
INSERT IGNORE INTO `user_notification_config` (`user_id`, `email_notifications`, `email_address`, `push_notifications`, `review_reminder`, `report_weekly`, `report_monthly`, `gmt_create`, `gmt_modified`)
VALUES ('demo001', 1, 'demo@cortex.com', 0, 1, 1, 1, '2026-03-01 10:00:00', '2026-06-01 10:00:00');

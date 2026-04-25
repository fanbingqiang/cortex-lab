# Cortex 多智能体协作框架 - 项目规划

## 一、项目定位

### 1.1 核心价值
**轻量级、透明可控、可进化的多智能体协作框架**

与Claude Code等产品的差异化：
- **透明性**：用户能看到每个Agent的决策过程和执行细节
- **可控性**：用户可以干预、调整Agent的行为
- **进化性**：记住用户的性格特点和工作习惯，越用越顺手
- **轻量级**：不追求"全自动化"，而是"人机协作"

### 1.2 目标场景
- **比赛场景**：展示多智能体协作的创新性
- **企业场景**：可扩展为内部工具，支持私有化部署

---

## 二、技术选型

### 2.1 大模型API

| 用途 | 模型 | 价格 | 选择理由 |
|------|------|------|----------|
| 主力对话/推理 | DeepSeek-V3.2 | 输入¥0.2-2/百万tokens，输出¥3/百万tokens | 国产最便宜，支持128K上下文，支持Tool Calls |
| 图像理解 | GPT-4o-mini | 输入$0.15/百万tokens，输出$0.6/百万tokens | 识图性价比最高，比GPT-4o便宜10倍 |

**成本估算**：
- 纯文本对话：DeepSeek约¥0.5-3/百万tokens
- 图像理解：GPT-4o-mini约$0.01-0.05/张图

### 2.2 后端技术栈

| 组件 | 技术选型 | 版本 | 理由 |
|------|----------|------|------|
| 框架 | Spring Boot | 3.2+ | 主流、生态完善、企业认可度高 |
| ORM | MyBatis-Plus | 3.5+ | 简化CRUD，支持多租户扩展 |
| 数据库 | MySQL | 8.0+ | 主流关系型数据库 |
| 缓存 | Redis | 7.0+ | 会话管理、热点数据缓存 |
| 消息队列 | RabbitMQ（可选） | 3.12+ | 异步任务处理，企业扩展用 |
| 任务调度 | XXL-JOB（可选） | 2.4+ | 定时任务、重试机制 |

### 2.3 前端技术栈（建议）

| 组件 | 技术选型 | 理由 |
|------|----------|------|
| 框架 | Vue 3 + TypeScript | 轻量、易上手 |
| UI库 | Element Plus | 企业级组件库 |
| 状态管理 | Pinia | Vue 3官方推荐 |
| 可视化 | D3.js / X6 | 任务图、流程图展示 |

---

## 三、系统架构

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端展示层                                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐           │
│  │ 任务面板  │ │ Agent管理│ │ 可观测面板│ │ 用户画像 │           │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘           │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        API网关层                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │ 统一入口 / 认证鉴权 / 限流熔断 / 日志追踪                   │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        核心服务层                               │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │ 任务拆解引擎 │ │ 调度中心    │ │ 执行引擎    │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │ Agent注册中心│ │ 记忆服务    │ │ 提示词管理  │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        Agent执行层                              │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐       │
│  │搜索Agent│ │分析Agent│ │报告Agent│ │代码Agent│ │图像Agent│       │
│  └────────┘ └────────┘ └────────┘ └────────┘ └────────┘       │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        基础设施层                               │
│  ┌────────┐ ┌────────┐ ┌────────┐ ┌────────┐                  │
│  │ MySQL  │ │ Redis  │ │ LLM API│ │ 文件存储│                  │
│  └────────┘ └────────┘ └────────┘ └────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 核心模块职责

| 模块 | 职责 | 关键类/接口 |
|------|------|-------------|
| 任务拆解引擎 | 将用户需求拆解为任务图 | `TaskDecomposer`, `TaskNode`, `TaskGraph` |
| Agent注册中心 | 管理Agent元数据，支持热插拔 | `AgentRegistry`, `AgentMetadata` |
| 调度中心 | 根据任务特征匹配Agent | `TaskDispatcher`, `AgentMatcher` |
| 执行引擎 | 按依赖关系执行任务 | `TaskExecutor`, `ExecutionContext` |
| 记忆服务 | 存储用户画像、工作习惯 | `MemoryService`, `UserProfile` |
| 提示词管理 | 动态组装提示词，注入记忆 | `PromptManager`, `PromptTemplate` |

---

## 四、核心模块详细设计

### 4.1 任务拆解引擎

**输入**：用户自然语言需求
**输出**：有向无环图(DAG)形式的任务图

```
用户输入: "帮我分析竞品A的市场策略，生成一份报告"

任务图:
┌─────────────┐
│ 搜索竞品信息 │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌─────────────┐
│ 分析产品定位 │────▶│ 分析营销策略 │
└─────────────┘     └──────┬──────┘
                           │
                           ▼
                    ┌─────────────┐
                    │ 生成分析报告 │
                    └─────────────┘
```

**实现要点**：
1. 使用DeepSeek的推理能力进行任务拆解
2. 提示词模板化，引导模型输出结构化JSON
3. 支持用户手动调整任务图

### 4.2 Agent注册与发现

**Agent元数据结构**：
```java
public class AgentMetadata {
    private String agentId;           // Agent唯一标识
    private String name;              // Agent名称
    private String description;       // 能力描述
    private List<String> capabilities; // 能力标签 ["search", "analysis", "code"]
    private List<String> inputTypes;   // 接受的输入类型 ["text", "image", "url"]
    private List<String> outputTypes;  // 输出类型 ["text", "json", "file"]
    private Integer priority;          // 优先级
    private Boolean enabled;           // 是否启用
    private String promptTemplate;     // Agent专属提示词模板
}
```

**热插拔实现**：
- Agent配置存数据库，启动时加载到内存
- 提供API动态注册/注销Agent
- 支持Agent版本管理

### 4.3 调度中心

**调度策略**：
1. **能力匹配**：根据任务类型匹配Agent的capabilities
2. **负载均衡**：优先选择空闲的Agent
3. **历史表现**：参考Agent的历史成功率
4. **用户偏好**：用户可指定优先使用的Agent

**RAG匹配（可选增强）**：
- 将任务描述向量化
- 与Agent能力描述做相似度匹配
- 返回Top-K候选Agent

### 4.4 执行引擎

**执行流程**：
```
1. 解析任务图，确定执行顺序（拓扑排序）
2. 并行执行无依赖关系的任务
3. 每个任务执行时：
   a. 从记忆服务获取用户画像
   b. 组装提示词（任务描述 + 用户画像 + 历史上下文）
   c. 调用LLM API
   d. 解析结果，存入上下文
   e. 记录执行日志
4. 所有任务完成后，汇总结果
```

**容错机制**：
- 任务失败自动重试（最多3次）
- 支持断点续执行
- 失败任务可手动触发重新执行

---

## 五、"进化"功能设计

### 5.1 核心思路

**通过提示词注入用户画像，让模型"记住"用户特点**

不需要复杂的训练或微调，只需要：
1. 记录用户的行为数据（关键词级别）
2. 在每次请求时，将用户画像注入提示词
3. 模型根据画像调整输出风格

### 5.2 用户画像结构

```java
public class UserProfile {
    private Long userId;
    
    // 性格特征（关键词）
    private List<String> personalityTags;  
    // 示例: ["注重细节", "喜欢简洁", "技术导向", "数据驱动"]
    
    // 工作习惯（关键词）
    private List<String> workHabits;
    // 示例: ["先看结论", "喜欢表格", "需要代码示例"]
    
    // 踩过的坑（关键词 + 描述）
    private List<MistakeRecord> mistakes;
    // 示例: [{"keyword": "SQL注入", "desc": "上次忘记参数校验导致安全问题"}]
    
    // 偏好设置
    private Map<String, String> preferences;
    // 示例: {"outputFormat": "markdown", "language": "中文"}
}
```

### 5.3 记忆更新机制

**触发时机**：
1. 用户显式反馈（点赞/点踩）
2. 用户修改Agent输出
3. 用户手动添加标签
4. 任务失败后的复盘

**更新策略**：
```java
public void updateProfile(UserAction action) {
    // 1. 提取关键词（调用LLM）
    List<String> keywords = extractKeywords(action.getContent());
    
    // 2. 更新画像
    if (action.isPositive()) {
        // 正向反馈：增强权重
        profile.addTags(keywords);
    } else {
        // 负向反馈：记录为"坑"
        profile.addMistake(keywords, action.getReason());
    }
    
    // 3. 持久化
    userProfileRepository.save(profile);
}
```

### 5.4 提示词注入示例

```
你是一个智能助手，正在为用户"张三"提供服务。

【用户画像】
- 性格特点：注重细节、喜欢简洁、技术导向
- 工作习惯：先看结论、喜欢表格、需要代码示例
- 注意事项：
  * 曾在SQL注入问题上踩坑，请特别注意参数校验
  * 不喜欢冗长的解释，直接给方案

【当前任务】
{task_description}

请根据用户画像调整你的回答风格。
```

---

## 六、数据库设计

### 6.1 核心表结构

```sql
-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255),
    email VARCHAR(100),
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 用户画像表
CREATE TABLE user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    personality_tags JSON,      -- 性格标签
    work_habits JSON,           -- 工作习惯
    preferences JSON,           -- 偏好设置
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_id (user_id)
);

-- 踩坑记录表
CREATE TABLE mistake_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    description TEXT,
    task_id BIGINT,             -- 关联的任务
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Agent注册表
CREATE TABLE agent_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    capabilities JSON,
    input_types JSON,
    output_types JSON,
    priority INT DEFAULT 0,
    enabled TINYINT DEFAULT 1,
    prompt_template TEXT,
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_agent_id (agent_id)
);

-- 任务表
CREATE TABLE task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50) NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(20),         -- PENDING, RUNNING, SUCCESS, FAILED
    task_graph JSON,            -- 任务图结构
    result JSON,                -- 执行结果
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_id (task_id)
);

-- 任务执行日志表
CREATE TABLE task_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50) NOT NULL,
    node_id VARCHAR(50) NOT NULL,
    agent_id VARCHAR(50),
    input JSON,
    output JSON,
    status VARCHAR(20),
    error_message TEXT,
    duration_ms BIGINT,
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- LLM调用记录表（用于成本分析）
CREATE TABLE llm_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(50),
    model VARCHAR(50),
    input_tokens INT,
    output_tokens INT,
    cost DECIMAL(10, 6),
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 七、扩展性设计

### 7.1 多租户支持（企业扩展）

```java
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        TENANT_ID.set(tenantId);
    }
    
    public static Long getTenantId() {
        return TENANT_ID.get();
    }
}
```

- 所有表增加`tenant_id`字段
- MyBatis拦截器自动注入租户条件
- 支持租户级别的Agent配置

### 7.2 插件化Agent扩展

```java
public interface AgentPlugin {
    String getAgentId();
    AgentMetadata getMetadata();
    AgentResult execute(AgentContext context);
}

// Agent加载器
@Component
public class AgentPluginLoader {
    private Map<String, AgentPlugin> plugins = new ConcurrentHashMap<>();
    
    public void register(AgentPlugin plugin) {
        plugins.put(plugin.getAgentId(), plugin);
    }
    
    public AgentPlugin getPlugin(String agentId) {
        return plugins.get(agentId);
    }
}
```

### 7.3 多模型支持

```java
public interface LLMProvider {
    String getName();
    LLMResponse chat(LLMRequest request);
    boolean supportsVision();
}

@Component
public class DeepSeekProvider implements LLMProvider {
    // 实现DeepSeek调用
}

@Component
public class GPT4oMiniProvider implements LLMProvider {
    // 实现GPT-4o-mini调用（用于识图）
}

@Component
public class LLMProviderFactory {
    public LLMProvider getProvider(String modelName) {
        // 根据模型名返回对应的Provider
    }
}
```

---

## 八、需要注意和学习的关键点

### 8.1 技术难点

| 难点 | 解决方案 | 学习资源 |
|------|----------|----------|
| 任务图的正确性校验 | DAG检测算法，循环依赖检测 | 图论基础、拓扑排序 |
| LLM输出结构化 | 提示词工程 + JSON Schema约束 | Prompt Engineering指南 |
| 并发任务执行 | 线程池 + CompletableFuture | Java并发编程实战 |
| 用户画像更新策略 | 增量更新 + 权重衰减 | 推荐系统基础 |
| 成本控制 | Token计数 + 预算限制 | LLM成本优化实践 |

### 8.2 比赛加分项

1. **创新点展示**
   - "进化"机制：通过提示词实现个性化
   - 可视化：任务图动态展示
   - 人机协作：用户可干预每个环节

2. **工程化亮点**
   - 完善的日志和可观测性
   - 优雅的错误处理和重试机制
   - 支持水平扩展的架构设计

3. **演示准备**
   - 准备3个典型场景的演示
   - 展示"进化"效果：同一个任务，不同用户得到不同风格的输出
   - 展示可观测性：任务执行过程可视化

### 8.3 企业扩展注意事项

1. **安全性**
   - API Key加密存储
   - 敏感数据脱敏
   - 操作审计日志

2. **性能优化**
   - 热点数据Redis缓存
   - LLM响应流式输出
   - 数据库读写分离

3. **运维支持**
   - 健康检查接口
   - 监控指标暴露（Prometheus格式）
   - 部署文档和运维手册

---

## 九、实施计划

### 阶段一：基础框架（1-2周）

| 任务 | 产出 |
|------|------|
| 项目初始化 | Spring Boot项目骨架 |
| 数据库设计 | 建表SQL、实体类 |
| Agent注册中心 | Agent元数据管理API |
| 基础Agent实现 | 搜索Agent、分析Agent |

### 阶段二：核心功能（2-3周）

| 任务 | 产出 |
|------|------|
| 任务拆解引擎 | 需求→任务图转换 |
| 调度中心 | Agent匹配和调度 |
| 执行引擎 | 任务执行和结果汇总 |
| 记忆服务 | 用户画像存储和更新 |

### 阶段三：进化功能（1周）

| 任务 | 产出 |
|------|------|
| 提示词管理 | 动态提示词组装 |
| 画像更新机制 | 反馈驱动的画像更新 |
| 效果验证 | 不同用户的差异化输出 |

### 阶段四：前端和优化（1-2周）

| 任务 | 产出 |
|------|------|
| 前端界面 | 任务管理、Agent管理、可观测面板 |
| 可视化 | 任务图展示 |
| 性能优化 | 缓存、异步处理 |

### 阶段五：比赛准备（1周）

| 任务 | 产出 |
|------|------|
| 演示场景 | 3个典型用例 |
| 文档 | README、架构文档 |
| 部署 | 演示环境部署 |

---

## 十、风险和应对

| 风险 | 可能性 | 影响 | 应对措施 |
|------|--------|------|----------|
| LLM API不稳定 | 中 | 高 | 增加重试机制，准备备用模型 |
| 任务拆解不准确 | 高 | 中 | 允许用户手动调整任务图 |
| 成本超预算 | 中 | 中 | 设置Token上限，监控消耗 |
| 进化效果不明显 | 中 | 中 | 增加用户反馈入口，优化画像更新策略 |

---

## 十一、确认事项（已确认）

| 事项 | 确认结果 |
|------|----------|
| 前端技术栈 | Vue 3 + TypeScript |
| 演示场景 | 代码分析、技术方案设计 |
| 时间安排 | 1个月以上（可完整实现） |
| 部署方式 | 当前浏览器调试，后续支持Docker |

---

## 十二、下一步行动

确认计划后，将按以下顺序开始实施：

1. **初始化项目**：Spring Boot 3.2 + MyBatis-Plus + MySQL
2. **创建数据库**：执行建表SQL
3. **实现Agent注册中心**：Agent元数据管理
4. **实现基础Agent**：代码分析Agent、技术方案Agent
5. **实现任务拆解引擎**：需求→任务图
6. **实现记忆服务**：用户画像存储
7. **前端界面**：Vue 3项目初始化

---

*文档版本: v1.1*
*创建时间: 2026-04-04*
*更新时间: 2026-04-04*

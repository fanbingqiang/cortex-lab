# Cortex — Java 学习实验室 & AI 编程导师

基于 Spring Boot 3 + Vue 3 的全栈 Java 学习平台，集成 AI 苏格拉底导师、知识树、陷阱代码练习、社区分享。

## 技术栈

- **后端**: Java 17, Spring Boot 3.2.5, MyBatis-Plus 3.5.5
- **前端**: Vue 3 + TypeScript + CodeMirror 6
- **数据库**: MySQL 8.0 (Docker)
- **AI**: DeepSeek / OpenAI 兼容 API，流式 SSE
- **构建**: Maven, Docker Compose

## 快速启动

```bash
# 1. 配置 API Key
cp .env.example .env
# 编辑 .env 填入你的 LLM_API_KEY

# 2. 一键启动（MySQL + Redis + 应用）
docker compose up --build -d

# 3. 访问
# http://localhost:8081/lab/index.html
# Swagger: http://localhost:8081/swagger-ui.html
```

## 项目结构

```
src/main/java/com/cortex/
├── CortexApplication.java          # 启动入口
├── config/
│   ├── LlmConfig.java              # LLM 客户端配置
│   ├── WebConfig.java              # CORS 配置
│   ├── OpenApiConfig.java          # Swagger/OpenAPI
│   └── GlobalExceptionHandler.java # 全局异常处理
├── auth/
│   ├── JwtAuthFilter.java          # JWT 认证过滤器
│   └── JwtService.java             # JWT 工具
├── engine/
│   ├── CortexTaskExecutor.java     # 多智能体任务执行引擎
│   ├── TaskDecomposer.java         # LLM 任务分解
│   └── TaskEventService.java       # SSE 事件推送
├── llm/
│   ├── LlmClient.java              # OpenAI 兼容 HTTP 客户端（流式）
│   ├── LlmRequest.java             # 请求体构建
│   └── LlmResponse.java            # 响应解析
├── lab/
│   ├── controller/
│   │   ├── LabController.java      # 主控制器
│   │   ├── AuthController.java     # 用户认证
│   │   └── ExportController.java   # 数据导出
│   ├── service/
│   │   ├── AssistantService.java   # AI 助手（流式对话）
│   │   ├── DialogueService.java    # 场景对话
│   │   ├── KnowledgeTreeService.java    # Java 知识树
│   │   ├── KnowledgeCardService.java    # 知识卡片
│   │   ├── QuestionBankService.java     # 题库管理
│   │   ├── CommunityTrapService.java    # 社区陷阱
│   │   ├── SandboxService.java          # 代码沙箱
│   │   ├── SpringProjectService.java    # Spring 项目生成
│   │   ├── PlatformFeatureService.java  # 平台功能调度
│   │   ├── LearningReportService.java   # 学习报告
│   │   ├── TutorService.java            # 导师点评
│   │   ├── ScenarioService.java         # 场景管理
│   │   ├── DiscussionService.java       # 讨论功能
│   │   └── NotificationService.java     # 通知配置
│   ├── dto/                        # 数据传输对象
│   ├── entity/                     # 实体类
│   └── mapper/                     # MyBatis Mapper
├── controller/
│   └── CortexController.java       # 多智能体任务 API
├── service/
│   └── MemoryService.java          # 记忆服务
├── entity/                         # 核心实体
├── mapper/                         # 核心 Mapper
└── util/
    └── JsonUtils.java              # JSON 清理工具

lab-frontend/                       # Vue 3 前端源码
├── src/
│   ├── components/                 # 组件
│   ├── views/                      # 页面
│   ├── stores/                     # Pinia 状态
│   ├── composables/                # 组合式函数
│   └── api/                        # HTTP 客户端
└── ...
```

## 功能模块

### 1. AI 苏格拉底导师
知识树节点生成陷阱代码，流式对话引导学习者发现代码问题，渐进式提示，不直接给答案。

### 2. 知识树
覆盖 Java 核心、集合框架、并发编程、JVM、Spring 等 18 个分类、150+ 知识点节点，每个节点可生成可运行的陷阱代码。

### 3. 代码沙箱 & 黑板
中间面板是代码编辑器（CodeMirror 6），AI 通过 action 把代码加载到黑板，支持编译执行、断点练习。

### 4. 社区陷阱
用户提交踩坑案例，社区投票审核，审核通过同步到场景库，支持 UGC 积累。

### 5. 学习报告 & 复习
AI 生成学习报告，间隔重复复习提醒，薄弱领域分析。

## 设计文档

- [链路测试场景设计](链路测试场景设计.txt) — 6 个全链路集成测试场景

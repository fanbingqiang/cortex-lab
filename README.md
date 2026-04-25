# Cortex 多智能体协作框架 MVP

## 快速开始

### 1. 配置API Key

**方式一：环境变量（推荐）**
```bash
# Windows
set DEEPSEEK_API_KEY=your-api-key-here

# Linux/Mac
export DEEPSEEK_API_KEY=your-api-key-here
```

**方式二：修改配置文件**
编辑 `src/main/resources/application.yml`，将 `your-api-key-here` 替换为你的实际API Key。

### 2. 启动项目

**Windows:**
```bash
start.bat
```

**或手动启动:**
```bash
mvn spring-boot:run
```

### 3. 访问应用

- 主界面: http://localhost:8080
- H2控制台: http://localhost:8080/h2-console (JDBC URL: jdbc:h2:file:./data/cortex)

## 功能说明

### 任务执行
1. 输入用户ID（用于记忆用户画像）
2. 输入任务描述
3. 点击"执行任务"
4. 系统会自动拆解任务并分配给合适的Agent执行

### 用户画像
- 添加性格特点、工作习惯
- 记录踩过的坑
- Agent执行时会参考画像调整输出风格

## API接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /api/task/run | POST | 创建并执行任务 |
| /api/task/create | POST | 仅创建任务 |
| /api/task/execute | POST | 执行已创建的任务 |
| /api/agents | GET | 获取Agent列表 |
| /api/profile/{userId} | GET | 获取用户画像 |
| /api/profile/{userId}/tags | POST | 添加性格标签 |
| /api/profile/{userId}/habits | POST | 添加工作习惯 |
| /api/profile/{userId}/mistakes | POST | 记录踩坑 |

## 项目结构

```
cortex/
├── src/main/java/com/cortex/
│   ├── CortexApplication.java    # 启动类
│   ├── agent/                    # Agent相关
│   ├── config/                   # 配置类
│   ├── controller/               # REST接口
│   ├── dto/                      # 数据传输对象
│   ├── engine/                   # 核心引擎
│   ├── entity/                   # 实体类
│   ├── llm/                      # LLM调用
│   ├── mapper/                   # MyBatis Mapper
│   └── service/                  # 业务服务
├── src/main/resources/
│   ├── application.yml           # 配置文件
│   ├── schema.sql                # 数据库初始化
│   └── static/index.html         # 前端页面
└── pom.xml                       # Maven配置
```

## 注意事项

1. **API Key安全**: 不要将API Key提交到代码仓库
2. **成本控制**: DeepSeek按Token计费，注意监控使用量
3. **数据持久化**: 使用H2文件数据库，数据存储在 `./data/cortex.mv.db`

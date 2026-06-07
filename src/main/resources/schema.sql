-- ==================== 用户 ====================
-- 合并 user_auth + user_learning_profile
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    username VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(200),
    avatar VARCHAR(500),
    role VARCHAR(20) DEFAULT 'USER',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    personality_tags VARCHAR(500),
    work_habits VARCHAR(500),
    preferences VARCHAR(500),
    total_study_hours DOUBLE DEFAULT 0,
    total_questions_answered INT DEFAULT 0,
    total_correct INT DEFAULT 0,
    study_streak INT DEFAULT 0,
    last_study_date DATE,
    weak_areas VARCHAR(1000),
    preferred_direction VARCHAR(500),
    learning_goal VARCHAR(500),
    skill_level VARCHAR(20) DEFAULT 'BEGINNER',
    last_login_time TIMESTAMP,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO user (user_id, username, password_hash, email, role, status)
SELECT 'admin', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'admin@cortex.com', 'ADMIN', 'ACTIVE'
WHERE NOT EXISTS (SELECT 1 FROM user WHERE user_id = 'admin');

-- ==================== 错误记录 ====================
CREATE TABLE IF NOT EXISTS mistake_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    keyword VARCHAR(100) NOT NULL,
    description TEXT,
    task_id VARCHAR(50),
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 多 Agent 引擎 ====================
CREATE TABLE IF NOT EXISTS agent_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    capabilities VARCHAR(500),
    input_types VARCHAR(200),
    output_types VARCHAR(200),
    priority INT DEFAULT 0,
    enabled TINYINT DEFAULT 1,
    prompt_template TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    description TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    task_graph TEXT,
    result TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS task_execution_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL,
    node_id VARCHAR(50) NOT NULL,
    agent_id VARCHAR(50),
    input TEXT,
    output TEXT,
    status VARCHAR(20),
    error_message TEXT,
    duration_ms BIGINT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS llm_call_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(50),
    model VARCHAR(50),
    input_tokens INT,
    output_tokens INT,
    cost DECIMAL(10, 6),
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 实验室 - 陷阱题 ====================
CREATE TABLE IF NOT EXISTS lab_scenario (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    knowledge_point VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    trap_code TEXT NOT NULL,
    expected_pitfall VARCHAR(500),
    correct_explanation TEXT,
    hints TEXT,
    difficulty INT DEFAULT 1,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lab_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(50) NOT NULL UNIQUE,
    scenario_id BIGINT,
    user_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'IN_PROGRESS',
    attempts INT DEFAULT 0,
    chat_history TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 实验室 - 题库 ====================
CREATE TABLE IF NOT EXISTS lab_question_bank (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    trap_code TEXT,
    expected_pitfall VARCHAR(500),
    correct_explanation TEXT,
    hints TEXT,
    category VARCHAR(100),
    difficulty INT DEFAULT 2,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lab_question_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    mastered BOOLEAN DEFAULT FALSE,
    review_count INT DEFAULT 0,
    last_review_time TIMESTAMP,
    next_review_time TIMESTAMP,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lab_knowledge_card (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    title VARCHAR(200),
    key_points TEXT,
    detail_explanation TEXT,
    code_snippet TEXT,
    common_pitfalls TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS lab_discussion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question_id BIGINT NOT NULL,
    parent_id BIGINT,
    user_id VARCHAR(50),
    content TEXT NOT NULL,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 实验室 - 社区陷阱众包 ====================
CREATE TABLE IF NOT EXISTS lab_community_trap (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    knowledge_point VARCHAR(200),
    category VARCHAR(50) DEFAULT 'Java核心',
    java_version VARCHAR(10) DEFAULT '17',
    trap_code TEXT NOT NULL,
    expected_pitfall VARCHAR(500),
    correct_explanation TEXT,
    hints TEXT,
    difficulty INT DEFAULT 2,
    submitter VARCHAR(100) DEFAULT 'anonymous',
    status VARCHAR(20) DEFAULT 'PENDING',
    vote_count INT DEFAULT 0,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 实验室 - 知识树 ====================
CREATE TABLE IF NOT EXISTS lab_tree_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id VARCHAR(100) NOT NULL,
    user_id VARCHAR(50) NOT NULL,
    mastered BOOLEAN DEFAULT FALSE,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (node_id, user_id)
);

-- ==================== 全局 AI 助手 ====================
CREATE TABLE IF NOT EXISTS assistant_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_type VARCHAR(50) DEFAULT 'string',
    description VARCHAR(500),
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assistant_conversation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(50) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(200),
    message_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS assistant_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== RAG 知识块 ====================
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunk_key VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    keywords VARCHAR(500),
    source VARCHAR(100),
    source_id BIGINT,
    weight INT DEFAULT 1,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 反馈与自我进化 ====================
CREATE TABLE IF NOT EXISTS feedback_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id VARCHAR(50),
    message_id BIGINT,
    user_id VARCHAR(50),
    rating INT NOT NULL,
    feedback_type VARCHAR(20),
    comment TEXT,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS evolution_insight (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    insight_key VARCHAR(200) NOT NULL,
    insight_content TEXT NOT NULL,
    category VARCHAR(100),
    confidence DOUBLE DEFAULT 1.0,
    source_count INT DEFAULT 1,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 日常学习记录 ====================
-- 合并 learning_daily_log + 简化 user_learning_profile 的日常部分
CREATE TABLE IF NOT EXISTS daily_learning (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    log_date DATE NOT NULL,
    questions_answered INT DEFAULT 0,
    correct_count INT DEFAULT 0,
    study_minutes INT DEFAULT 0,
    knowledge_points_studied VARCHAR(1000),
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, log_date)
);

CREATE TABLE IF NOT EXISTS learning_report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    report_type VARCHAR(20) NOT NULL COMMENT 'WEEKLY/MONTHLY',
    report_data TEXT NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, report_type, period_start)
);

-- ==================== 通知设置 ====================
CREATE TABLE IF NOT EXISTS user_notification_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    email_notifications BOOLEAN DEFAULT FALSE,
    email_address VARCHAR(200),
    push_notifications BOOLEAN DEFAULT FALSE,
    review_reminder BOOLEAN DEFAULT TRUE,
    report_weekly BOOLEAN DEFAULT TRUE,
    report_monthly BOOLEAN DEFAULT TRUE,
    gmt_create TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    gmt_modified TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ==================== 种子数据 ====================

INSERT INTO agent_metadata (agent_id, name, description, capabilities, input_types, output_types, priority, enabled, prompt_template)
SELECT * FROM (VALUES
ROW('code-analyzer', '代码分析Agent', '分析代码结构、发现潜在问题、提供重构建议', '["code-analysis","bug-detection","refactoring"]', '["text","code"]', '["text","json"]', 1, 1, '你是一个专业的代码分析专家。请分析用户提供的代码，从以下维度进行评估：
1. 代码质量和可读性
2. 潜在的Bug和安全问题
3. 性能优化建议
4. 重构建议

请以结构化的方式输出分析结果。'),

ROW('tech-designer', '技术方案Agent', '根据需求设计技术方案，包括架构设计、技术选型、实现路径', '["architecture","tech-design","solution"]', '["text"]', '["text","json"]', 1, 1, '你是一个资深的技术架构师。请根据用户的需求描述，设计一个完整的技术方案，包括：
1. 需求分析
2. 架构设计
3. 技术选型及理由
4. 实现步骤
5. 潜在风险和应对措施

请以清晰的层次结构输出方案。'),

ROW('search-agent', '搜索Agent', '搜索和收集信息', '["search","information-collection"]', '["text"]', '["text","json"]', 1, 1, '你是一个信息搜索专家。请根据用户的需求，整理相关信息，包括：
1. 关键概念解释
2. 相关技术或工具
3. 最佳实践
4. 参考资源

请以结构化的方式输出搜索结果。'),

ROW('report-generator', '报告生成Agent', '生成结构化的分析报告', '["report","summary","documentation"]', '["text","json"]', '["text","markdown"]', 1, 1, '你是一个专业的技术文档撰写专家。请根据提供的信息，生成一份结构清晰、内容详实的报告，包括：
1. 摘要
2. 详细分析
3. 结论和建议

请使用Markdown格式输出。')
) AS t (agent_id, name, description, capabilities, input_types, output_types, priority, enabled, prompt_template)
WHERE NOT EXISTS (SELECT 1 FROM agent_metadata WHERE agent_metadata.agent_id = t.agent_id);

INSERT INTO assistant_config (config_key, config_value, config_type, description) SELECT * FROM (VALUES
ROW('vendor', 'deepseek', 'string', '模型厂商：deepseek/openai/siliconflow/ollama/custom'),
ROW('api_key', '', 'string', '用户自定义的 API Key（支持任意 OpenAI 兼容厂商）'),
ROW('base_url', 'https://api.deepseek.com', 'string', 'API 接口地址，默认为 DeepSeek，可改为任意 OpenAI 兼容地址'),
ROW('model', 'deepseek-chat', 'string', '使用的 AI 模型名，可任意指定'),
ROW('temperature', '0.7', 'double', 'AI回复的随机性，0-1之间，越高越随机'),
ROW('max_tokens', '2048', 'int', 'AI回复的最大token数'),
ROW('system_prompt', '你是一个智能编程导师"小C"，可以帮助用户解决任何编程问题。回答要简洁有力、有针对性，不要泛泛而谈。当用户的问题不够具体时，主动询问细节。', 'text', '系统提示词'),
ROW('rag_enabled', 'true', 'boolean', '是否启用RAG知识检索'),
ROW('history_enabled', 'true', 'boolean', '是否使用对话历史'),
ROW('evolution_enabled', 'true', 'boolean', '是否启用自我进化功能'),
ROW('max_history_length', '20', 'int', '对话历史最大条数')
) AS t (config_key, config_value, config_type, description)
WHERE NOT EXISTS (SELECT 1 FROM assistant_config WHERE assistant_config.config_key = t.config_key);

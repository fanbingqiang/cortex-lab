-- ============================================================================
-- Cortex2 数据库初始化脚本（MySQL）
-- 用法: mysql -u root -p < init-mysql.sql
-- ============================================================================

-- 创建数据库（库名以项目名 cortex2 开头）
CREATE DATABASE IF NOT EXISTS `cortex2`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE `cortex2`;

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================================================
-- 1. 核心基础表
-- ============================================================================

-- 用户画像
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100) DEFAULT NULL,
    `personality_tags` VARCHAR(500) DEFAULT NULL,
    `work_habits` VARCHAR(500) DEFAULT NULL,
    `preferences` VARCHAR(500) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户画像';

-- 错误记录
CREATE TABLE IF NOT EXISTS `mistake_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `keyword` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `task_id` VARCHAR(50) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_keyword` (`keyword`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错误记录';

-- Agent 元数据
CREATE TABLE IF NOT EXISTS `agent_metadata` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `agent_id` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `capabilities` VARCHAR(500) DEFAULT NULL,
    `input_types` VARCHAR(200) DEFAULT NULL,
    `output_types` VARCHAR(200) DEFAULT NULL,
    `priority` INT NOT NULL DEFAULT 0,
    `enabled` TINYINT NOT NULL DEFAULT 1,
    `prompt_template` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    KEY `idx_priority` (`priority`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent 元数据';

-- 任务
CREATE TABLE IF NOT EXISTS `task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(50) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `title` VARCHAR(255) DEFAULT NULL,
    `description` TEXT DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `task_graph` TEXT DEFAULT NULL,
    `result` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_id` (`task_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务';

-- 任务执行日志
CREATE TABLE IF NOT EXISTS `task_execution_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(50) NOT NULL,
    `node_id` VARCHAR(50) NOT NULL,
    `agent_id` VARCHAR(50) DEFAULT NULL,
    `input` TEXT DEFAULT NULL,
    `output` TEXT DEFAULT NULL,
    `status` VARCHAR(20) DEFAULT NULL,
    `error_message` TEXT DEFAULT NULL,
    `duration_ms` BIGINT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志';

-- LLM 调用日志
CREATE TABLE IF NOT EXISTS `llm_call_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `task_id` VARCHAR(50) DEFAULT NULL,
    `model` VARCHAR(50) DEFAULT NULL,
    `input_tokens` INT DEFAULT NULL,
    `output_tokens` INT DEFAULT NULL,
    `cost` DECIMAL(10,6) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_model` (`model`),
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM 调用日志';

-- ============================================================================
-- 2. 实验室 / 学习模块
-- ============================================================================

-- 实验室场景（陷阱题目）
CREATE TABLE IF NOT EXISTS `lab_scenario` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `node_id` VARCHAR(100) DEFAULT NULL COMMENT '知识树节点ID，用于缓存命中',
    `knowledge_point` VARCHAR(200) NOT NULL,
    `category` VARCHAR(100) DEFAULT NULL,
    `trap_code` TEXT NOT NULL,
    `expected_pitfall` VARCHAR(500) DEFAULT NULL,
    `correct_explanation` TEXT DEFAULT NULL,
    `hints` TEXT DEFAULT NULL,
    `difficulty` INT NOT NULL DEFAULT 1,
    `type` VARCHAR(20) DEFAULT NULL COMMENT 'null=trap, concept, command, algorithm',
    `generated_content` TEXT DEFAULT NULL COMMENT '非陷阱类型的完整内容',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_id` (`node_id`),
    KEY `idx_knowledge_point` (`knowledge_point`),
    KEY `idx_category` (`category`),
    KEY `idx_difficulty` (`difficulty`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室场景（陷阱题目）';

-- 实验室会话
CREATE TABLE IF NOT EXISTS `lab_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(50) NOT NULL,
    `scenario_id` BIGINT DEFAULT NULL,
    `user_id` VARCHAR(50) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    `attempts` INT NOT NULL DEFAULT 0,
    `chat_history` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_scenario_id` (`scenario_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='实验室会话';

-- 题库
CREATE TABLE IF NOT EXISTS `lab_question_bank` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `trap_code` TEXT DEFAULT NULL,
    `expected_pitfall` VARCHAR(500) DEFAULT NULL,
    `correct_explanation` TEXT DEFAULT NULL,
    `hints` TEXT DEFAULT NULL,
    `category` VARCHAR(100) DEFAULT NULL,
    `difficulty` INT NOT NULL DEFAULT 2,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_category` (`category`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题库';

-- 题目进度
CREATE TABLE IF NOT EXISTS `lab_question_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `mastered` TINYINT NOT NULL DEFAULT 0,
    `review_count` INT NOT NULL DEFAULT 0,
    `last_review_time` TIMESTAMP NULL DEFAULT NULL,
    `next_review_time` TIMESTAMP NULL DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_question_user` (`question_id`, `user_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_next_review` (`next_review_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目进度';

-- 知识卡片
CREATE TABLE IF NOT EXISTS `lab_knowledge_card` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `title` VARCHAR(200) DEFAULT NULL,
    `key_points` TEXT DEFAULT NULL,
    `detail_explanation` TEXT DEFAULT NULL,
    `code_snippet` TEXT DEFAULT NULL,
    `common_pitfalls` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识卡片';

-- 讨论
CREATE TABLE IF NOT EXISTS `lab_discussion` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `question_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT NULL,
    `user_id` VARCHAR(50) DEFAULT NULL,
    `content` TEXT NOT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='讨论';

-- 社区陷阱众包
CREATE TABLE IF NOT EXISTS `lab_community_trap` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `knowledge_point` VARCHAR(200) DEFAULT NULL,
    `category` VARCHAR(50) DEFAULT 'Java核心',
    `java_version` VARCHAR(10) DEFAULT '17',
    `trap_code` TEXT NOT NULL,
    `expected_pitfall` VARCHAR(500) DEFAULT NULL,
    `correct_explanation` TEXT DEFAULT NULL,
    `hints` TEXT DEFAULT NULL,
    `difficulty` INT NOT NULL DEFAULT 2,
    `submitter` VARCHAR(100) DEFAULT 'anonymous',
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `vote_count` INT NOT NULL DEFAULT 0,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_category` (`category`),
    KEY `idx_difficulty` (`difficulty`),
    KEY `idx_submitter` (`submitter`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社区陷阱众包';

-- 知识树节点进度
CREATE TABLE IF NOT EXISTS `lab_tree_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `node_id` VARCHAR(100) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `mastered` TINYINT NOT NULL DEFAULT 0,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_node_user` (`node_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识树节点进度';

-- ============================================================================
-- 3. AI 助手模块
-- ============================================================================

-- 助手配置
CREATE TABLE IF NOT EXISTS `assistant_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_key` VARCHAR(100) NOT NULL,
    `config_value` TEXT DEFAULT NULL,
    `config_type` VARCHAR(50) DEFAULT 'string',
    `description` VARCHAR(500) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助手配置';

-- 助手会话
CREATE TABLE IF NOT EXISTS `assistant_conversation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(50) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) DEFAULT NULL,
    `message_count` INT NOT NULL DEFAULT 0,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_id` (`conversation_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助手会话';

-- 助手消息
CREATE TABLE IF NOT EXISTS `assistant_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `conversation_id` VARCHAR(50) NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    `content` TEXT NOT NULL,
    `metadata` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conversation_id` (`conversation_id`),
    KEY `idx_role` (`role`),
    KEY `idx_gmt_create` (`gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='助手消息';


-- ============================================================================
-- 4. 组织 / 企业版
-- ============================================================================

-- 系统组织
CREATE TABLE IF NOT EXISTS `sys_organization` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `org_id` VARCHAR(50) NOT NULL,
    `org_name` VARCHAR(200) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `plan_type` VARCHAR(20) NOT NULL DEFAULT 'FREE',
    `max_members` INT NOT NULL DEFAULT 10,
    `contact_email` VARCHAR(200) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_id` (`org_id`),
    KEY `idx_status` (`status`),
    KEY `idx_plan_type` (`plan_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统组织';

-- 系统用户
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password_hash` VARCHAR(255) DEFAULT NULL,
    `display_name` VARCHAR(200) DEFAULT NULL,
    `email` VARCHAR(200) DEFAULT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    `org_id` VARCHAR(50) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `last_login_time` TIMESTAMP NULL DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_role` (`role`),
    KEY `idx_status` (`status`),
    KEY `idx_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户';

-- 组织学习记录
CREATE TABLE IF NOT EXISTS `org_learning_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `org_id` VARCHAR(50) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `record_date` DATE NOT NULL,
    `questions_answered` INT NOT NULL DEFAULT 0,
    `questions_correct` INT NOT NULL DEFAULT 0,
    `code_executions` INT NOT NULL DEFAULT 0,
    `scenarios_completed` INT NOT NULL DEFAULT 0,
    `cards_viewed` INT NOT NULL DEFAULT 0,
    `chat_messages` INT NOT NULL DEFAULT 0,
    `study_duration_minutes` INT NOT NULL DEFAULT 0,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_org_user_date` (`org_id`, `user_id`, `record_date`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织学习记录';

-- 组织培训报告
CREATE TABLE IF NOT EXISTS `org_training_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `report_id` VARCHAR(50) NOT NULL,
    `org_id` VARCHAR(50) NOT NULL,
    `report_type` VARCHAR(20) NOT NULL,
    `report_period_start` DATE NOT NULL,
    `report_period_end` DATE NOT NULL,
    `generated_by` VARCHAR(50) DEFAULT NULL,
    `total_members` INT NOT NULL DEFAULT 0,
    `active_members` INT NOT NULL DEFAULT 0,
    `total_questions` INT NOT NULL DEFAULT 0,
    `avg_accuracy` DOUBLE NOT NULL DEFAULT 0,
    `top_students` TEXT DEFAULT NULL,
    `weak_topics` TEXT DEFAULT NULL,
    `summary_content` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_id` (`report_id`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_period_start` (`report_period_start`),
    KEY `idx_period_end` (`report_period_end`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='组织培训报告';

-- API 密钥
CREATE TABLE IF NOT EXISTS `sys_api_key` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `api_key` VARCHAR(100) NOT NULL,
    `org_id` VARCHAR(50) NOT NULL,
    `key_name` VARCHAR(200) DEFAULT NULL,
    `permission_scope` VARCHAR(200) DEFAULT NULL,
    `allowed_ips` TEXT DEFAULT NULL,
    `expires_at` TIMESTAMP NULL DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `last_used_at` TIMESTAMP NULL DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key` (`api_key`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expires_at` (`expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API 密钥';

-- ============================================================================
-- 5. 面试模块
-- ============================================================================

-- 面试配置
CREATE TABLE IF NOT EXISTS `interview_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `config_id` VARCHAR(50) NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `description` TEXT DEFAULT NULL,
    `question_ids` TEXT DEFAULT NULL,
    `question_count` INT NOT NULL DEFAULT 5,
    `time_limit_minutes` INT NOT NULL DEFAULT 30,
    `difficulty_range` VARCHAR(50) DEFAULT NULL,
    `categories` VARCHAR(500) DEFAULT NULL,
    `passing_score` INT NOT NULL DEFAULT 60,
    `created_by` VARCHAR(50) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_id` (`config_id`),
    KEY `idx_created_by` (`created_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试配置';

-- 面试会话
CREATE TABLE IF NOT EXISTS `interview_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(50) NOT NULL,
    `config_id` VARCHAR(50) DEFAULT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    `current_question_index` INT NOT NULL DEFAULT 0,
    `total_questions` INT NOT NULL DEFAULT 0,
    `time_limit_minutes` INT NOT NULL DEFAULT 30,
    `started_at` TIMESTAMP NULL DEFAULT NULL,
    `completed_at` TIMESTAMP NULL DEFAULT NULL,
    `total_score` INT NOT NULL DEFAULT 0,
    `max_score` INT NOT NULL DEFAULT 100,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_session_id` (`session_id`),
    KEY `idx_config_id` (`config_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试会话';

-- 面试回答
CREATE TABLE IF NOT EXISTS `interview_answer` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` VARCHAR(50) NOT NULL,
    `question_id` BIGINT NOT NULL,
    `question_index` INT NOT NULL,
    `user_code` TEXT DEFAULT NULL,
    `is_correct` TINYINT DEFAULT NULL,
    `ai_score` INT DEFAULT NULL,
    `ai_comment` TEXT DEFAULT NULL,
    `time_spent_seconds` INT DEFAULT NULL,
    `started_at` TIMESTAMP NULL DEFAULT NULL,
    `submitted_at` TIMESTAMP NULL DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_question_id` (`question_id`),
    KEY `idx_question_index` (`question_index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试回答';

-- 面试报告
CREATE TABLE IF NOT EXISTS `interview_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `report_id` VARCHAR(50) NOT NULL,
    `session_id` VARCHAR(50) NOT NULL,
    `user_id` VARCHAR(50) NOT NULL,
    `total_score` INT NOT NULL,
    `max_score` INT NOT NULL,
    `accuracy` DOUBLE DEFAULT NULL,
    `total_time_seconds` INT DEFAULT NULL,
    `completed_questions` INT DEFAULT NULL,
    `avg_score_per_question` DOUBLE DEFAULT NULL,
    `strength_areas` TEXT DEFAULT NULL,
    `weak_areas` TEXT DEFAULT NULL,
    `ai_summary` TEXT DEFAULT NULL,
    `suggestions` TEXT DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_report_id` (`report_id`),
    KEY `idx_session_id` (`session_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试报告';

-- 面试候选人
CREATE TABLE IF NOT EXISTS `interview_candidate` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `candidate_id` VARCHAR(50) NOT NULL,
    `org_id` VARCHAR(50) NOT NULL,
    `name` VARCHAR(100) DEFAULT NULL,
    `email` VARCHAR(200) DEFAULT NULL,
    `phone` VARCHAR(50) DEFAULT NULL,
    `resume_url` VARCHAR(500) DEFAULT NULL,
    `position` VARCHAR(200) DEFAULT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    `interview_session_id` VARCHAR(50) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_candidate_id` (`candidate_id`),
    KEY `idx_org_id` (`org_id`),
    KEY `idx_status` (`status`),
    KEY `idx_position` (`position`),
    KEY `idx_interview_session_id` (`interview_session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试候选人';

-- ============================================================================
-- 6. 用户认证与学习
-- ============================================================================

-- 用户表（与 Java User 实体对齐：认证 + 学习画像）
CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `email` VARCHAR(200) DEFAULT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `last_login_time` TIMESTAMP NULL DEFAULT NULL,
    `personality_tags` VARCHAR(500) DEFAULT NULL,
    `work_habits` VARCHAR(500) DEFAULT NULL,
    `preferences` VARCHAR(1000) DEFAULT NULL,
    `total_study_hours` DOUBLE NOT NULL DEFAULT 0,
    `total_questions_answered` INT NOT NULL DEFAULT 0,
    `total_correct` INT NOT NULL DEFAULT 0,
    `study_streak` INT NOT NULL DEFAULT 0,
    `last_study_date` DATE DEFAULT NULL,
    `weak_areas` VARCHAR(1000) DEFAULT NULL,
    `preferred_direction` VARCHAR(500) DEFAULT NULL,
    `learning_goal` VARCHAR(500) DEFAULT NULL,
    `skill_level` VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户（认证+画像）';

-- 每日学习日志（与 Java DailyLearning 实体对齐）
CREATE TABLE IF NOT EXISTS `daily_learning` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `log_date` DATE NOT NULL,
    `questions_answered` INT NOT NULL DEFAULT 0,
    `correct_count` INT NOT NULL DEFAULT 0,
    `study_minutes` INT NOT NULL DEFAULT 0,
    `knowledge_points_studied` VARCHAR(1000) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `log_date`),
    KEY `idx_log_date` (`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日学习日志';

-- 用户认证（预留拆分，当前 Java 使用 user 表）
CREATE TABLE IF NOT EXISTS `user_auth` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `username` VARCHAR(100) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `email` VARCHAR(200) DEFAULT NULL,
    `avatar` VARCHAR(500) DEFAULT NULL,
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    `last_login_time` TIMESTAMP NULL DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户认证';

-- 用户学习画像
CREATE TABLE IF NOT EXISTS `user_learning_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `total_study_hours` DOUBLE NOT NULL DEFAULT 0,
    `total_questions_answered` INT NOT NULL DEFAULT 0,
    `total_correct` INT NOT NULL DEFAULT 0,
    `study_streak` INT NOT NULL DEFAULT 0,
    `last_study_date` DATE DEFAULT NULL,
    `weak_areas` VARCHAR(1000) DEFAULT NULL,
    `preferred_direction` VARCHAR(500) DEFAULT NULL,
    `learning_goal` VARCHAR(500) DEFAULT NULL,
    `skill_level` VARCHAR(20) NOT NULL DEFAULT 'BEGINNER',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户学习画像';

-- 学习报告
CREATE TABLE IF NOT EXISTS `learning_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `report_type` VARCHAR(20) NOT NULL COMMENT 'WEEKLY/MONTHLY',
    `report_data` TEXT NOT NULL,
    `period_start` DATE NOT NULL,
    `period_end` DATE NOT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_period` (`user_id`, `report_type`, `period_start`),
    KEY `idx_report_type` (`report_type`),
    KEY `idx_period_start` (`period_start`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习报告';

-- 学习日志
CREATE TABLE IF NOT EXISTS `learning_daily_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `log_date` DATE NOT NULL,
    `questions_answered` INT NOT NULL DEFAULT 0,
    `correct_count` INT NOT NULL DEFAULT 0,
    `study_minutes` INT NOT NULL DEFAULT 0,
    `knowledge_points_studied` VARCHAR(1000) DEFAULT NULL,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_date` (`user_id`, `log_date`),
    KEY `idx_log_date` (`log_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习日志';

-- 通知设置
CREATE TABLE IF NOT EXISTS `user_notification_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `email_notifications` TINYINT NOT NULL DEFAULT 0,
    `email_address` VARCHAR(200) DEFAULT NULL,
    `push_notifications` TINYINT NOT NULL DEFAULT 0,
    `review_reminder` TINYINT NOT NULL DEFAULT 1,
    `report_weekly` TINYINT NOT NULL DEFAULT 1,
    `report_monthly` TINYINT NOT NULL DEFAULT 1,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知设置';

-- ============================================================================
-- 7. 种子数据
-- ============================================================================

-- 默认助手配置
INSERT IGNORE INTO `assistant_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('vendor', 'deepseek', 'string', '模型厂商：deepseek/openai/siliconflow/ollama/custom'),
('api_key', '', 'string', '用户自定义的 API Key（支持任意 OpenAI 兼容厂商）'),
('base_url', 'https://api.deepseek.com', 'string', 'API 接口地址，默认为 DeepSeek，可改为任意 OpenAI 兼容地址'),
('model', 'deepseek-chat', 'string', '使用的 AI 模型名，可任意指定'),
('temperature', '0.7', 'double', 'AI回复的随机性，0-1之间，越高越随机'),
('max_tokens', '2048', 'int', 'AI回复的最大token数'),
('system_prompt', '你是一个智能编程导师"小C"，可以帮助用户解决任何编程问题。回答要简洁有力、有针对性，不要泛泛而谈。当用户的问题不够具体时，主动询问细节。', 'text', '系统提示词'),
('history_enabled', 'true', 'boolean', '是否使用对话历史'),
('max_history_length', '20', 'int', '对话历史最大条数');

-- 默认组织
INSERT IGNORE INTO `sys_organization` (`org_id`, `org_name`, `description`, `plan_type`, `max_members`)
VALUES ('default-org', '默认组织', '系统默认组织，所有用户归属于此', 'FREE', 100);

-- 默认管理员（密码: admin123，SHA-256 哈希）
INSERT IGNORE INTO `sys_user` (`user_id`, `username`, `password_hash`, `display_name`, `role`, `org_id`)
VALUES ('admin', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', '管理员', 'ROOT', 'default-org');

-- 默认管理员认证
INSERT IGNORE INTO `user_auth` (`user_id`, `username`, `password_hash`, `email`, `role`, `status`)
VALUES ('admin', 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'admin@cortex.com', 'ADMIN', 'ACTIVE');

-- 预置 Agent
INSERT IGNORE INTO `agent_metadata` (`agent_id`, `name`, `description`, `capabilities`, `input_types`, `output_types`, `priority`, `enabled`, `prompt_template`) VALUES
('code-analyzer', '代码分析Agent', '分析代码结构、发现潜在问题、提供重构建议',
 '["code-analysis","bug-detection","refactoring"]', '["text","code"]', '["text","json"]', 1, 1,
 '你是一个专业的代码分析专家。请分析用户提供的代码，从以下维度进行评估：\n1. 代码质量和可读性\n2. 潜在的Bug和安全问题\n3. 性能优化建议\n4. 重构建议\n\n请以结构化的方式输出分析结果。'),
('tech-designer', '技术方案Agent', '根据需求设计技术方案，包括架构设计、技术选型、实现路径',
 '["architecture","tech-design","solution"]', '["text"]', '["text","json"]', 1, 1,
 '你是一个资深的技术架构师。请根据用户的需求描述，设计一个完整的技术方案，包括：\n1. 需求分析\n2. 架构设计\n3. 技术选型及理由\n4. 实现步骤\n5. 潜在风险和应对措施\n\n请以清晰的层次结构输出方案。'),
('search-agent', '搜索Agent', '搜索和收集信息',
 '["search","information-collection"]', '["text"]', '["text","json"]', 1, 1,
 '你是一个信息搜索专家。请根据用户的需求，整理相关信息，包括：\n1. 关键概念解释\n2. 相关技术或工具\n3. 最佳实践\n4. 参考资源\n\n请以结构化的方式输出搜索结果。'),
('report-generator', '报告生成Agent', '生成结构化的分析报告',
 '["report","summary","documentation"]', '["text","json"]', '["text","markdown"]', 1, 1,
 '你是一个专业的技术文档撰写专家。请根据提供的信息，生成一份结构清晰、内容详实的报告，包括：\n1. 摘要\n2. 详细分析\n3. 结论和建议\n\n请使用Markdown格式输出。');

-- 预置陷阱题目（改由 Java BuiltinScenarioSeedService 写入，避免 SQL 文件编码导致乱码）

-- ============================================================================
-- 8. 新学习场景模块
-- ============================================================================

-- 输出预测题
CREATE TABLE IF NOT EXISTS `lab_prediction_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `knowledge_point` VARCHAR(200) NOT NULL,
    `code` TEXT NOT NULL COMMENT '预测用代码',
    `expected_output` TEXT NOT NULL COMMENT '真实输出',
    `explanation` TEXT DEFAULT NULL COMMENT '原理说明',
    `difficulty` INT NOT NULL DEFAULT 2,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_knowledge_point` (`knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='输出预测题';

-- 口语表达评估记录
CREATE TABLE IF NOT EXISTS `lab_expression_assessment` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `knowledge_point` VARCHAR(200) DEFAULT NULL,
    `user_explanation` TEXT NOT NULL,
    `score` INT NOT NULL DEFAULT 0,
    `missing_points` TEXT DEFAULT NULL COMMENT '遗漏的知识点',
    `feedback` TEXT DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_knowledge_point` (`knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='口语表达评估';

-- 干预规则与日志
CREATE TABLE IF NOT EXISTS `lab_intervention_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `error_type` VARCHAR(100) DEFAULT NULL,
    `knowledge_point` VARCHAR(200) DEFAULT NULL,
    `intervention_type` VARCHAR(50) NOT NULL DEFAULT 'REPEATED_ERROR',
    `trigger_count` INT NOT NULL DEFAULT 1,
    `suggestion_data` TEXT DEFAULT NULL COMMENT 'JSON: 建议内容',
    `dismissed` TINYINT NOT NULL DEFAULT 0,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_error_type` (`error_type`),
    KEY `idx_dismissed` (`dismissed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='主动干预日志';

-- 错题变式题
CREATE TABLE IF NOT EXISTS `lab_variant_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `original_question_id` BIGINT NOT NULL,
    `variant_code` TEXT NOT NULL,
    `expected_output` TEXT DEFAULT NULL,
    `explanation` TEXT DEFAULT NULL,
    `knowledge_point` VARCHAR(200) DEFAULT NULL,
    `difficulty` INT NOT NULL DEFAULT 2,
    `user_id` VARCHAR(50) DEFAULT NULL,
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_original_question` (`original_question_id`),
    KEY `idx_knowledge_point` (`knowledge_point`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='变式训练题';

-- 闯关关卡
CREATE TABLE IF NOT EXISTS `lab_level_question` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `knowledge_point` VARCHAR(200) NOT NULL,
    `level_number` INT NOT NULL,
    `question_type` VARCHAR(20) NOT NULL DEFAULT 'FILL' COMMENT 'FILL=填空, TRAP=改错, CODE=从零写',
    `question_data` TEXT NOT NULL COMMENT 'JSON: {title,description,templateCode,expectedAnswer,hints}',
    `unlock_after_level` INT DEFAULT 0 COMMENT '需要通关的前置关卡号,0表示无条件',
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_kp_level` (`knowledge_point`, `level_number`),
    KEY `idx_knowledge_point` (`knowledge_point`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关卡';

-- 闯关进度
CREATE TABLE IF NOT EXISTS `lab_level_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` VARCHAR(50) NOT NULL,
    `level_id` BIGINT NOT NULL,
    `completed` TINYINT NOT NULL DEFAULT 0,
    `attempts` INT NOT NULL DEFAULT 0,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_level` (`user_id`, `level_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='闯关进度';

-- 知识点关联图谱
CREATE TABLE IF NOT EXISTS `knowledge_relation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `source_node` VARCHAR(100) NOT NULL,
    `target_node` VARCHAR(100) NOT NULL,
    `relation_type` VARCHAR(50) NOT NULL DEFAULT 'RELATED' COMMENT 'RELATED, PREREQUISITE, EXTENDS, CONTRAST',
    `weight` INT NOT NULL DEFAULT 1,
    `gmt_create` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_edge` (`source_node`, `target_node`),
    KEY `idx_source` (`source_node`),
    KEY `idx_target` (`target_node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识点关联图谱';

-- 闯关种子数据
INSERT IGNORE INTO `lab_level_question` (`knowledge_point`, `level_number`, `question_type`, `question_data`, `unlock_after_level`) VALUES
('HashMap原理', 1, 'FILL',
 '{"title":"HashMap数据结构","description":"HashMap 在 JDK 8 中使用什么数据结构？","templateCode":"// HashMap 在 JDK 8 中使用的是____ + ____ + 红黑树","expectedAnswer":"数组+链表","hints":["JDK 7 只有数组+链表","JDK 8 引入了红黑树优化"]}', 0),
('HashMap原理', 2, 'TRAP',
 '{"title":"HashMap 扩容陷阱","description":"下面的代码运行时 HashMap 会扩容几次？","templateCode":"import java.util.*;\npublic class HashMapResize {\n    public static void main(String[] args) {\n        HashMap<String,String> map = new HashMap<>();\n        for(int i=0;i<13;i++){\n            map.put(\"key\"+i,\"val\"+i);\n        }\n        System.out.println(\"size: \"+map.size());\n    }\n}","expectedAnswer":"扩容从 16→32 触发一次","hints":["默认初始容量是多少？","加载因子是多少？","什么时候触发扩容？"]}', 1),
('HashMap原理', 3, 'CODE',
 '{"title":"手写简易 HashMap","description":"实现一个简单的 put 和 get 方法，使用数组+链表。","templateCode":"class SimpleHashMap<K,V> {\n    // 请实现 put 和 get\n    // 1. 数组默认容量 16\n    // 2. 使用链表解决哈希冲突\n    // 3. 实现 hash 方法\n}","expectedAnswer":"","hints":["hashCode() 映射到数组下标","链表节点存 key+value+next","get 时遍历链表找 key 相等"]}', 2);

-- 初始知识点关联数据
INSERT IGNORE INTO `knowledge_relation` (`source_node`, `target_node`, `relation_type`, `weight`) VALUES
('collection-hashmap', 'java-basics-equals', 'PREREQUISITE', 3),
('collection-hashmap', 'collection-concurrent-hashmap', 'EXTENDS', 2),
('collection-hashmap', 'collection-treemap', 'CONTRAST', 1),
('collection-concurrent-hashmap', 'concurrency-synchronized', 'PREREQUISITE', 2),
('collection-arraylist', 'collection-hashmap', 'CONTRAST', 1),
('java-basics-string', 'java-basics-equals', 'RELATED', 2),
('java-basics-datatype', 'java-basics-string', 'RELATED', 1),
('exception-try-finally', 'exception-hierarchy', 'EXTENDS', 2),
('concurrency-synchronized', 'concurrency-reentrantlock', 'CONTRAST', 2),
('concurrency-synchronized', 'concurrency-volatile', 'CONTRAST', 1),
('concurrency-threadpool', 'concurrency-completable-future', 'EXTENDS', 2),
('concurrency-threadpool', 'concurrency-deadlock', 'RELATED', 1),
('jvm-memory', 'jvm-gc', 'PREREQUISITE', 3),
('jvm-memory', 'jvm-oom', 'RELATED', 2),
('jvm-gc', 'jvm-collector', 'EXTENDS', 2),
('spring-ioc', 'spring-aop', 'PREREQUISITE', 2),
('spring-ioc', 'spring-trap-circular', 'EXTENDS', 2),
('spring-aop', 'spring-trap-aop', 'EXTENDS', 2),
('spring-transaction', 'spring-trap-transaction', 'EXTENDS', 2),
('database-index', 'database-sql-optimize', 'EXTENDS', 3),
('database-transaction', 'database-lock', 'RELATED', 2),
('concurrency-volatile', 'jvm-memory', 'RELATED', 1);

-- ============================================================================
-- 初始化完成
-- ============================================================================

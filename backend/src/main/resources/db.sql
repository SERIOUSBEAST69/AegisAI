-- TrustAI 数据治理平台核心表 DDL

CREATE TABLE `user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `role_id` BIGINT COMMENT '角色ID',
  `department` VARCHAR(50) COMMENT '部门',
  `phone` VARCHAR(20) COMMENT '联系方式',
  `email` VARCHAR(100) COMMENT '邮箱',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1-正常 0-禁用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_username(`username`),
  INDEX idx_role(`role_id`)
) COMMENT='系统用户表';

CREATE TABLE `role` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '角色ID',
  `name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(200) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='角色表';

CREATE TABLE `permission` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '权限ID',
  `name` VARCHAR(50) NOT NULL COMMENT '权限名称',
  `code` VARCHAR(50) NOT NULL COMMENT '权限编码',
  `type` VARCHAR(20) COMMENT '类型（菜单/按钮/数据）',
  `parent_id` BIGINT DEFAULT NULL COMMENT '父权限ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='权限表';

CREATE TABLE `role_permission` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `role_id` BIGINT NOT NULL,
  `permission_id` BIGINT NOT NULL,
  INDEX idx_role(`role_id`),
  INDEX idx_permission(`permission_id`)
) COMMENT='角色-权限关联表';

CREATE TABLE `data_asset` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '数据资产ID',
  `name` VARCHAR(100) NOT NULL COMMENT '资产名称',
  `type` VARCHAR(50) COMMENT '类型（MySQL/Excel/API等）',
  `sensitivity_level` VARCHAR(20) COMMENT '敏感等级（公开/内部/敏感/受限）',
  `location` VARCHAR(200) COMMENT '存储位置/连接信息',
  `discovery_time` DATETIME COMMENT '发现时间',
  `owner_id` BIGINT COMMENT '负责人ID',
  `lineage` TEXT COMMENT '数据血缘信息（JSON）',
  `description` VARCHAR(200) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_type(`type`),
  INDEX idx_sensitivity(`sensitivity_level`)
) COMMENT='数据资产表';

CREATE TABLE `ai_model` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '模型ID',
  `name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `type` VARCHAR(50) COMMENT '类型（外部API/ONNX/PMML等）',
  `risk_level` VARCHAR(20) COMMENT '风险等级（低/中/高）',
  `endpoint` VARCHAR(200) COMMENT '接入方式/端点',
  `developer` VARCHAR(50) COMMENT '开发者',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1-启用 0-下架',
  `description` VARCHAR(200) COMMENT '描述',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='AI模型表';

CREATE TABLE `audit_log` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '日志ID',
  `user_id` BIGINT COMMENT '用户ID',
  `asset_id` BIGINT COMMENT '资产ID',
  `operation` VARCHAR(50) COMMENT '操作类型',
  `operation_time` DATETIME COMMENT '操作时间',
  `ip` VARCHAR(50) COMMENT 'IP地址',
  `device` VARCHAR(100) COMMENT '设备信息',
  `input_overview` VARCHAR(200) COMMENT '输入概要（脱敏）',
  `output_overview` VARCHAR(200) COMMENT '输出概要（脱敏）',
  `result` VARCHAR(20) COMMENT '结果（成功/失败）',
  `hash` VARCHAR(128) COMMENT '哈希链/签名',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  INDEX idx_user(`user_id`),
  INDEX idx_asset(`asset_id`),
  INDEX idx_time(`operation_time`)
) COMMENT='审计日志表';

CREATE TABLE `approval_request` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '审批单ID',
  `applicant_id` BIGINT COMMENT '申请人ID',
  `asset_id` BIGINT COMMENT '资产ID',
  `reason` VARCHAR(200) COMMENT '申请理由',
  `status` VARCHAR(20) COMMENT '状态（待审批/通过/拒绝）',
  `approver_id` BIGINT COMMENT '审批人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_applicant(`applicant_id`),
  INDEX idx_asset(`asset_id`)
) COMMENT='访问审批单表';

CREATE TABLE `compliance_policy` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '策略ID',
  `name` VARCHAR(100) NOT NULL COMMENT '策略名称',
  `rule_content` TEXT COMMENT '规则内容（JSON/IF-THEN）',
  `scope` VARCHAR(50) COMMENT '生效范围（全局/指定资产/模型）',
  `status` TINYINT DEFAULT 1 COMMENT '状态 1-启用 0-停用',
  `version` INT DEFAULT 1 COMMENT '版本号',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='合规策略表';

CREATE TABLE `risk_event` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '风险事件ID',
  `type` VARCHAR(50) COMMENT '事件类型',
  `level` VARCHAR(20) COMMENT '风险等级',
  `related_log_id` BIGINT COMMENT '关联日志ID',
  `status` VARCHAR(20) COMMENT '状态（待处理/已处理）',
  `handler_id` BIGINT COMMENT '处置人ID',
  `process_log` TEXT COMMENT '处置记录',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX idx_type(`type`),
  INDEX idx_level(`level`)
) COMMENT='风险事件表';

-- 敏感数据扫描任务
CREATE TABLE `sensitive_scan_task` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '任务ID',
  `source_type` VARCHAR(20) COMMENT '来源类型：file/db',
  `source_path` VARCHAR(200) COMMENT '文件路径或库表',
  `status` VARCHAR(20) COMMENT '状态：pending/running/done/failed',
  `sensitive_ratio` DECIMAL(5,2) COMMENT '敏感占比百分比',
  `report_path` VARCHAR(200) COMMENT '报表存储路径',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='敏感数据扫描任务表';

-- AI 调用成本统计
CREATE TABLE `model_call_stat` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `model_id` BIGINT,
  `user_id` BIGINT,
  `date` DATE,
  `call_count` INT DEFAULT 0,
  `total_latency_ms` BIGINT DEFAULT 0,
  `cost_cents` INT DEFAULT 0,
  INDEX idx_model_date(`model_id`, `date`),
  INDEX idx_user_date(`user_id`, `date`)
) COMMENT='模型调用成本统计';

-- 告警闭环记录
CREATE TABLE `alert_record` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `title` VARCHAR(100),
  `level` VARCHAR(20),
  `status` VARCHAR(20) COMMENT 'open/claimed/resolved/archived',
  `assignee_id` BIGINT,
  `related_log_id` BIGINT,
  `resolution` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='告警闭环记录';

-- 数据主体权利申请工单
CREATE TABLE `subject_request` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT,
  `type` VARCHAR(30) COMMENT 'access/export/delete',
  `status` VARCHAR(20) COMMENT 'pending/processing/done/rejected',
  `comment` VARCHAR(200),
  `handler_id` BIGINT,
  `result` TEXT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='数据主体权利申请工单';

-- 数据资产共享审批
CREATE TABLE `data_share_request` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `asset_id` BIGINT,
  `applicant_id` BIGINT,
  `collaborators` VARCHAR(200) COMMENT '协作人ID列表',
  `reason` VARCHAR(200),
  `status` VARCHAR(20) COMMENT 'pending/approved/rejected',
  `approver_id` BIGINT,
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='数据资产共享审批';

-- 脱敏规则
CREATE TABLE `desensitize_rule` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(100),
  `pattern` VARCHAR(100),
  `mask` VARCHAR(20),
  `example` VARCHAR(200),
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='脱敏规则定义';

CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
  `real_name` VARCHAR(50) COMMENT '真实姓名',
  `nickname` VARCHAR(50) COMMENT '昵称',
  `avatar` VARCHAR(255) COMMENT '头像地址',
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

SET @has_sys_user_nickname := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'nickname'
);
SET @sql_add_sys_user_nickname := IF(
  @has_sys_user_nickname = 0,
  'ALTER TABLE sys_user ADD COLUMN nickname VARCHAR(50) COMMENT ''昵称'' AFTER real_name',
  'SELECT 1'
);
PREPARE stmt FROM @sql_add_sys_user_nickname;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_sys_user_avatar := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'sys_user' AND column_name = 'avatar'
);
SET @sql_add_sys_user_avatar := IF(
  @has_sys_user_avatar = 0,
  'ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) COMMENT ''头像地址'' AFTER nickname',
  'SELECT 1'
);
PREPARE stmt FROM @sql_add_sys_user_avatar;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @legacy_user_exists := (
  SELECT COUNT(*)
  FROM information_schema.tables
  WHERE table_schema = DATABASE() AND table_name = 'user'
);

SET @sync_user_sql := IF(
  @legacy_user_exists > 0,
  'INSERT INTO sys_user (id, username, password, real_name, nickname, avatar, role_id, department, phone, email, status, create_time, update_time) '
  'SELECT u.id, u.username, u.password, u.real_name, COALESCE(NULLIF(u.real_name, ""), u.username), NULL, u.role_id, u.department, u.phone, u.email, u.status, u.create_time, u.update_time '
  'FROM user u '
  'INNER JOIN ( '
  '  SELECT username, COALESCE(MAX(CASE WHEN password LIKE ''$2a$%%'' THEN id END), MAX(id)) AS keep_id '
  '  FROM user GROUP BY username '
  ') picked ON picked.keep_id = u.id '
  'LEFT JOIN sys_user s ON s.username = (u.username COLLATE utf8mb4_unicode_ci) '
  'WHERE s.id IS NULL',
  'SELECT 1'
);

PREPARE sync_user_stmt FROM @sync_user_sql;
EXECUTE sync_user_stmt;
DEALLOCATE PREPARE sync_user_stmt;

SET @has_audit_log_risk_level := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'audit_log' AND column_name = 'risk_level'
);
SET @sql_add_audit_log_risk_level := IF(
  @has_audit_log_risk_level = 0,
  'ALTER TABLE audit_log ADD COLUMN risk_level VARCHAR(20) COMMENT ''风险等级'' AFTER result',
  'SELECT 1'
);
PREPARE stmt FROM @sql_add_audit_log_risk_level;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_risk_event_audit_log_ids := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'risk_event' AND column_name = 'audit_log_ids'
);
SET @sql_add_risk_event_audit_log_ids := IF(
  @has_risk_event_audit_log_ids = 0,
  'ALTER TABLE risk_event ADD COLUMN audit_log_ids VARCHAR(500) COMMENT ''关联审计日志ID集合'' AFTER related_log_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql_add_risk_event_audit_log_ids;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `desense_recommend_rule` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `data_category` VARCHAR(50) COMMENT '数据分类：如身份证、银行卡',
  `user_role` VARCHAR(50) COMMENT '调用方角色：如admin/auditor',
  `strategy` VARCHAR(50) COMMENT '策略：mask/hash/tokenize',
  `rule_id` BIGINT COMMENT '关联脱敏规则ID',
  `priority` INT DEFAULT 0 COMMENT '优先级，越小越高',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_cat_role(`data_category`, `user_role`)
) COMMENT='脱敏推荐规则表';

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `config_key` VARCHAR(128) NOT NULL UNIQUE COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `description` VARCHAR(255) COMMENT '配置说明',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='系统配置表';
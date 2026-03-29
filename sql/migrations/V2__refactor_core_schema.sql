-- V2: 重构数据库基础模型
-- 说明：
-- 1) 本脚本给出增量思路，适用于从旧版 network_repair 升级。
-- 2) 新增字段使用 ADD COLUMN，重构字段通过兼容保留 + 新字段迁移，避免一次性破坏。

USE network_repair;

-- =========================
-- 用户、角色、用户角色
-- =========================
CREATE TABLE IF NOT EXISTS `role` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(50) NOT NULL,
  role_status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_by BIGINT NULL,
  update_by BIGINT NULL,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_role_code (role_code)
);

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_by BIGINT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(80) NOT NULL,
  permission_name VARCHAR(100) NOT NULL,
  permission_type VARCHAR(20) NOT NULL DEFAULT 'API',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_permission_code (permission_code)
);

CREATE TABLE IF NOT EXISTS role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 旧表 sys_user 重构为 user（如已存在 user 则跳过）
CREATE TABLE IF NOT EXISTS `user` LIKE sys_user;

ALTER TABLE `user`
  ADD UNIQUE KEY uk_user_employee_no (employee_no);

-- =========================
-- 设备模型
-- =========================
CREATE TABLE IF NOT EXISTS device_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(50) NOT NULL,
  type_name VARCHAR(50) NOT NULL,
  sort_no INT DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_device_type_code (type_code)
);

CREATE TABLE IF NOT EXISTS device LIKE network_device;

ALTER TABLE device
  ADD COLUMN IF NOT EXISTS device_type_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS brand VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS model VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS campus VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS building VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS machine_room VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS office VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS enable_date DATE NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS warranty_expire_date DATE NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS owner_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS owner_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS management_dept VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS last_fault_time DATETIME NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS total_repair_order_count INT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS total_repair_count INT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS fault_reason_stats JSON NULL COMMENT '新增';

-- =========================
-- 工单与维修
-- =========================
ALTER TABLE repair_order
  ADD COLUMN IF NOT EXISTS reporter_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS reporter_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(20) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS reporter_department VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS report_location VARCHAR(150) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS device_code VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS device_name VARCHAR(100) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS device_type VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS fault_type VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS affect_wide_area_network TINYINT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS audit_by_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS audit_by_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS assign_by BIGINT NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS assign_by_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS assign_by_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS assign_maintainer_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS assign_maintainer_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS need_purchase_parts TINYINT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS parts_description VARCHAR(500) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS apply_delay TINYINT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS original_expected_finish_time DATETIME NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS delayed_expected_finish_time DATETIME NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS user_confirm_result VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS remark VARCHAR(500) NULL COMMENT '新增';

ALTER TABLE repair_order_flow
  ADD COLUMN IF NOT EXISTS operation_type VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS operator_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS operator_name VARCHAR(50) NULL COMMENT '新增';

ALTER TABLE repair_record
  ADD COLUMN IF NOT EXISTS repair_order_no VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS device_code VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS maintainer_employee_no VARCHAR(30) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS maintainer_name VARCHAR(50) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS used_parts TINYINT DEFAULT 0 COMMENT '新增',
  ADD COLUMN IF NOT EXISTS used_parts_desc VARCHAR(500) NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS labor_hours INT NULL COMMENT '新增',
  ADD COLUMN IF NOT EXISTS repair_conclusion VARCHAR(500) NULL COMMENT '新增';

CREATE TABLE IF NOT EXISTS repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  user_employee_no VARCHAR(30) NOT NULL,
  confirm_result VARCHAR(30),
  satisfaction_score INT,
  feedback_content VARCHAR(1000),
  confirm_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL
);

-- =========================
-- 公告、日志、附件、字典、三方绑定
-- =========================
CREATE TABLE IF NOT EXISTS announcement LIKE notice;
ALTER TABLE announcement
  ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PUBLISHED',
  ADD COLUMN IF NOT EXISTS sort_no INT DEFAULT 0;

CREATE TABLE IF NOT EXISTS business_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(50) NOT NULL,
  business_no VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  operator_id BIGINT,
  operator_employee_no VARCHAR(30),
  operator_name VARCHAR(50),
  content VARCHAR(1000),
  status VARCHAR(20),
  create_time DATETIME NOT NULL
);

CREATE TABLE IF NOT EXISTS file_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(50) NOT NULL,
  business_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(50),
  file_size BIGINT,
  file_hash VARCHAR(128),
  uploader_id BIGINT,
  uploader_employee_no VARCHAR(30),
  upload_time DATETIME NOT NULL,
  remark VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS third_party_bind (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  user_employee_no VARCHAR(30) NOT NULL,
  provider VARCHAR(30) NOT NULL,
  open_id VARCHAR(100) NOT NULL,
  union_id VARCHAR(100),
  bind_status VARCHAR(20) NOT NULL DEFAULT 'BOUND',
  bind_time DATETIME NOT NULL,
  unbind_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_provider_openid (provider, open_id)
);

CREATE TABLE IF NOT EXISTS dictionary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(50) NOT NULL,
  dict_code VARCHAR(50) NOT NULL,
  dict_label VARCHAR(100) NOT NULL,
  dict_value VARCHAR(100) NOT NULL,
  sort_no INT DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_dict_type_code (dict_type, dict_code)
);

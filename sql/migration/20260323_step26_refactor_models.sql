-- Step26: 数据库设计与基础数据模型重构（在 step2 schema 基础上增量执行）
-- 目标：统一 15 张核心业务表字段、约束、索引，并保证主线代码字段可映射。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================
-- user / role / user_role
-- =========================
ALTER TABLE `user`
  ADD COLUMN IF NOT EXISTS employee_no VARCHAR(30) NULL COMMENT '工号（全局唯一）' AFTER id,
  ADD COLUMN IF NOT EXISTS third_party_bound_flag TINYINT NOT NULL DEFAULT 0 COMMENT '是否绑定第三方账号' AFTER qq_open_id,
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER third_party_bound_flag,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD UNIQUE KEY IF NOT EXISTS uk_user_employee_no (employee_no),
  ADD KEY IF NOT EXISTS idx_user_employee_no (employee_no),
  ADD KEY IF NOT EXISTS idx_user_department_status (department, status),
  ADD KEY IF NOT EXISTS idx_user_last_login_time (last_login_time);

ALTER TABLE `role`
  ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用' AFTER role_status,
  ADD COLUMN IF NOT EXISTS role_desc VARCHAR(500) NULL AFTER role_name,
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER remark,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD KEY IF NOT EXISTS idx_role_status (status);

ALTER TABLE user_role
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER role_id,
  ADD KEY IF NOT EXISTS idx_user_role_user_id (user_id),
  ADD KEY IF NOT EXISTS idx_user_role_role_id (role_id),
  ADD UNIQUE KEY IF NOT EXISTS uk_user_role_user_id_role_id (user_id, role_id);

-- =========================
-- device_type / device
-- =========================
ALTER TABLE device_type
  ADD COLUMN IF NOT EXISTS status TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0禁用' AFTER type_name,
  ADD UNIQUE KEY IF NOT EXISTS uk_device_type_name (type_name),
  ADD KEY IF NOT EXISTS idx_device_type_status_sort (status, sort_no);

ALTER TABLE device
  ADD COLUMN IF NOT EXISTS device_type_name VARCHAR(100) NULL COMMENT '设备类型名称快照' AFTER device_type,
  ADD COLUMN IF NOT EXISTS serial_no VARCHAR(100) NULL COMMENT '序列号（兼容字段）' AFTER serial_number,
  ADD COLUMN IF NOT EXISTS building VARCHAR(100) NULL AFTER building_location,
  ADD COLUMN IF NOT EXISTS machine_room VARCHAR(100) NULL AFTER building,
  ADD COLUMN IF NOT EXISTS office VARCHAR(100) NULL AFTER machine_room,
  ADD COLUMN IF NOT EXISTS owner_user_id BIGINT NULL AFTER owner_name,
  ADD COLUMN IF NOT EXISTS owner_employee_no VARCHAR(30) NULL AFTER owner_user_id,
  ADD COLUMN IF NOT EXISTS management_dept VARCHAR(100) NULL AFTER manage_department,
  ADD COLUMN IF NOT EXISTS total_repair_order_count INT NOT NULL DEFAULT 0 AFTER total_repair_requests,
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER remark,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD UNIQUE KEY IF NOT EXISTS uk_device_device_code (device_code),
  ADD KEY IF NOT EXISTS idx_device_status (status),
  ADD KEY IF NOT EXISTS idx_device_type_status (device_type, status),
  ADD KEY IF NOT EXISTS idx_device_owner_job_no (owner_employee_no),
  ADD KEY IF NOT EXISTS idx_device_location (campus, building_location),
  ADD CONSTRAINT fk_device_owner_user FOREIGN KEY (owner_user_id) REFERENCES `user`(id);

-- =========================
-- repair_order
-- =========================
ALTER TABLE repair_order
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER remark,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD UNIQUE KEY IF NOT EXISTS uk_repair_order_order_no (order_no),
  ADD KEY IF NOT EXISTS idx_repair_order_reporter_job_no (reporter_employee_no),
  ADD KEY IF NOT EXISTS idx_repair_order_status_assign_time (status, assign_maintainer_id, report_time),
  ADD KEY IF NOT EXISTS idx_repair_order_device_status_time (device_id, status, update_time),
  ADD KEY IF NOT EXISTS idx_repair_order_priority_status (priority, status),
  ADD KEY IF NOT EXISTS idx_repair_order_assign_maintainer (assign_maintainer_id);

-- =========================
-- repair_order_flow
-- =========================
ALTER TABLE repair_order_flow
  ADD COLUMN IF NOT EXISTS operation_time DATETIME NULL AFTER operator_role,
  ADD COLUMN IF NOT EXISTS system_recommend_assign_flag TINYINT NOT NULL DEFAULT 0 AFTER operation_type,
  ADD COLUMN IF NOT EXISTS ext_json JSON NULL AFTER system_recommend_assign_flag,
  ADD COLUMN IF NOT EXISTS update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER create_time,
  ADD KEY IF NOT EXISTS idx_repair_order_flow_order_time (repair_order_id, operation_time),
  ADD KEY IF NOT EXISTS idx_repair_order_flow_operator_time (operator_id, operation_time),
  ADD KEY IF NOT EXISTS idx_repair_order_flow_operation_type (operation_type);

UPDATE repair_order_flow SET operation_time = create_time WHERE operation_time IS NULL;

-- =========================
-- repair_record / feedback
-- =========================
ALTER TABLE repair_record
  ADD COLUMN IF NOT EXISTS repair_count_no INT NOT NULL DEFAULT 1 AFTER device_code,
  ADD COLUMN IF NOT EXISTS create_by BIGINT NULL AFTER remark,
  ADD COLUMN IF NOT EXISTS update_by BIGINT NULL AFTER create_by,
  ADD KEY IF NOT EXISTS idx_repair_record_order_id (repair_order_id),
  ADD KEY IF NOT EXISTS idx_repair_record_device_id (device_id),
  ADD KEY IF NOT EXISTS idx_repair_record_maintainer_id (maintainer_id),
  ADD KEY IF NOT EXISTS idx_repair_record_finish_time (finish_time);

ALTER TABLE repair_feedback
  ADD COLUMN IF NOT EXISTS repair_record_id BIGINT NULL AFTER repair_order_id,
  ADD KEY IF NOT EXISTS idx_repair_feedback_order_time (repair_order_id, create_time),
  ADD KEY IF NOT EXISTS idx_repair_feedback_user_time (user_id, create_time),
  ADD CONSTRAINT fk_repair_feedback_record FOREIGN KEY (repair_record_id) REFERENCES repair_record(id);

-- =========================
-- announcement
-- =========================
ALTER TABLE announcement
  ADD COLUMN IF NOT EXISTS expire_time DATETIME NULL AFTER publish_time,
  ADD COLUMN IF NOT EXISTS top_flag TINYINT NOT NULL DEFAULT 0 AFTER publisher_id,
  ADD KEY IF NOT EXISTS idx_announcement_status_publish (status, publish_time),
  ADD KEY IF NOT EXISTS idx_announcement_publisher (publisher_id);

-- =========================
-- operation_log / business_log
-- =========================
ALTER TABLE operation_log
  ADD COLUMN IF NOT EXISTS user_agent VARCHAR(500) NULL AFTER ip,
  ADD COLUMN IF NOT EXISTS result_status VARCHAR(20) NULL AFTER response_code,
  ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000) NULL AFTER result_status,
  ADD COLUMN IF NOT EXISTS cost_ms BIGINT NULL AFTER error_message,
  ADD COLUMN IF NOT EXISTS create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER operation_time,
  ADD KEY IF NOT EXISTS idx_operation_log_module_time (module, operation_time),
  ADD KEY IF NOT EXISTS idx_operation_log_user_time (user_id, operation_time),
  ADD KEY IF NOT EXISTS idx_operation_log_trace_id (trace_id);

ALTER TABLE business_log
  ADD COLUMN IF NOT EXISTS trace_id VARCHAR(64) NULL AFTER id,
  ADD COLUMN IF NOT EXISTS biz_type VARCHAR(64) NULL AFTER business_type,
  ADD COLUMN IF NOT EXISTS biz_id BIGINT NULL AFTER biz_type,
  ADD COLUMN IF NOT EXISTS order_no VARCHAR(50) NULL AFTER biz_id,
  ADD COLUMN IF NOT EXISTS operator_job_no VARCHAR(30) NULL AFTER operator_employee_no,
  ADD COLUMN IF NOT EXISTS operator_role VARCHAR(64) NULL AFTER operator_name,
  ADD COLUMN IF NOT EXISTS ext_json JSON NULL AFTER status,
  ADD COLUMN IF NOT EXISTS operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER ext_json,
  ADD KEY IF NOT EXISTS idx_business_log_biz (biz_type, biz_id, operation_time),
  ADD KEY IF NOT EXISTS idx_business_log_order_time (order_no, operation_time),
  ADD KEY IF NOT EXISTS idx_business_log_operator_time (operator_id, operation_time),
  ADD KEY IF NOT EXISTS idx_business_log_trace_id (trace_id);

UPDATE business_log
SET biz_type = IFNULL(biz_type, business_type),
    order_no = IFNULL(order_no, business_no),
    operator_job_no = IFNULL(operator_job_no, operator_employee_no),
    operation_time = IFNULL(operation_time, create_time)
WHERE 1=1;

-- =========================
-- file_attachment / third_party_bind / dictionary
-- =========================
CREATE TABLE IF NOT EXISTS file_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(32) NULL,
  business_id BIGINT NULL,
  biz_type VARCHAR(32) NULL,
  biz_id BIGINT NULL,
  file_name VARCHAR(255) NOT NULL,
  original_file_name VARCHAR(255) NULL,
  file_path VARCHAR(500) NULL,
  file_url VARCHAR(1000) NOT NULL,
  file_type VARCHAR(100) NULL,
  file_size BIGINT NULL,
  file_hash VARCHAR(128) NULL,
  uploader_id BIGINT NOT NULL,
  uploader_employee_no VARCHAR(30) NULL,
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_file_attachment_biz (biz_type, biz_id),
  KEY idx_file_attachment_legacy_biz (business_type, business_id),
  KEY idx_file_attachment_uploader_time (uploader_id, upload_time),
  CONSTRAINT fk_file_attachment_uploader FOREIGN KEY (uploader_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

ALTER TABLE third_party_bind
  ADD COLUMN IF NOT EXISTS user_employee_no VARCHAR(30) NULL AFTER user_id,
  ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NULL AFTER user_employee_no,
  ADD COLUMN IF NOT EXISTS bind_status VARCHAR(20) NULL AFTER status,
  ADD COLUMN IF NOT EXISTS unbind_time DATETIME NULL AFTER bind_time,
  ADD UNIQUE KEY IF NOT EXISTS uk_third_party_bind_platform_open (platform, open_id),
  ADD KEY IF NOT EXISTS idx_third_party_bind_user_platform (user_id, platform, status);

UPDATE third_party_bind
SET provider = IFNULL(provider, platform),
    bind_status = IFNULL(bind_status, CASE WHEN status = 1 THEN 'BIND' ELSE 'UNBIND' END)
WHERE 1=1;

ALTER TABLE dictionary
  ADD COLUMN IF NOT EXISTS dict_value VARCHAR(255) NULL AFTER dict_label,
  ADD UNIQUE KEY IF NOT EXISTS uk_dictionary_type_code (dict_type, dict_code),
  ADD KEY IF NOT EXISTS idx_dictionary_type_status_sort (dict_type, status, sort_no);

SET FOREIGN_KEY_CHECKS = 1;

-- Step 2: 企业级数据库重构（MySQL 8+）
-- 执行方式：mysql -u用户名 -p 数据库名 < sql/migration/20260322_step2_enterprise_schema.sql
-- 策略：
-- 1) 对与新模型同名的旧表先重命名为 *_legacy（保留旧数据）。
-- 2) 创建新结构表。
-- 3) 将旧数据回填到新表（幂等插入）。

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =========================================================
-- 0) 重命名冲突旧表（仅当存在旧表且不存在目标legacy时）
-- =========================================================
DROP PROCEDURE IF EXISTS sp_rename_if_needed;
DELIMITER $$
CREATE PROCEDURE sp_rename_if_needed(IN old_name VARCHAR(128), IN legacy_name VARCHAR(128))
BEGIN
  IF EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = old_name
  ) AND NOT EXISTS (
      SELECT 1 FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = legacy_name
  ) THEN
      SET @sql = CONCAT('RENAME TABLE `', old_name, '` TO `', legacy_name, '`');
      PREPARE stmt FROM @sql;
      EXECUTE stmt;
      DEALLOCATE PREPARE stmt;
  END IF;
END$$
DELIMITER ;

CALL sp_rename_if_needed('repair_order', 'repair_order_legacy');
CALL sp_rename_if_needed('repair_order_flow', 'repair_order_flow_legacy');
CALL sp_rename_if_needed('repair_record', 'repair_record_legacy');
CALL sp_rename_if_needed('operation_log', 'operation_log_legacy');

DROP PROCEDURE IF EXISTS sp_rename_if_needed;

-- =========================================================
-- 1) dictionary
-- =========================================================
CREATE TABLE IF NOT EXISTS dictionary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(64) NOT NULL,
  dict_code VARCHAR(64) NOT NULL,
  dict_label VARCHAR(128) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_dictionary_type_code (dict_type, dict_code),
  KEY idx_dictionary_type_status_sort (dict_type, status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- =========================================================
-- 2) role / user / user_role
-- =========================================================
CREATE TABLE IF NOT EXISTS `role` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(128) NOT NULL,
  role_desc VARCHAR(500) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_code (role_code),
  KEY idx_role_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_no VARCHAR(30) NOT NULL,
  username VARCHAR(50) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  phone VARCHAR(20) NULL,
  email VARCHAR(100) NULL,
  department VARCHAR(100) NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time DATETIME NULL,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_job_no (job_no),
  UNIQUE KEY uk_user_username (username),
  KEY idx_user_status_deleted (status, deleted),
  KEY idx_user_department (department),
  KEY idx_user_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_role (user_id, role_id),
  KEY idx_user_role_user (user_id),
  KEY idx_user_role_role (role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user`(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES `role`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- =========================================================
-- 3) third_party_bind
-- =========================================================
CREATE TABLE IF NOT EXISTS third_party_bind (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  platform VARCHAR(20) NOT NULL COMMENT 'WECHAT/QQ',
  open_id VARCHAR(128) NULL,
  union_id VARCHAR(128) NULL,
  bind_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_platform_open_id (platform, open_id),
  UNIQUE KEY uk_platform_union_id (platform, union_id),
  KEY idx_bind_user_platform_status (user_id, platform, status),
  CONSTRAINT fk_bind_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方绑定表';

-- =========================================================
-- 4) device_type / device
-- =========================================================
CREATE TABLE IF NOT EXISTS device_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(64) NOT NULL,
  type_name VARCHAR(128) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  sort_no INT NOT NULL DEFAULT 0,
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_type_code (type_code),
  UNIQUE KEY uk_device_type_name (type_name),
  KEY idx_device_type_status_sort (status, sort_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备类型表';

CREATE TABLE IF NOT EXISTS device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_no VARCHAR(50) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  device_type_id BIGINT NULL,
  device_type VARCHAR(64) NULL,
  brand_model VARCHAR(100) NULL,
  ip_address VARCHAR(50) NULL,
  mac_address VARCHAR(50) NULL,
  department_or_location VARCHAR(200) NULL,
  purchase_date DATE NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
  remark VARCHAR(500) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_device_no (device_no),
  KEY idx_device_status (status),
  KEY idx_device_type_id (device_type_id),
  KEY idx_device_location (department_or_location),
  KEY idx_device_update_time (update_time),
  CONSTRAINT fk_device_type FOREIGN KEY (device_type_id) REFERENCES device_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

-- =========================================================
-- 5) repair_order / repair_order_flow
-- =========================================================
CREATE TABLE IF NOT EXISTS repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL,

  repair_user_id BIGINT NOT NULL,
  repair_user_job_no VARCHAR(30) NOT NULL,
  contact_phone VARCHAR(20) NULL,
  department_or_location VARCHAR(200) NULL,

  device_id BIGINT NOT NULL,
  device_no VARCHAR(50) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(64) NULL,

  fault_type VARCHAR(64) NULL,
  fault_desc TEXT NULL,
  urgency_level VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
  wide_area_impact TINYINT NOT NULL DEFAULT 0,

  submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  audit_time DATETIME NULL,
  auditor_id BIGINT NULL,

  assign_time DATETIME NULL,
  assigner_id BIGINT NULL,
  assignee_user_id BIGINT NULL,

  accept_time DATETIME NULL,
  start_repair_time DATETIME NULL,

  current_status VARCHAR(32) NOT NULL DEFAULT 'SUBMITTED',
  progress_percent INT NOT NULL DEFAULT 0,

  need_purchase TINYINT NOT NULL DEFAULT 0,
  purchase_desc VARCHAR(500) NULL,

  delay_apply_flag TINYINT NOT NULL DEFAULT 0,
  original_expect_finish_time DATETIME NULL,
  delayed_expect_finish_time DATETIME NULL,

  actual_finish_time DATETIME NULL,
  acceptance_time DATETIME NULL,
  user_confirm_result VARCHAR(32) NULL,
  satisfaction_score INT NULL,
  user_feedback_content VARCHAR(1000) NULL,

  close_reason VARCHAR(500) NULL,
  remark VARCHAR(1000) NULL,

  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  UNIQUE KEY uk_repair_order_no (order_no),
  KEY idx_repair_order_repair_user (repair_user_id),
  KEY idx_repair_order_assignee (assignee_user_id),
  KEY idx_repair_order_device (device_id),
  KEY idx_repair_order_status_submit (current_status, submit_time),
  KEY idx_repair_order_status_update (current_status, update_time),
  KEY idx_repair_order_urgency_status (urgency_level, current_status),

  CONSTRAINT fk_repair_order_user_repair FOREIGN KEY (repair_user_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_user_auditor FOREIGN KEY (auditor_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_user_assigner FOREIGN KEY (assigner_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_user_assignee FOREIGN KEY (assignee_user_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_device FOREIGN KEY (device_id) REFERENCES device(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单表';

CREATE TABLE IF NOT EXISTS repair_order_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  from_status VARCHAR(32) NULL,
  to_status VARCHAR(32) NOT NULL,
  operation_type VARCHAR(64) NOT NULL,
  operator_id BIGINT NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  operation_desc VARCHAR(1000) NULL,
  system_recommend_assign_flag TINYINT NOT NULL DEFAULT 0,
  ext_json JSON NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_flow_order_time (repair_order_id, operation_time),
  KEY idx_flow_operator_time (operator_id, operation_time),
  KEY idx_flow_operation_type (operation_type),
  CONSTRAINT fk_flow_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_flow_operator FOREIGN KEY (operator_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单流程记录表';

-- =========================================================
-- 6) repair_record / repair_feedback
-- =========================================================
CREATE TABLE IF NOT EXISTS repair_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_id BIGINT NOT NULL,
  repair_order_id BIGINT NOT NULL,
  repair_count_no INT NOT NULL DEFAULT 1,
  report_time DATETIME NULL,
  accept_time DATETIME NULL,
  start_repair_time DATETIME NULL,
  finish_time DATETIME NULL,
  fault_reason VARCHAR(500) NULL,
  handling_measures TEXT NULL,
  part_replaced_flag TINYINT NOT NULL DEFAULT 0,
  part_info VARCHAR(1000) NULL,
  delayed_flag TINYINT NOT NULL DEFAULT 0,
  delay_reason VARCHAR(1000) NULL,
  actual_duration_hours DECIMAL(10,2) NULL,
  repair_result VARCHAR(64) NULL,
  user_confirm_result VARCHAR(32) NULL,
  user_satisfaction_score INT NULL,
  maintainer_user_id BIGINT NOT NULL,
  remark VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT NOT NULL DEFAULT 0,
  KEY idx_record_order (repair_order_id),
  KEY idx_record_device (device_id),
  KEY idx_record_maintainer (maintainer_user_id),
  KEY idx_record_finish_time (finish_time),
  CONSTRAINT fk_record_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_record_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_record_maintainer FOREIGN KEY (maintainer_user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修记录表';

CREATE TABLE IF NOT EXISTS repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  repair_record_id BIGINT NULL,
  user_id BIGINT NOT NULL,
  confirm_result VARCHAR(32) NOT NULL,
  satisfaction_score INT NULL,
  feedback_content VARCHAR(1000) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_feedback_order_time (repair_order_id, create_time),
  KEY idx_feedback_user_time (user_id, create_time),
  CONSTRAINT fk_feedback_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_feedback_record FOREIGN KEY (repair_record_id) REFERENCES repair_record(id),
  CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- =========================================================
-- 7) announcement
-- =========================================================
CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NULL,
  status VARCHAR(32) NOT NULL DEFAULT 'PUBLISHED',
  publish_time DATETIME NULL,
  expire_time DATETIME NULL,
  publisher_id BIGINT NULL,
  top_flag TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_announcement_status_publish (status, publish_time),
  KEY idx_announcement_publisher (publisher_id),
  CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- =========================================================
-- 8) operation_log / business_log
-- =========================================================
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NULL,
  user_id BIGINT NULL,
  username VARCHAR(50) NULL,
  module VARCHAR(100) NULL,
  operation_type VARCHAR(64) NULL,
  operation_desc VARCHAR(500) NULL,
  request_method VARCHAR(20) NULL,
  request_url VARCHAR(500) NULL,
  ip VARCHAR(50) NULL,
  user_agent VARCHAR(500) NULL,
  result_status VARCHAR(20) NULL,
  error_message VARCHAR(1000) NULL,
  cost_ms BIGINT NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_op_user_time (user_id, operation_time),
  KEY idx_op_module_time (module, operation_time),
  KEY idx_op_type_time (operation_type, operation_time),
  KEY idx_op_trace_id (trace_id),
  CONSTRAINT fk_op_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统审计日志表';

CREATE TABLE IF NOT EXISTS business_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64) NULL,
  biz_type VARCHAR(64) NOT NULL,
  biz_id BIGINT NOT NULL,
  order_no VARCHAR(50) NULL,
  action VARCHAR(64) NOT NULL,
  operator_id BIGINT NULL,
  operator_job_no VARCHAR(30) NULL,
  operator_role VARCHAR(64) NULL,
  content VARCHAR(1000) NULL,
  ext_json JSON NULL,
  operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz_type_id_time (biz_type, biz_id, operation_time),
  KEY idx_biz_order_no_time (order_no, operation_time),
  KEY idx_biz_operator_time (operator_id, operation_time),
  KEY idx_biz_trace_id (trace_id),
  CONSTRAINT fk_biz_operator FOREIGN KEY (operator_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务日志表';

-- =========================================================
-- 9) file_attachment
-- =========================================================
CREATE TABLE IF NOT EXISTS file_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  original_file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(500) NOT NULL,
  file_url VARCHAR(1000) NOT NULL,
  file_type VARCHAR(100) NULL,
  uploader_id BIGINT NOT NULL,
  upload_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_file_biz_type_id (biz_type, biz_id),
  KEY idx_file_uploader_time (uploader_id, upload_time),
  KEY idx_file_upload_time (upload_time),
  CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件表';

-- =========================================================
-- 10) 初始化字典与角色（幂等）
-- =========================================================
INSERT INTO `role` (role_code, role_name, role_desc, status)
SELECT 'ADMIN', '系统管理员', '系统管理与全局审批', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE role_code='ADMIN');
INSERT INTO `role` (role_code, role_name, role_desc, status)
SELECT 'MAINTAINER', '维修人员', '接单与维修', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE role_code='MAINTAINER');
INSERT INTO `role` (role_code, role_name, role_desc, status)
SELECT 'USER', '报修用户', '报修与验收', 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE role_code='USER');

INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'DEVICE_TYPE','SWITCH','交换机',10,1,'设备类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='DEVICE_TYPE' AND dict_code='SWITCH');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'FAULT_TYPE','NETWORK_INTERRUPT','网络中断',10,1,'故障类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='FAULT_TYPE' AND dict_code='NETWORK_INTERRUPT');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'ORDER_STATUS','SUBMITTED','已提交',10,1,'工单状态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='ORDER_STATUS' AND dict_code='SUBMITTED');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'URGENCY_LEVEL','MEDIUM','中',20,1,'紧急程度' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='URGENCY_LEVEL' AND dict_code='MEDIUM');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'USER_STATUS','ENABLED','启用',10,1,'用户状态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='USER_STATUS' AND dict_code='ENABLED');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'ANNOUNCEMENT_STATUS','PUBLISHED','已发布',20,1,'公告状态' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='ANNOUNCEMENT_STATUS' AND dict_code='PUBLISHED');
INSERT INTO dictionary (dict_type, dict_code, dict_label, sort_no, status, remark)
SELECT 'OPERATION_TYPE','FLOW_ACTION','流程动作',40,1,'操作类型' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM dictionary WHERE dict_type='OPERATION_TYPE' AND dict_code='FLOW_ACTION');

-- =========================================================
-- 11) 旧数据迁移（依赖 legacy 与旧表）
-- =========================================================
-- 11.1 sys_user -> user
INSERT INTO `user` (id, job_no, username, real_name, password, phone, email, department, status, last_login_time, deleted, create_time, update_time)
SELECT su.id, su.employee_no, su.username, COALESCE(su.real_name, su.username), su.password,
       su.phone, su.email, su.department, COALESCE(su.status,1), su.last_login_time, 0,
       COALESCE(su.create_time,NOW()), COALESCE(su.update_time,NOW())
FROM sys_user su
WHERE NOT EXISTS (SELECT 1 FROM `user` u WHERE u.id = su.id);

-- 11.2 user_role
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW()
FROM `user` u
JOIN sys_user su ON su.id = u.id
JOIN `role` r ON r.role_code = UPPER(su.role)
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);

-- 11.3 third_party_bind
INSERT INTO third_party_bind (user_id, platform, open_id, union_id, bind_time, status, create_time, update_time)
SELECT su.id, 'WECHAT', su.wx_open_id, NULL, NOW(), 1, NOW(), NOW()
FROM sys_user su
WHERE su.wx_open_id IS NOT NULL AND su.wx_open_id <> ''
  AND NOT EXISTS (SELECT 1 FROM third_party_bind t WHERE t.platform='WECHAT' AND t.open_id=su.wx_open_id);

INSERT INTO third_party_bind (user_id, platform, open_id, union_id, bind_time, status, create_time, update_time)
SELECT su.id, 'QQ', su.qq_open_id, NULL, NOW(), 1, NOW(), NOW()
FROM sys_user su
WHERE su.qq_open_id IS NOT NULL AND su.qq_open_id <> ''
  AND NOT EXISTS (SELECT 1 FROM third_party_bind t WHERE t.platform='QQ' AND t.open_id=su.qq_open_id);

-- 11.4 network_device -> device_type/device
INSERT INTO device_type (type_code, type_name, status, sort_no, remark, create_time, update_time)
SELECT DISTINCT UPPER(REPLACE(IFNULL(nd.device_type,'UNKNOWN'),' ', '_')),
       IFNULL(nd.device_type,'未知类型'), 1, 100, '旧表迁移', NOW(), NOW()
FROM network_device nd
WHERE NOT EXISTS (
  SELECT 1 FROM device_type dt WHERE dt.type_name = IFNULL(nd.device_type,'未知类型')
);

INSERT INTO device (id, device_no, device_name, device_type_id, device_type, brand_model, ip_address, mac_address,
                    department_or_location, purchase_date, status, remark, create_time, update_time)
SELECT nd.id, nd.device_code, nd.device_name, dt.id, dt.type_code, nd.brand_model, nd.ip_address, nd.mac_address,
       nd.location, nd.purchase_date,
       CASE nd.status WHEN '正常' THEN 'NORMAL' WHEN '故障' THEN 'FAULT' WHEN '维修中' THEN 'REPAIRING' WHEN '停用' THEN 'DISABLED' ELSE 'NORMAL' END,
       nd.remark, COALESCE(nd.create_time,NOW()), COALESCE(nd.update_time,NOW())
FROM network_device nd
LEFT JOIN device_type dt ON dt.type_name = IFNULL(nd.device_type,'未知类型')
WHERE NOT EXISTS (SELECT 1 FROM device d WHERE d.id = nd.id);

-- 11.5 repair_order_legacy -> repair_order
INSERT INTO repair_order (
  id, order_no, repair_user_id, repair_user_job_no, contact_phone, department_or_location,
  device_id, device_no, device_name, device_type, fault_type, fault_desc,
  urgency_level, wide_area_impact, submit_time, audit_time, auditor_id,
  assign_time, assigner_id, assignee_user_id, accept_time, start_repair_time,
  current_status, progress_percent, need_purchase, purchase_desc,
  delay_apply_flag, original_expect_finish_time, delayed_expect_finish_time,
  actual_finish_time, acceptance_time, user_confirm_result, satisfaction_score,
  user_feedback_content, close_reason, remark, deleted, create_time, update_time
)
SELECT ro.id, ro.order_no, ro.reporter_id, COALESCE(u.job_no,''), u.phone, COALESCE(u.department,d.department_or_location),
       ro.device_id, d.device_no, d.device_name, d.device_type, NULL, ro.description,
       CASE ro.priority WHEN '低' THEN 'LOW' WHEN '中' THEN 'MEDIUM' WHEN '高' THEN 'HIGH' ELSE 'MEDIUM' END,
       0, COALESCE(ro.report_time,NOW()), ro.audit_time, ro.audit_by,
       ro.assign_time, ro.audit_by, ro.assign_maintainer_id, ro.accept_time, ro.start_repair_time,
       CASE ro.status
         WHEN '已提交' THEN 'SUBMITTED' WHEN '审核通过' THEN 'AUDIT_PASS' WHEN '审核驳回' THEN 'AUDIT_REJECT'
         WHEN '待分配' THEN 'PENDING_ASSIGN' WHEN '待接单' THEN 'PENDING_ACCEPT' WHEN '维修人员已接单' THEN 'ACCEPTED'
         WHEN '维修中' THEN 'IN_PROGRESS' WHEN '处理中' THEN 'IN_PROGRESS' WHEN '待验收' THEN 'PENDING_ACCEPTANCE'
         WHEN '已完成' THEN 'COMPLETED' WHEN '已关闭' THEN 'CLOSED' WHEN '已取消' THEN 'CANCELLED' ELSE 'SUBMITTED'
       END,
       COALESCE(ro.progress,0), 0, NULL, 0, ro.expected_finish_time, NULL,
       ro.finish_time, ro.confirm_time,
       CASE WHEN ro.feedback IS NULL OR ro.feedback='' THEN NULL WHEN ro.status='已完成' THEN 'RESOLVED' ELSE NULL END,
       ro.satisfaction_score, ro.feedback, ro.close_reason, ro.title, 0,
       COALESCE(ro.create_time,NOW()), COALESCE(ro.update_time,NOW())
FROM repair_order_legacy ro
LEFT JOIN `user` u ON u.id=ro.reporter_id
LEFT JOIN device d ON d.id=ro.device_id
WHERE NOT EXISTS (SELECT 1 FROM repair_order nro WHERE nro.id=ro.id);

-- 11.6 repair_order_flow_legacy -> repair_order_flow
INSERT INTO repair_order_flow (
  repair_order_id, from_status, to_status, operation_type, operator_id, operation_time,
  operation_desc, system_recommend_assign_flag, ext_json, create_time, update_time
)
SELECT rf.repair_order_id,
       CASE rf.from_status
         WHEN '已提交' THEN 'SUBMITTED' WHEN '审核通过' THEN 'AUDIT_PASS' WHEN '审核驳回' THEN 'AUDIT_REJECT'
         WHEN '待分配' THEN 'PENDING_ASSIGN' WHEN '待接单' THEN 'PENDING_ACCEPT' WHEN '维修人员已接单' THEN 'ACCEPTED'
         WHEN '维修中' THEN 'IN_PROGRESS' WHEN '待验收' THEN 'PENDING_ACCEPTANCE' WHEN '已完成' THEN 'COMPLETED'
         WHEN '已关闭' THEN 'CLOSED' WHEN '已取消' THEN 'CANCELLED' ELSE rf.from_status END,
       CASE rf.to_status
         WHEN '已提交' THEN 'SUBMITTED' WHEN '审核通过' THEN 'AUDIT_PASS' WHEN '审核驳回' THEN 'AUDIT_REJECT'
         WHEN '待分配' THEN 'PENDING_ASSIGN' WHEN '待接单' THEN 'PENDING_ACCEPT' WHEN '维修人员已接单' THEN 'ACCEPTED'
         WHEN '维修中' THEN 'IN_PROGRESS' WHEN '待验收' THEN 'PENDING_ACCEPTANCE' WHEN '已完成' THEN 'COMPLETED'
         WHEN '已关闭' THEN 'CLOSED' WHEN '已取消' THEN 'CANCELLED' ELSE rf.to_status END,
       rf.action,
       rf.operator_id,
       COALESCE(rf.create_time,NOW()),
       rf.remark,
       CASE WHEN rf.action='AUTO_ASSIGN' THEN 1 ELSE 0 END,
       JSON_OBJECT('legacy_operator_role', rf.operator_role),
       COALESCE(rf.create_time,NOW()),
       NOW()
FROM repair_order_flow_legacy rf
WHERE NOT EXISTS (
  SELECT 1 FROM repair_order_flow nf
  WHERE nf.repair_order_id = rf.repair_order_id
    AND nf.operation_type = rf.action
    AND nf.operation_time = COALESCE(rf.create_time,NOW())
);

-- 11.7 repair_record_legacy -> repair_record
INSERT INTO repair_record (
  id, device_id, repair_order_id, repair_count_no, report_time, accept_time, start_repair_time, finish_time,
  fault_reason, handling_measures, part_replaced_flag, part_info, delayed_flag, delay_reason,
  actual_duration_hours, repair_result, user_confirm_result, user_satisfaction_score,
  maintainer_user_id, remark, create_time, update_time, deleted
)
SELECT rr.id, rr.device_id, rr.repair_order_id, 1, ro.submit_time, ro.accept_time, ro.start_repair_time, rr.repair_time,
       rr.fault_reason, CONCAT(IFNULL(rr.process_detail,''), '\n', IFNULL(rr.result_detail,'')), 0, NULL, 0, NULL,
       CASE WHEN ro.start_repair_time IS NOT NULL AND rr.repair_time IS NOT NULL
         THEN TIMESTAMPDIFF(MINUTE, ro.start_repair_time, rr.repair_time) / 60 ELSE NULL END,
       CASE WHEN rr.is_resolved=1 THEN 'FIXED' ELSE 'UNRESOLVED' END,
       CASE WHEN rr.is_resolved=1 THEN 'RESOLVED' ELSE 'UNRESOLVED' END,
       ro.satisfaction_score,
       rr.maintainer_id,
       rr.result_detail,
       COALESCE(rr.create_time,NOW()), COALESCE(rr.update_time,NOW()), 0
FROM repair_record_legacy rr
LEFT JOIN repair_order ro ON ro.id=rr.repair_order_id
WHERE NOT EXISTS (SELECT 1 FROM repair_record nr WHERE nr.id=rr.id);

-- 11.8 notice -> announcement
INSERT INTO announcement (id, title, content, status, publish_time, expire_time, publisher_id, top_flag, create_time, update_time)
SELECT n.id, n.title, n.content, 'PUBLISHED', n.create_time, NULL, n.publisher_id, 0,
       COALESCE(n.create_time,NOW()), COALESCE(n.update_time,NOW())
FROM notice n
WHERE NOT EXISTS (SELECT 1 FROM announcement a WHERE a.id=n.id);

-- 11.9 operation_log_legacy -> operation_log
INSERT INTO operation_log (
  id, trace_id, user_id, username, module, operation_type, operation_desc,
  request_method, request_url, ip, user_agent, result_status, error_message,
  cost_ms, operation_time, create_time
)
SELECT ol.id, NULL, ol.user_id, ol.username, ol.module, ol.operation_type, ol.operation_desc,
       ol.request_method, ol.request_url, ol.ip, NULL, 'SUCCESS', NULL,
       NULL, ol.operation_time, COALESCE(ol.operation_time,NOW())
FROM operation_log_legacy ol
WHERE NOT EXISTS (SELECT 1 FROM operation_log nol WHERE nol.id=ol.id);

SET FOREIGN_KEY_CHECKS = 1;

-- 校园网络设备管理与故障报修系统：最终统一可执行 SQL（Step23）
-- 说明：该脚本为当前代码的最终收敛版本，优先保证 auth/repair/report/log 模块字段一致性。

CREATE DATABASE IF NOT EXISTS network_repair DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE network_repair;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS login_log;
DROP TABLE IF EXISTS business_log;
DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS repair_feedback;
DROP TABLE IF EXISTS repair_record;
DROP TABLE IF EXISTS repair_order_flow;
DROP TABLE IF EXISTS repair_order;
DROP TABLE IF EXISTS file_attachment;
DROP TABLE IF EXISTS third_party_bind;
DROP TABLE IF EXISTS dictionary;
DROP TABLE IF EXISTS device;
DROP TABLE IF EXISTS device_type;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS role;
DROP TABLE IF EXISTS `user`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(30) NOT NULL,
  username VARCHAR(50) NOT NULL,
  password VARCHAR(255) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  department VARCHAR(100),
  role VARCHAR(30),
  status TINYINT NOT NULL DEFAULT 1,
  last_login_time DATETIME,
  wx_open_id VARCHAR(100),
  qq_open_id VARCHAR(100),
  third_party_bound_flag TINYINT NOT NULL DEFAULT 0,
  create_by BIGINT,
  update_by BIGINT,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_user_employee_no (employee_no),
  UNIQUE KEY uk_user_username (username)
);

CREATE TABLE role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(100) NOT NULL,
  role_status VARCHAR(20) DEFAULT 'ENABLED',
  role_desc VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  create_by BIGINT,
  update_by BIGINT,
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_role_code (role_code)
);

CREATE TABLE user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_by BIGINT,
  create_time DATETIME,
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user`(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role(id)
);

CREATE TABLE device_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(50) NOT NULL,
  type_name VARCHAR(100) NOT NULL,
  status TINYINT DEFAULT 1,
  sort_no INT DEFAULT 0,
  remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_device_type_code (type_code)
);

CREATE TABLE device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_code VARCHAR(50) NOT NULL,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50),
  device_type_name VARCHAR(100),
  brand VARCHAR(100),
  model VARCHAR(100),
  serial_number VARCHAR(100),
  serial_no VARCHAR(100),
  campus VARCHAR(100),
  building_location VARCHAR(150),
  building VARCHAR(100),
  machine_room VARCHAR(100),
  office VARCHAR(100),
  location VARCHAR(150),
  purchase_date DATE,
  enable_date DATE,
  warranty_expiry_date DATE,
  owner_user_id BIGINT,
  owner_employee_no VARCHAR(30),
  owner_name VARCHAR(50),
  management_dept VARCHAR(100),
  manage_department VARCHAR(100),
  status VARCHAR(20),
  last_fault_time DATETIME,
  total_repair_requests INT DEFAULT 0,
  total_repair_order_count INT DEFAULT 0,
  total_repair_count INT DEFAULT 0,
  fault_reason_stats VARCHAR(500),
  repair_approval_required TINYINT DEFAULT 0,
  remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_device_code (device_code),
  CONSTRAINT fk_device_owner FOREIGN KEY (owner_user_id) REFERENCES `user`(id)
);

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL,
  reporter_id BIGINT NOT NULL,
  reporter_employee_no VARCHAR(30) NOT NULL,
  reporter_name VARCHAR(50),
  contact_phone VARCHAR(20),
  reporter_department VARCHAR(100),
  report_location VARCHAR(150),
  device_id BIGINT NOT NULL,
  device_code VARCHAR(50) NOT NULL,
  device_name VARCHAR(100),
  device_type VARCHAR(50),
  title VARCHAR(100) NOT NULL,
  fault_type VARCHAR(50),
  description TEXT,
  priority VARCHAR(20) NOT NULL,
  affect_wide_area_network TINYINT DEFAULT 0,
  report_time DATETIME NOT NULL,
  audit_time DATETIME,
  audit_by BIGINT,
  audit_by_employee_no VARCHAR(30),
  audit_by_name VARCHAR(50),
  assign_time DATETIME,
  assign_by BIGINT,
  assign_by_employee_no VARCHAR(30),
  assign_by_name VARCHAR(50),
  assign_maintainer_id BIGINT,
  assign_maintainer_employee_no VARCHAR(30),
  assign_maintainer_name VARCHAR(50),
  accept_time DATETIME,
  start_repair_time DATETIME,
  status VARCHAR(30) NOT NULL,
  progress INT DEFAULT 0,
  need_purchase_parts TINYINT DEFAULT 0,
  parts_description VARCHAR(500),
  apply_delay TINYINT DEFAULT 0,
  original_expected_finish_time DATETIME,
  delayed_expected_finish_time DATETIME,
  expected_finish_time DATETIME,
  finish_time DATETIME,
  confirm_time DATETIME,
  user_confirm_result VARCHAR(30),
  satisfaction_score INT,
  feedback VARCHAR(500),
  close_reason VARCHAR(255),
  remark VARCHAR(500),
  create_by BIGINT,
  update_by BIGINT,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_repair_order_no (order_no),
  KEY idx_repair_order_status (status),
  CONSTRAINT fk_repair_order_reporter FOREIGN KEY (reporter_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_repair_order_auditor FOREIGN KEY (audit_by) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_assigner FOREIGN KEY (assign_by) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_maintainer FOREIGN KEY (assign_maintainer_id) REFERENCES `user`(id)
);

CREATE TABLE repair_order_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30),
  action VARCHAR(50) NOT NULL,
  operation_type VARCHAR(50),
  operator_id BIGINT,
  operator_employee_no VARCHAR(30),
  operator_name VARCHAR(50),
  operator_role VARCHAR(30),
  remark VARCHAR(500),
  operation_time DATETIME,
  system_recommend_assign_flag TINYINT,
  ext_json VARCHAR(2000),
  create_time DATETIME NOT NULL,
  update_time DATETIME,
  KEY idx_order_flow_order (repair_order_id),
  CONSTRAINT fk_order_flow_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id)
);

CREATE TABLE repair_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  repair_order_no VARCHAR(50),
  device_id BIGINT NOT NULL,
  device_code VARCHAR(50),
  repair_count_no INT,
  maintenance_count_no INT,
  report_time DATETIME,
  accept_time DATETIME,
  start_repair_time DATETIME,
  finish_time DATETIME,
  maintainer_id BIGINT NOT NULL,
  maintainer_employee_no VARCHAR(30),
  maintainer_name VARCHAR(50),
  fault_reason VARCHAR(255),
  process_detail TEXT,
  fix_measure TEXT,
  result_detail TEXT,
  is_resolved TINYINT,
  used_parts TINYINT DEFAULT 0,
  used_parts_desc VARCHAR(500),
  delay_applied TINYINT DEFAULT 0,
  delay_reason VARCHAR(500),
  labor_hours INT,
  repair_conclusion VARCHAR(500),
  user_confirm_result VARCHAR(30),
  user_satisfaction INT,
  photo_urls TEXT,
  remark VARCHAR(500),
  repair_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_repair_record_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_repair_record_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_repair_record_maintainer FOREIGN KEY (maintainer_id) REFERENCES `user`(id)
);

CREATE TABLE repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  repair_record_id BIGINT,
  user_id BIGINT NOT NULL,
  user_employee_no VARCHAR(30) NOT NULL,
  confirm_result VARCHAR(30),
  satisfaction_score INT,
  feedback_content VARCHAR(1000),
  confirm_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_repair_feedback_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_repair_feedback_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

CREATE TABLE announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT,
  publisher_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  sort_no INT DEFAULT 0,
  publish_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_id) REFERENCES `user`(id)
);

CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  module VARCHAR(50),
  operation_type VARCHAR(50),
  operation_desc VARCHAR(255),
  request_method VARCHAR(20),
  request_url VARCHAR(255),
  request_params VARCHAR(2000),
  response_code VARCHAR(20),
  result_status VARCHAR(20),
  error_message VARCHAR(500),
  trace_id VARCHAR(64),
  ip VARCHAR(50),
  user_agent VARCHAR(255),
  cost_ms BIGINT,
  operation_time DATETIME NOT NULL,
  create_time DATETIME,
  CONSTRAINT fk_operation_log_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

CREATE TABLE business_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  trace_id VARCHAR(64),
  business_type VARCHAR(50) NOT NULL,
  business_no VARCHAR(50) NOT NULL,
  biz_type VARCHAR(50),
  biz_id BIGINT,
  order_no VARCHAR(50),
  action VARCHAR(50) NOT NULL,
  operator_id BIGINT,
  operator_employee_no VARCHAR(30),
  operator_job_no VARCHAR(30),
  operator_name VARCHAR(50),
  operator_role VARCHAR(30),
  content VARCHAR(1000),
  status VARCHAR(20),
  ext_json VARCHAR(2000),
  operation_time DATETIME,
  create_time DATETIME NOT NULL,
  KEY idx_business_no (business_no),
  CONSTRAINT fk_business_log_user FOREIGN KEY (operator_id) REFERENCES `user`(id)
);

CREATE TABLE login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  ip VARCHAR(50),
  user_agent VARCHAR(255),
  login_status VARCHAR(20),
  fail_reason VARCHAR(255),
  login_time DATETIME,
  KEY idx_login_status_time (login_status, login_time)
);

CREATE TABLE file_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(50),
  business_id BIGINT,
  biz_type VARCHAR(50),
  biz_id BIGINT,
  file_name VARCHAR(255),
  file_url VARCHAR(1000),
  file_type VARCHAR(50),
  uploader_id BIGINT,
  upload_time DATETIME,
  remark VARCHAR(500),
  create_time DATETIME,
  update_time DATETIME,
  KEY idx_attachment_biz (biz_type, biz_id)
);

CREATE TABLE third_party_bind (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  platform VARCHAR(30),
  provider VARCHAR(30),
  open_id VARCHAR(100) NOT NULL,
  union_id VARCHAR(100),
  bind_time DATETIME,
  status TINYINT DEFAULT 1,
  bind_status TINYINT DEFAULT 1,
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_platform_openid (platform, open_id),
  CONSTRAINT fk_third_party_user FOREIGN KEY (user_id) REFERENCES `user`(id)
);

CREATE TABLE dictionary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(50),
  dict_code VARCHAR(100),
  dict_label VARCHAR(100),
  dict_value VARCHAR(255),
  sort_no INT DEFAULT 0,
  status TINYINT DEFAULT 1,
  remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME,
  UNIQUE KEY uk_dict_type_code (dict_type, dict_code)
);

-- 最小可用基础数据
INSERT INTO role (role_code, role_name, role_status, role_desc, status, create_time, update_time)
VALUES
('ADMIN','系统管理员','ENABLED','系统全局管理',1,NOW(),NOW()),
('MAINTAINER','维修人员','ENABLED','设备维修处理',1,NOW(),NOW()),
('USER','报修用户','ENABLED','发起报修与反馈',1,NOW(),NOW());

INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
VALUES
('A2026001','admin','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','系统管理员','13800000001','admin@campus.edu','网络信息中心','admin',1,0,NOW(),NOW()),
('M2026001','maint1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','王工','13800000004','maint1@campus.edu','网络运维组','maintainer',1,0,NOW(),NOW()),
('U2026001','user1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','张三','13800000002','user1@campus.edu','教务处','user',1,0,NOW(),NOW());

INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='A2026001' AND r.role_code='ADMIN';
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='M2026001' AND r.role_code='MAINTAINER';
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='U2026001' AND r.role_code='USER';

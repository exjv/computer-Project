DROP DATABASE IF EXISTS network_repair;
CREATE DATABASE network_repair DEFAULT CHARACTER SET utf8mb4;
USE network_repair;

-- ======================
-- 基础组织模型
-- ======================
CREATE TABLE `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(30) NOT NULL COMMENT '工号（全局唯一）',
  username VARCHAR(50) NOT NULL,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  phone VARCHAR(20),
  email VARCHAR(100),
  department VARCHAR(100),
  role VARCHAR(20) NOT NULL COMMENT '兼容旧结构角色字段',
  status TINYINT DEFAULT 1 COMMENT '1启用 0禁用',
  last_login_time DATETIME,
  wx_open_id VARCHAR(100),
  qq_open_id VARCHAR(100),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_user_employee_no (employee_no),
  UNIQUE KEY uk_user_username (username)
) COMMENT='用户表';

CREATE TABLE `role` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(50) NOT NULL,
  role_name VARCHAR(50) NOT NULL,
  role_status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_role_code (role_code)
) COMMENT='角色表';

CREATE TABLE user_role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_user_role (user_id, role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user`(id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES `role`(id)
) COMMENT='用户角色关联表';

CREATE TABLE permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  permission_code VARCHAR(80) NOT NULL,
  permission_name VARCHAR(100) NOT NULL,
  permission_type VARCHAR(20) NOT NULL DEFAULT 'API',
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_permission_code (permission_code)
) COMMENT='权限表';

CREATE TABLE role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  create_time DATETIME NOT NULL,
  UNIQUE KEY uk_role_permission (role_id, permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES `role`(id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission(id)
) COMMENT='角色权限关联表';

CREATE TABLE device_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_code VARCHAR(50) NOT NULL,
  type_name VARCHAR(50) NOT NULL,
  sort_no INT DEFAULT 0,
  status VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  remark VARCHAR(255),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_device_type_code (type_code)
) COMMENT='设备类型表';

-- ======================
-- 设备与工单
-- ======================
CREATE TABLE device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_code VARCHAR(50) NOT NULL COMMENT '设备编号（唯一）',
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50) NOT NULL COMMENT '设备类型编码',
  device_type_name VARCHAR(50),
  brand VARCHAR(100),
  model VARCHAR(100),
  serial_no VARCHAR(100),
  brand_model VARCHAR(100),
  ip_address VARCHAR(50),
  mac_address VARCHAR(50),
  campus VARCHAR(100),
  building VARCHAR(100),
  machine_room VARCHAR(100),
  office VARCHAR(100),
  location VARCHAR(150),
  purchase_date DATE,
  enable_date DATE,
  warranty_expire_date DATE,
  owner_user_id BIGINT,
  owner_employee_no VARCHAR(30),
  owner_name VARCHAR(50),
  management_dept VARCHAR(100),
  status VARCHAR(20) NOT NULL COMMENT 'DEVICE_NORMAL/DEVICE_FAULT/DEVICE_REPAIRING/DEVICE_DISABLED',
  last_fault_time DATETIME,
  total_repair_order_count INT DEFAULT 0,
  total_repair_count INT DEFAULT 0,
  fault_reason_stats JSON,
  remark VARCHAR(500),
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_device_code (device_code),
  KEY idx_device_type (device_type),
  CONSTRAINT fk_device_type FOREIGN KEY (device_type) REFERENCES device_type(type_code),
  CONSTRAINT fk_device_owner FOREIGN KEY (owner_user_id) REFERENCES `user`(id)
) COMMENT='设备表';

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL COMMENT '工单编号（唯一）',
  reporter_id BIGINT NOT NULL COMMENT '报修用户',
  reporter_employee_no VARCHAR(30) NOT NULL COMMENT '报修人工号',
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
  priority VARCHAR(20) NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
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
  status VARCHAR(30) NOT NULL COMMENT 'SUBMITTED/APPROVED/ASSIGNED/ACCEPTED/IN_PROGRESS/PENDING_ACCEPTANCE/COMPLETED/CLOSED/CANCELED',
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
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  UNIQUE KEY uk_repair_order_no (order_no),
  KEY idx_repair_order_status (status),
  CONSTRAINT fk_repair_order_reporter FOREIGN KEY (reporter_id) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_repair_order_auditor FOREIGN KEY (audit_by) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_assigner FOREIGN KEY (assign_by) REFERENCES `user`(id),
  CONSTRAINT fk_repair_order_maintainer FOREIGN KEY (assign_maintainer_id) REFERENCES `user`(id)
) COMMENT='工单表';

CREATE TABLE repair_order_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  from_status VARCHAR(30),
  to_status VARCHAR(30),
  action VARCHAR(50),
  operation_type VARCHAR(50),
  operator_id BIGINT,
  operator_employee_no VARCHAR(30),
  operator_name VARCHAR(50),
  operator_role VARCHAR(20),
  remark VARCHAR(500),
  create_time DATETIME NOT NULL,
  KEY idx_flow_order (repair_order_id),
  CONSTRAINT fk_repair_order_flow_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_repair_order_flow_operator FOREIGN KEY (operator_id) REFERENCES `user`(id)
) COMMENT='工单流程记录表';

CREATE TABLE repair_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  repair_order_no VARCHAR(50),
  device_id BIGINT NOT NULL,
  device_code VARCHAR(50),
  maintainer_id BIGINT NOT NULL,
  maintainer_employee_no VARCHAR(30),
  maintainer_name VARCHAR(50),
  fault_reason VARCHAR(255),
  process_detail TEXT,
  result_detail TEXT,
  is_resolved TINYINT,
  used_parts TINYINT DEFAULT 0,
  used_parts_desc VARCHAR(500),
  labor_hours INT,
  repair_conclusion VARCHAR(500),
  repair_time DATETIME,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_repair_record_order FOREIGN KEY (repair_order_id) REFERENCES repair_order(id),
  CONSTRAINT fk_repair_record_device FOREIGN KEY (device_id) REFERENCES device(id),
  CONSTRAINT fk_repair_record_maintainer FOREIGN KEY (maintainer_id) REFERENCES `user`(id)
) COMMENT='维修记录表';

CREATE TABLE repair_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
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
) COMMENT='用户反馈表';

CREATE TABLE announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT,
  publisher_id BIGINT,
  status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
  sort_no INT DEFAULT 0,
  create_time DATETIME NOT NULL,
  update_time DATETIME NOT NULL,
  CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_id) REFERENCES `user`(id)
) COMMENT='公告表';

-- ======================
-- 日志、附件、字典、三方绑定
-- ======================
CREATE TABLE operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  module VARCHAR(50),
  operation_type VARCHAR(50),
  operation_desc VARCHAR(255),
  request_method VARCHAR(20),
  request_url VARCHAR(255),
  request_params VARCHAR(1000),
  response_code VARCHAR(20),
  trace_id VARCHAR(64),
  ip VARCHAR(50),
  operation_time DATETIME NOT NULL,
  CONSTRAINT fk_operation_log_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) COMMENT='系统操作日志表';

CREATE TABLE business_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  business_type VARCHAR(50) NOT NULL,
  business_no VARCHAR(50) NOT NULL,
  action VARCHAR(50) NOT NULL,
  operator_id BIGINT,
  operator_employee_no VARCHAR(30),
  operator_name VARCHAR(50),
  content VARCHAR(1000),
  status VARCHAR(20),
  create_time DATETIME NOT NULL,
  KEY idx_business_no (business_no),
  CONSTRAINT fk_business_log_user FOREIGN KEY (operator_id) REFERENCES `user`(id)
) COMMENT='业务日志表';

CREATE TABLE login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  ip VARCHAR(50),
  user_agent VARCHAR(255),
  login_status VARCHAR(20),
  fail_reason VARCHAR(255),
  login_time DATETIME NOT NULL,
  CONSTRAINT fk_login_log_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) COMMENT='登录日志表';

CREATE TABLE file_attachment (
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
  remark VARCHAR(255),
  KEY idx_file_business (business_type, business_id),
  CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES `user`(id)
) COMMENT='附件表';

CREATE TABLE third_party_bind (
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
  UNIQUE KEY uk_provider_openid (provider, open_id),
  CONSTRAINT fk_third_party_bind_user FOREIGN KEY (user_id) REFERENCES `user`(id)
) COMMENT='第三方登录绑定表';

CREATE TABLE dictionary (
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
) COMMENT='数据字典表';

-- ======================
-- 初始化基础数据（精简）
-- ======================
INSERT INTO `role` (id, role_code, role_name, role_status, remark, create_time, update_time) VALUES
(1, 'admin', '系统管理员', 'ENABLED', '系统管理角色', NOW(), NOW()),
(2, 'maintainer', '维修人员', 'ENABLED', '维修执行角色', NOW(), NOW()),
(3, 'user', '报修用户', 'ENABLED', '普通报修用户', NOW(), NOW());

INSERT INTO permission (id, permission_code, permission_name, permission_type, status, create_time, update_time) VALUES
(1,'user:manage','用户管理','API','ENABLED',NOW(),NOW()),
(2,'role:manage','角色管理','API','ENABLED',NOW(),NOW()),
(3,'device:manage','设备管理','API','ENABLED',NOW(),NOW()),
(4,'repair:order:approve','工单审批','API','ENABLED',NOW(),NOW()),
(5,'repair:order:assign','工单分配','API','ENABLED',NOW(),NOW()),
(6,'repair:order:view:all','工单全量查看','API','ENABLED',NOW(),NOW()),
(7,'repair:order:view:self','工单本人查看','API','ENABLED',NOW(),NOW()),
(8,'repair:order:create','发起报修','API','ENABLED',NOW(),NOW()),
(9,'repair:record:write','维修记录填写','API','ENABLED',NOW(),NOW()),
(10,'notice:publish','公告发布','API','ENABLED',NOW(),NOW()),
(11,'statistics:view','统计分析查看','API','ENABLED',NOW(),NOW()),
(12,'report:export','报表生成','API','ENABLED',NOW(),NOW()),
(13,'log:operation:view','操作日志查看','API','ENABLED',NOW(),NOW()),
(14,'log:business:view','业务日志查看','API','ENABLED',NOW(),NOW()),
(15,'repair:supervise','维修流程监管','API','ENABLED',NOW(),NOW()),
(16,'repair:order:accept','接单','API','ENABLED',NOW(),NOW()),
(17,'repair:order:reject','拒单','API','ENABLED',NOW(),NOW()),
(18,'repair:order:progress','维修进度更新','API','ENABLED',NOW(),NOW()),
(19,'repair:attachment:upload','维修照片上传','API','ENABLED',NOW(),NOW()),
(20,'repair:expected-finish:update','预计完成时间填写','API','ENABLED',NOW(),NOW()),
(21,'repair:delay:apply','延期申请','API','ENABLED',NOW(),NOW()),
(22,'repair:parts:apply','采购配件申请','API','ENABLED',NOW(),NOW()),
(23,'repair:feedback:confirm','报修确认与评价','API','ENABLED',NOW(),NOW());

INSERT INTO role_permission (role_id, permission_id, create_time) VALUES
(1,1,NOW()),(1,2,NOW()),(1,3,NOW()),(1,4,NOW()),(1,5,NOW()),(1,6,NOW()),(1,8,NOW()),(1,10,NOW()),(1,11,NOW()),(1,12,NOW()),(1,13,NOW()),(1,14,NOW()),(1,15,NOW()),
(2,7,NOW()),(2,9,NOW()),(2,16,NOW()),(2,17,NOW()),(2,18,NOW()),(2,19,NOW()),(2,20,NOW()),(2,21,NOW()),(2,22,NOW()),(2,23,NOW()),
(3,7,NOW()),(3,8,NOW()),(3,23,NOW());

INSERT INTO `user`
(id, employee_no, username, password, real_name, phone, email, department, role, status, create_time, update_time) VALUES
(1, 'A2026001', 'admin', '$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.', '系统管理员', '13800000001', 'admin@campus.edu', '网络信息中心', 'admin', 1, NOW(), NOW()),
(2, 'U2026001', 'user1', '$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.', '张三', '13800000002', 'user1@campus.edu', '教务处', 'user', 1, NOW(), NOW()),
(3, 'M2026001', 'maint1', '$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.', '王工', '13800000003', 'm1@campus.edu', '网络运维组', 'maintainer', 1, NOW(), NOW());

INSERT INTO user_role (user_id, role_id, create_time) VALUES
(1,1,NOW()),(2,3,NOW()),(3,2,NOW());

INSERT INTO device_type (id, type_code, type_name, sort_no, status, create_time, update_time) VALUES
(1,'SWITCH','交换机',1,'ENABLED',NOW(),NOW()),
(2,'ROUTER','路由器',2,'ENABLED',NOW(),NOW()),
(3,'FIREWALL','防火墙',3,'ENABLED',NOW(),NOW()),
(4,'AP','无线AP',4,'ENABLED',NOW(),NOW()),
(5,'SERVER','服务器',5,'ENABLED',NOW(),NOW());

INSERT INTO device
(id, device_code, device_name, device_type, device_type_name, brand, model, serial_no, location, purchase_date, enable_date, warranty_expire_date, owner_user_id, owner_employee_no, owner_name, management_dept, status, total_repair_order_count, total_repair_count, create_time, update_time)
VALUES
(1, 'DEV-001', '核心交换机A', 'SWITCH', '交换机', 'H3C', 'S5560', 'SN001', '信息楼机房', '2021-03-12', '2021-04-01', '2027-03-12', 3, 'M2026001', '王工', '网络运维组', '正常', 0, 0, NOW(), NOW());

INSERT INTO repair_order
(id, order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location, device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network, report_time, status, progress, create_time, update_time)
VALUES
(1, 'RO202603220001', 2, 'U2026001', '张三', '13800000002', '教务处', '信息楼机房', 1, 'DEV-001', '核心交换机A', '交换机', '核心交换机端口告警', '链路异常', '端口丢包率较高', '高', 1, NOW(), '已提交', 5, NOW(), NOW());

INSERT INTO announcement (id, title, content, publisher_id, status, sort_no, create_time, update_time) VALUES
(1, '系统初始化公告', '数据库模型已升级为业务流程版。', 1, 'PUBLISHED', 1, NOW(), NOW());

INSERT INTO dictionary (dict_type, dict_code, dict_label, dict_value, sort_no, status, create_time, update_time) VALUES
('REPAIR_PRIORITY', 'LOW', '低', 'LOW', 1, 'ENABLED', NOW(), NOW()),
('REPAIR_PRIORITY', 'MEDIUM', '中', 'MEDIUM', 2, 'ENABLED', NOW(), NOW()),
('REPAIR_PRIORITY', '高', '高', '高', 3, 'ENABLED', NOW(), NOW());

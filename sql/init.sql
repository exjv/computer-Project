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
  device_type VARCHAR(50),
  brand VARCHAR(100),
  model VARCHAR(100),
  serial_number VARCHAR(100),
  campus VARCHAR(100),
  building_location VARCHAR(150),
  enable_date DATE,
  warranty_expiry_date DATE,
  owner_name VARCHAR(50),
  manage_department VARCHAR(100),
  brand_model VARCHAR(100),
  ip_address VARCHAR(50),
  mac_address VARCHAR(50),
  campus VARCHAR(100),
  building VARCHAR(100),
  machine_room VARCHAR(100),
  office VARCHAR(100),
  location VARCHAR(150),
  purchase_date DATE,
  status VARCHAR(20),
  last_fault_time DATETIME,
  total_repair_requests INT DEFAULT 0,
  total_repair_count INT DEFAULT 0,
  fault_reason_stats VARCHAR(500),
  repair_approval_required TINYINT DEFAULT 0,
  remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
);

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
  login_time DATETIME
);

INSERT INTO sys_user
(id,employee_no,username,password,real_name,phone,email,department,role,status,last_login_time,wx_open_id,qq_open_id,create_time,update_time) VALUES
(1,'A2026001','admin','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','系统管理员','13800000001','admin@campus.edu','网络信息中心','admin',1,NULL,NULL,NULL,NOW(),NOW()),
(2,'U2026001','user1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','张三','13800000002','user1@campus.edu','教务处','user',1,NULL,NULL,NULL,NOW(),NOW()),
(3,'U2026002','user2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','李四','13800000003','user2@campus.edu','图书馆','user',1,NULL,NULL,NULL,NOW(),NOW()),
(4,'M2026001','maint1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','王工','13800000004','m1@campus.edu','网络运维组','maintainer',1,NULL,NULL,NULL,NOW(),NOW()),
(5,'M2026002','maint2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','赵工','13800000005','m2@campus.edu','网络运维组','maintainer',1,NULL,NULL,NULL,NOW(),NOW());

INSERT INTO network_device
(id,device_code,device_name,device_type,brand,model,serial_number,campus,building_location,enable_date,warranty_expiry_date,owner_name,manage_department,brand_model,ip_address,mac_address,location,purchase_date,status,last_fault_time,total_repair_requests,total_repair_count,fault_reason_stats,repair_approval_required,remark,create_time,update_time) VALUES
(1,'DEV-001','核心交换机A','交换机','H3C','S5560','SN-001','主校区','信息楼机房','2021-03-15','2027-03-12','王工','网络信息中心','H3C/S5560','10.0.0.1','00-11-22-33-44-01','信息楼机房','2021-03-12','正常',NULL,0,0,'',1,'核心层设备',NOW(),NOW()),
(2,'DEV-002','出口路由器','路由器','Cisco','ISR4431','SN-002','主校区','网络中心','2020-06-15','2026-06-10','赵工','网络信息中心','Cisco/ISR4431','10.0.0.254','00-11-22-33-44-02','网络中心','2020-06-10','维修中',NOW(),1,0,'',1,'出口链路设备',NOW(),NOW()),
(3,'DEV-003','防火墙1','防火墙','Hillstone','SG-6000','SN-003','主校区','网络中心','2021-08-15','2026-08-11','王工','网络信息中心','Hillstone/SG-6000','10.0.1.1','00-11-22-33-44-03','网络中心','2021-08-11','正常',NULL,0,0,'',1,'边界防护',NOW(),NOW()),
(4,'DEV-004','图书馆AP-01','无线AP','Ruijie','RG-AP820','SN-004','主校区','图书馆三层','2022-01-07','2026-01-05','李工','图书馆信息部','Ruijie/RG-AP820','10.0.2.11','00-11-22-33-44-04','图书馆三层','2022-01-05','维修中',NOW(),1,0,'',0,'无线覆盖',NOW(),NOW()),
(5,'DEV-005','教学楼汇聚交换机','交换机','Huawei','S5735','SN-005','东校区','教学楼A栋','2022-09-03','2027-09-01','赵工','网络信息中心','Huawei/S5735','10.0.3.1','00-11-22-33-44-05','教学楼A栋','2022-09-01','正常',NULL,0,0,'',0,'汇聚层设备',NOW(),NOW()),
(6,'DEV-006','认证服务器','服务器','Dell','R740','SN-006','主校区','数据中心','2021-12-10','2026-12-09','王工','数据中心','Dell/R740','10.0.9.10','00-11-22-33-44-06','数据中心','2021-12-09','停用',NULL,0,0,'',1,'备用服务器',NOW(),NOW());

INSERT INTO repair_order
(id,order_no,device_id,reporter_id,title,description,priority,status,assign_maintainer_id,progress,report_time,audit_time,audit_by,assign_time,accept_time,start_repair_time,expected_finish_time,finish_time,confirm_time,satisfaction_score,feedback,close_reason,create_time,update_time) VALUES
(1,'RO202406010001',2,2,'出口网络中断','校园网无法访问外网','高','待分配',NULL,30,NOW(),NOW(),1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(2,'RO202406010002',4,3,'图书馆无线信号弱','三层区域频繁掉线','中','待接单',4,35,NOW(),NOW(),1,NOW(),NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(3,'RO202406010003',1,2,'核心交换机端口告警','端口丢包率高','高','维修中',5,70,NOW(),NOW(),1,NOW(),NOW(),NOW(),DATE_ADD(NOW(), INTERVAL 6 HOUR),NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(4,'RO202406010004',5,3,'教学楼网络延迟','课堂视频卡顿','中','已完成',4,100,NOW(),NOW(),1,NOW(),NOW(),NOW(),DATE_ADD(NOW(), INTERVAL 2 HOUR),NOW(),NOW(),5,'已恢复正常',NULL,NOW(),NOW()),
(5,'RO202406010005',3,2,'防火墙策略异常','部分系统无法访问','低','审核驳回',NULL,0,NOW(),NOW(),1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'信息不足，需补充日志截图',NOW(),NOW());

INSERT INTO repair_order_flow (repair_order_id,from_status,to_status,action,operator_id,operator_role,remark,create_time) VALUES
(1,NULL,'已提交/待审核','SUBMIT',2,'user','用户提交工单',NOW()),
(1,'已提交/待审核','审核通过','ADMIN_APPROVE',1,'admin','审核通过',NOW()),
(1,'审核通过','待分配','ADMIN_TO_ASSIGN',1,'admin','进入待分配',NOW()),
(2,NULL,'已提交/待审核','SUBMIT',3,'user','用户提交工单',NOW()),
(2,'已提交/待审核','审核通过','ADMIN_APPROVE',1,'admin','审核通过',NOW()),
(2,'审核通过','待分配','ADMIN_TO_ASSIGN',1,'admin','进入待分配',NOW()),
(2,'待分配','待接单','ADMIN_ASSIGN',1,'admin','分配给王工',NOW());

INSERT INTO repair_record VALUES
(1,4,5,4,'交换机端口老化','更换上联模块并重启设备','延迟恢复正常',1,NOW(),NOW(),NOW()),
(2,5,3,5,'策略冲突','梳理ACL并调整策略顺序','业务访问恢复',1,NOW(),NOW(),NOW()),
(3,3,1,5,'光模块松动','重新插拔并清洁光纤接口','当前稳定观察中',0,NOW(),NOW(),NOW());

INSERT INTO notice VALUES
(1,'校园网设备巡检通知','本周六进行核心网络设备巡检，期间部分区域网络可能短时波动。',1,NOW(),NOW()),
(2,'报修系统升级完成','校园网络报修系统已升级，支持在线查看维修进度。',1,NOW(),NOW());

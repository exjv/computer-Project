DROP DATABASE IF EXISTS network_repair;
CREATE DATABASE network_repair DEFAULT CHARACTER SET utf8mb4;
USE network_repair;

CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(30) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password VARCHAR(100) NOT NULL,
  real_name VARCHAR(50),
  phone VARCHAR(20),
  email VARCHAR(100),
  department VARCHAR(100),
  role VARCHAR(20) NOT NULL,
  status TINYINT DEFAULT 1,
  last_login_time DATETIME,
  wx_open_id VARCHAR(100),
  qq_open_id VARCHAR(100),
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE network_device (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  device_code VARCHAR(50) NOT NULL UNIQUE,
  device_name VARCHAR(100) NOT NULL,
  device_type VARCHAR(50),
  brand_model VARCHAR(100),
  ip_address VARCHAR(50),
  mac_address VARCHAR(50),
  location VARCHAR(100),
  purchase_date DATE,
  status VARCHAR(20),
  remark VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE repair_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(50) NOT NULL UNIQUE,
  device_id BIGINT NOT NULL,
  reporter_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  description TEXT,
  priority VARCHAR(20),
  status VARCHAR(20),
  assign_maintainer_id BIGINT,
  progress INT DEFAULT 0,
  report_time DATETIME,
  audit_time DATETIME,
  audit_by BIGINT,
  assign_time DATETIME,
  accept_time DATETIME,
  start_repair_time DATETIME,
  expected_finish_time DATETIME,
  finish_time DATETIME,
  confirm_time DATETIME,
  satisfaction_score INT,
  feedback VARCHAR(500),
  close_reason VARCHAR(255),
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE repair_order_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  from_status VARCHAR(20),
  to_status VARCHAR(20),
  action VARCHAR(40),
  operator_id BIGINT,
  operator_role VARCHAR(20),
  remark VARCHAR(500),
  create_time DATETIME
);

CREATE TABLE repair_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  repair_order_id BIGINT NOT NULL,
  device_id BIGINT NOT NULL,
  maintainer_id BIGINT NOT NULL,
  fault_reason VARCHAR(255),
  process_detail TEXT,
  result_detail TEXT,
  is_resolved TINYINT,
  repair_time DATETIME,
  create_time DATETIME,
  update_time DATETIME
);

CREATE TABLE notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(100) NOT NULL,
  content TEXT,
  publisher_id BIGINT,
  create_time DATETIME,
  update_time DATETIME
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
  ip VARCHAR(50),
  operation_time DATETIME
);

CREATE TABLE login_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(50),
  ip VARCHAR(50),
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

INSERT INTO network_device VALUES
(1,'DEV-001','核心交换机A','交换机','H3C S5560','10.0.0.1','00-11-22-33-44-01','信息楼机房','2021-03-12','正常','核心层设备',NOW(),NOW()),
(2,'DEV-002','出口路由器','路由器','Cisco ISR4431','10.0.0.254','00-11-22-33-44-02','网络中心','2020-06-10','故障','出口链路设备',NOW(),NOW()),
(3,'DEV-003','防火墙1','防火墙','Hillstone SG-6000','10.0.1.1','00-11-22-33-44-03','网络中心','2021-08-11','正常','边界防护',NOW(),NOW()),
(4,'DEV-004','图书馆AP-01','无线AP','Ruijie RG-AP820','10.0.2.11','00-11-22-33-44-04','图书馆三层','2022-01-05','维修中','无线覆盖',NOW(),NOW()),
(5,'DEV-005','教学楼汇聚交换机','交换机','Huawei S5735','10.0.3.1','00-11-22-33-44-05','教学楼A栋','2022-09-01','正常','汇聚层设备',NOW(),NOW()),
(6,'DEV-006','认证服务器','服务器','Dell R740','10.0.9.10','00-11-22-33-44-06','数据中心','2021-12-09','停用','备用服务器',NOW(),NOW());

INSERT INTO repair_order
(id,order_no,device_id,reporter_id,title,description,priority,status,assign_maintainer_id,progress,report_time,audit_time,audit_by,assign_time,accept_time,start_repair_time,expected_finish_time,finish_time,confirm_time,satisfaction_score,feedback,close_reason,create_time,update_time) VALUES
(1,'RO202406010001',2,2,'出口网络中断','校园网无法访问外网','高','待分配',NULL,30,NOW(),NOW(),1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(2,'RO202406010002',4,3,'图书馆无线信号弱','三层区域频繁掉线','中','待接单',4,35,NOW(),NOW(),1,NOW(),NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(3,'RO202406010003',1,2,'核心交换机端口告警','端口丢包率高','高','维修中',5,70,NOW(),NOW(),1,NOW(),NOW(),NOW(),DATE_ADD(NOW(), INTERVAL 6 HOUR),NULL,NULL,NULL,NULL,NULL,NOW(),NOW()),
(4,'RO202406010004',5,3,'教学楼网络延迟','课堂视频卡顿','中','已完成',4,100,NOW(),NOW(),1,NOW(),NOW(),NOW(),DATE_ADD(NOW(), INTERVAL 2 HOUR),NOW(),NOW(),5,'已恢复正常',NULL,NOW(),NOW()),
(5,'RO202406010005',3,2,'防火墙策略异常','部分系统无法访问','低','审核驳回',NULL,0,NOW(),NOW(),1,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'信息不足，需补充日志截图',NOW(),NOW());

INSERT INTO repair_order_flow (repair_order_id,from_status,to_status,action,operator_id,operator_role,remark,create_time) VALUES
(1,NULL,'已提交','SUBMIT',2,'user','用户提交工单',NOW()),
(1,'已提交','审核通过','ADMIN_APPROVE',1,'admin','审核通过',NOW()),
(1,'审核通过','待分配','ADMIN_TO_ASSIGN',1,'admin','进入待分配',NOW()),
(2,NULL,'已提交','SUBMIT',3,'user','用户提交工单',NOW()),
(2,'已提交','审核通过','ADMIN_APPROVE',1,'admin','审核通过',NOW()),
(2,'审核通过','待分配','ADMIN_TO_ASSIGN',1,'admin','进入待分配',NOW()),
(2,'待分配','待接单','ADMIN_ASSIGN',1,'admin','分配给王工',NOW());

INSERT INTO repair_record VALUES
(1,4,5,4,'交换机端口老化','更换上联模块并重启设备','延迟恢复正常',1,NOW(),NOW(),NOW()),
(2,5,3,5,'策略冲突','梳理ACL并调整策略顺序','业务访问恢复',1,NOW(),NOW(),NOW()),
(3,3,1,5,'光模块松动','重新插拔并清洁光纤接口','当前稳定观察中',0,NOW(),NOW(),NOW());

INSERT INTO notice VALUES
(1,'校园网设备巡检通知','本周六进行核心网络设备巡检，期间部分区域网络可能短时波动。',1,NOW(),NOW()),
(2,'报修系统升级完成','校园网络报修系统已升级，支持在线查看维修进度。',1,NOW(),NOW());

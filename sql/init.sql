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
  report_time DATETIME,
  assign_time DATETIME,
  finish_time DATETIME,
  create_time DATETIME,
  update_time DATETIME
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

INSERT INTO repair_order VALUES
(1,'RO202406010001',2,2,'出口网络中断','校园网无法访问外网','高','待处理',NULL,NOW(),NULL,NULL,NOW(),NOW()),
(2,'RO202406010002',4,3,'图书馆无线信号弱','三层区域频繁掉线','中','已分配',4,NOW(),NOW(),NULL,NOW(),NOW()),
(3,'RO202406010003',1,2,'核心交换机端口告警','端口丢包率高','高','处理中',5,NOW(),NOW(),NULL,NOW(),NOW()),
(4,'RO202406010004',5,3,'教学楼网络延迟','课堂视频卡顿','中','已完成',4,NOW(),NOW(),NOW(),NOW(),NOW()),
(5,'RO202406010005',3,2,'防火墙策略异常','部分系统无法访问','低','已关闭',5,NOW(),NOW(),NOW(),NOW(),NOW());

INSERT INTO repair_record VALUES
(1,4,5,4,'交换机端口老化','更换上联模块并重启设备','延迟恢复正常',1,NOW(),NOW(),NOW()),
(2,5,3,5,'策略冲突','梳理ACL并调整策略顺序','业务访问恢复',1,NOW(),NOW(),NOW()),
(3,3,1,5,'光模块松动','重新插拔并清洁光纤接口','当前稳定观察中',0,NOW(),NOW(),NOW());

INSERT INTO notice VALUES
(1,'校园网设备巡检通知','本周六进行核心网络设备巡检，期间部分区域网络可能短时波动。',1,NOW(),NOW()),
(2,'报修系统升级完成','校园网络报修系统已升级，支持在线查看维修进度。',1,NOW(),NOW());

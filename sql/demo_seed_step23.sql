-- Step23 演示数据脚本（建议在 init.sql 初始化后执行）
USE network_repair;

-- 1) 补充演示用户
INSERT INTO `user` (id, employee_no, username, password, real_name, phone, email, department, role, status, create_time, update_time)
VALUES
(4,'M2026002','maint2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','李工','13800000004','m2@campus.edu','网络运维组','maintainer',1,NOW(),NOW()),
(5,'U2026002','user2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','赵老师','13800000005','u2@campus.edu','图书馆','user',1,NOW(),NOW())
ON DUPLICATE KEY UPDATE real_name=VALUES(real_name), phone=VALUES(phone), update_time=NOW();

INSERT INTO user_role (user_id, role_id, create_time)
SELECT 4,2,NOW() FROM dual WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id=4 AND role_id=2);
INSERT INTO user_role (user_id, role_id, create_time)
SELECT 5,3,NOW() FROM dual WHERE NOT EXISTS (SELECT 1 FROM user_role WHERE user_id=5 AND role_id=3);

-- 2) 补充设备（体现不同频率）
INSERT INTO device
(id, device_code, device_name, device_type, device_type_name, brand, model, serial_no, campus, building, machine_room, location,
 purchase_date, enable_date, warranty_expire_date, owner_user_id, owner_employee_no, owner_name, management_dept, status,
 total_repair_order_count, total_repair_count, create_time, update_time)
VALUES
(2,'DEV-002','汇聚交换机B','SWITCH','交换机','H3C','S6520','SN002','主校区','信息楼','2F机房','信息楼2F机房','2020-01-10','2020-02-01','2026-01-10',4,'M2026002','李工','网络运维组','故障',6,4,NOW(),NOW()),
(3,'DEV-003','出口防火墙C','FIREWALL','防火墙','Hillstone','E5650','SN003','主校区','网络中心','核心机房','网络中心核心机房','2022-03-15','2022-04-01','2028-03-15',3,'M2026001','王工','网络运维组','正常',2,2,NOW(),NOW()),
(4,'DEV-004','无线AP-D','AP','无线AP','Huawei','AirEngine','SN004','主校区','图书馆','弱电间','图书馆三层','2023-05-20','2023-06-01','2027-05-20',4,'M2026002','李工','网络运维组','维修中',3,1,NOW(),NOW()),
(5,'DEV-005','路由器E','ROUTER','路由器','Cisco','ISR4331','SN005','主校区','教学楼A','1F机柜','教学楼A一层','2019-09-10','2019-10-01','2024-09-10',3,'M2026001','王工','网络运维组','故障',8,5,NOW(),NOW())
ON DUPLICATE KEY UPDATE device_name=VALUES(device_name), owner_user_id=VALUES(owner_user_id), status=VALUES(status),
 total_repair_order_count=VALUES(total_repair_order_count), total_repair_count=VALUES(total_repair_count), update_time=NOW();

-- 3) 清理旧演示工单区间
DELETE FROM repair_feedback WHERE repair_order_id BETWEEN 1001 AND 1012;
DELETE FROM repair_record WHERE repair_order_id BETWEEN 1001 AND 1012;
DELETE FROM repair_order_flow WHERE repair_order_id BETWEEN 1001 AND 1012;
DELETE FROM business_log WHERE business_no IN ('RO202603230001','RO202603230002','RO202603230003','RO202603230004','RO202603230005','RO202603230006','RO202603230007','RO202603230008');
DELETE FROM repair_order WHERE id BETWEEN 1001 AND 1012;

-- 4) 工单主数据（含延期/采购/返修/未解决）
INSERT INTO repair_order
(id,order_no,reporter_id,reporter_employee_no,reporter_name,contact_phone,reporter_department,report_location,
 device_id,device_code,device_name,device_type,title,fault_type,description,priority,affect_wide_area_network,
 report_time,audit_time,audit_by,audit_by_employee_no,audit_by_name,assign_time,assign_by,assign_by_employee_no,assign_by_name,
 assign_maintainer_id,assign_maintainer_employee_no,assign_maintainer_name,accept_time,start_repair_time,status,progress,
 need_purchase_parts,parts_description,apply_delay,original_expected_finish_time,delayed_expected_finish_time,expected_finish_time,finish_time,
 confirm_time,user_confirm_result,satisfaction_score,feedback,close_reason,remark,create_time,update_time)
VALUES
(1001,'RO202603230001',2,'U2026001','张三','13800000002','教务处','信息楼机房',2,'DEV-002','汇聚交换机B','交换机','交换机频繁丢包','链路异常','近一周多次抖动','高',1,
 '2026-03-18 08:10:00','2026-03-18 08:30:00',1,'A2026001','系统管理员','2026-03-18 08:40:00',1,'A2026001','系统管理员',3,'M2026001','王工','2026-03-18 08:50:00','2026-03-18 09:00:00','已完成',100,
 1,'更换光模块','0','2026-03-18 13:00:00',NULL,'2026-03-18 12:30:00','2026-03-18 12:20:00',
 '2026-03-18 13:10:00','已解决',4,'恢复稳定',NULL,'高频设备治理样例','2026-03-18 08:10:00','2026-03-18 13:10:00'),

(1002,'RO202603230002',5,'U2026002','赵老师','13800000005','图书馆','图书馆三层',4,'DEV-004','无线AP-D','无线AP','AP离线告警','无线中断','图书馆三层WiFi中断','中',0,
 '2026-03-19 09:20:00','2026-03-19 09:40:00',1,'A2026001','系统管理员','2026-03-19 09:45:00',1,'A2026001','系统管理员',4,'M2026002','李工','2026-03-19 10:00:00','2026-03-19 10:15:00','维修中',60,
 0,NULL,1,'2026-03-19 18:00:00','2026-03-20 12:00:00','2026-03-20 10:00:00',NULL,
 NULL,NULL,NULL,NULL,NULL,'延期审批后继续处理','2026-03-19 09:20:00','2026-03-20 09:00:00'),

(1003,'RO202603230003',2,'U2026001','张三','13800000002','教务处','网络中心核心机房',3,'DEV-003','出口防火墙C','防火墙','防火墙策略异常','配置错误','外网访问异常','高',1,
 '2026-03-19 11:05:00','2026-03-19 11:20:00',1,'A2026001','系统管理员','2026-03-19 11:25:00',1,'A2026001','系统管理员',3,'M2026001','王工','2026-03-19 11:35:00','2026-03-19 11:45:00','待验收/待确认',90,
 0,NULL,0,'2026-03-19 16:00:00',NULL,'2026-03-19 15:30:00','2026-03-19 15:10:00',
 NULL,NULL,NULL,NULL,NULL,'等待用户确认','2026-03-19 11:05:00','2026-03-19 15:10:00'),

(1004,'RO202603230004',5,'U2026002','赵老师','13800000005','图书馆','教学楼A一层',5,'DEV-005','路由器E','路由器','路由器重启循环','设备老化','重启后仍不稳定','高',1,
 '2026-03-20 08:00:00','2026-03-20 08:15:00',1,'A2026001','系统管理员','2026-03-20 08:20:00',1,'A2026001','系统管理员',4,'M2026002','李工','2026-03-20 08:30:00','2026-03-20 08:45:00','维修中',65,
 1,'待采购电源模块',0,'2026-03-20 17:00:00',NULL,'2026-03-20 16:30:00',NULL,
 '2026-03-20 12:00:00','未解决',2,'仍有间歇中断',NULL,'返修场景','2026-03-20 08:00:00','2026-03-20 12:00:00'),

(1005,'RO202603230005',2,'U2026001','张三','13800000002','教务处','信息楼机房',2,'DEV-002','汇聚交换机B','交换机','交换机端口CRC异常','端口异常','端口CRC持续增长','中',0,
 '2026-03-21 07:45:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'待分配',30,
 0,NULL,0,'2026-03-21 18:00:00',NULL,NULL,NULL,
 NULL,NULL,NULL,NULL,NULL,'待分配样例','2026-03-21 07:45:00','2026-03-21 07:45:00'),

(1006,'RO202603230006',5,'U2026002','赵老师','13800000005','图书馆','图书馆三层',4,'DEV-004','无线AP-D','无线AP','AP认证失败','认证故障','部分终端认证失败','低',0,
 '2026-03-21 10:30:00','2026-03-21 10:45:00',1,'A2026001','系统管理员','2026-03-21 10:50:00',1,'A2026001','系统管理员',4,'M2026002','李工','2026-03-21 11:00:00','2026-03-21 11:10:00','待采购/待配件',70,
 1,'待采购PoE模块',0,'2026-03-21 20:00:00',NULL,'2026-03-21 19:00:00',NULL,
 NULL,NULL,NULL,NULL,NULL,'采购场景','2026-03-21 10:30:00','2026-03-21 11:20:00'),

(1007,'RO202603230007',2,'U2026001','张三','13800000002','教务处','教学楼A一层',5,'DEV-005','路由器E','路由器','路由器老化更换建议','硬件故障','设备过保且反复故障','高',1,
 '2026-03-22 09:00:00','2026-03-22 09:10:00',1,'A2026001','系统管理员','2026-03-22 09:15:00',1,'A2026001','系统管理员',3,'M2026001','王工','2026-03-22 09:25:00','2026-03-22 09:35:00','已关闭',100,
 0,NULL,0,'2026-03-22 18:00:00',NULL,'2026-03-22 17:00:00','2026-03-22 16:20:00',
 '2026-03-22 16:40:00','已解决',5,'已恢复，建议后续更换', '设备计划退役', '关闭后不可继续维修样例','2026-03-22 09:00:00','2026-03-22 16:40:00'),

(1008,'RO202603230008',5,'U2026002','赵老师','13800000005','图书馆','图书馆三层',4,'DEV-004','无线AP-D','无线AP','AP偶发离线','无线中断','暂未定位根因','中',0,
 '2026-03-22 11:00:00',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,'已提交/待审核',10,
 0,NULL,0,'2026-03-22 20:00:00',NULL,NULL,NULL,
 NULL,NULL,NULL,NULL,NULL,'待审核样例','2026-03-22 11:00:00','2026-03-22 11:00:00');

-- 5) 维修记录（与已完成/返修逻辑对应）
INSERT INTO repair_record
(id,repair_order_id,repair_order_no,device_id,device_code,maintainer_id,maintainer_employee_no,maintainer_name,fault_reason,process_detail,result_detail,is_resolved,used_parts,used_parts_desc,labor_hours,repair_conclusion,repair_time,create_time,update_time)
VALUES
(2001,1001,'RO202603230001',2,'DEV-002',3,'M2026001','王工','链路异常','更换模块并清理端口','恢复正常',1,1,'光模块',3,'已恢复','2026-03-18 12:20:00','2026-03-18 12:20:00','2026-03-18 12:20:00'),
(2002,1004,'RO202603230004',5,'DEV-005',4,'M2026002','李工','设备老化','更换配置后仍异常','未彻底解决',0,1,'电源模块待更换',4,'返修中','2026-03-20 12:00:00','2026-03-20 12:00:00','2026-03-20 12:00:00'),
(2003,1007,'RO202603230007',5,'DEV-005',3,'M2026001','王工','硬件故障','临时切换备用链路','已恢复',1,0,NULL,6,'关闭前恢复','2026-03-22 16:20:00','2026-03-22 16:20:00','2026-03-22 16:20:00');

-- 6) 用户反馈（含未解决/评分）
INSERT INTO repair_feedback
(id,repair_order_id,user_id,user_employee_no,confirm_result,satisfaction_score,feedback_content,confirm_time,create_time,update_time)
VALUES
(3001,1001,2,'U2026001','已解决',4,'处理及时，网络已恢复','2026-03-18 13:10:00','2026-03-18 13:10:00','2026-03-18 13:10:00'),
(3002,1004,5,'U2026002','未解决',2,'仍偶发中断，需继续处理','2026-03-20 12:00:00','2026-03-20 12:00:00','2026-03-20 12:00:00'),
(3003,1007,2,'U2026001','已解决',5,'已恢复稳定','2026-03-22 16:40:00','2026-03-22 16:40:00','2026-03-22 16:40:00');

-- 7) 业务日志（全链路可追踪）
INSERT INTO business_log (id,business_type,business_no,action,operator_id,operator_employee_no,operator_name,content,status,create_time) VALUES
(4001,'REPAIR_ORDER','RO202603230001','SUBMIT',2,'U2026001','张三','用户提交报修：交换机频繁丢包','已提交/待审核','2026-03-18 08:10:00'),
(4002,'REPAIR_ORDER','RO202603230001','ADMIN_ASSIGN',1,'A2026001','系统管理员','分配给王工','待接单','2026-03-18 08:40:00'),
(4003,'REPAIR_ORDER','RO202603230001','MAINTAINER_FINISH',3,'M2026001','王工','提交完工','待验收/待确认','2026-03-18 12:20:00'),
(4004,'REPAIR_ORDER','RO202603230001','USER_FEEDBACK',2,'U2026001','张三','确认已解决，评分4分','已完成','2026-03-18 13:10:00'),
(4005,'REPAIR_ORDER','RO202603230004','USER_CONFIRM_UNRESOLVED',5,'U2026002','赵老师','确认未解决并退回维修，评分2分','维修中','2026-03-20 12:00:00'),
(4006,'REPAIR_ORDER','RO202603230002','MAINTAINER_DELAY_APPLY',4,'M2026002','李工','申请延期至次日','申请延期中','2026-03-19 17:30:00'),
(4007,'REPAIR_ORDER','RO202603230006','MAINTAINER_PARTS_APPLY',4,'M2026002','李工','申请采购PoE模块','待采购/待配件','2026-03-21 11:20:00');

-- 8) 登录日志（含失败原因示例）
INSERT INTO login_log (id,user_id,username,ip,user_agent,login_status,fail_reason,login_time) VALUES
(5001,3,'maint1','10.10.1.23','Mozilla/5.0','FAIL','验证码错误','2026-03-21 09:22:00'),
(5002,3,'maint1','10.10.1.23','Mozilla/5.0','SUCCESS',NULL,'2026-03-21 09:24:00'),
(5003,5,'user2','10.10.2.15','Mozilla/5.0','SUCCESS',NULL,'2026-03-22 08:58:00');

-- 9) 修正设备统计字段，确保与工单/记录一致
UPDATE device d SET
  d.total_repair_order_count = (SELECT COUNT(*) FROM repair_order ro WHERE ro.device_id = d.id),
  d.total_repair_count = (SELECT COUNT(*) FROM repair_record rr WHERE rr.device_id = d.id AND rr.is_resolved = 1),
  d.last_fault_time = (SELECT MAX(ro.report_time) FROM repair_order ro WHERE ro.device_id = d.id),
  d.update_time = NOW();

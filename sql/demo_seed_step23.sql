-- Step23 演示数据脚本（建议在 init.sql + migration 脚本执行后运行）
USE network_repair;

-- =====================
-- 0) 基础用户与角色补齐（幂等）
-- =====================
INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
SELECT 'M2026002','maint2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','李工','13800000006','maint2@campus.edu','网络运维组','maintainer',1,0,NOW(),NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE employee_no='M2026002');
INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
SELECT 'U2026002','user2','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','赵老师','13800000005','user2@campus.edu','图书馆','user',1,0,NOW(),NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE employee_no='U2026002');

INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='M2026002' AND r.role_code='MAINTAINER'
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='U2026002' AND r.role_code='USER'
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);

-- =====================
-- 1) 设备档案（体现不同报修频率）
-- =====================
INSERT INTO device_type (type_code, type_name, status, sort_no, create_time, update_time)
SELECT 'FIREWALL', '防火墙', 1, 20, NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code='FIREWALL');
INSERT INTO device_type (type_code, type_name, status, sort_no, create_time, update_time)
SELECT 'AP', '无线AP', 1, 30, NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code='AP');
INSERT INTO device_type (type_code, type_name, status, sort_no, create_time, update_time)
SELECT 'ROUTER', '路由器', 1, 40, NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code='ROUTER');

INSERT INTO device (device_code, device_name, device_type, device_type_name, brand, model, serial_number, serial_no,
                    campus, building_location, building, machine_room, location,
                    purchase_date, enable_date, warranty_expiry_date,
                    owner_name, management_dept, manage_department, status,
                    total_repair_requests, total_repair_order_count, total_repair_count, fault_reason_stats,
                    create_time, update_time)
SELECT 'DEV-AGG-002', '汇聚交换机B', 'SWITCH', '交换机', 'H3C', 'S6520', 'SN-AGG-002', 'SN-AGG-002',
       '主校区', '信息楼2F机房', '信息楼', '2F机房', '信息楼2F机房',
       '2020-01-10', '2020-02-01', '2026-01-10',
       '李工', '网络运维组', '网络运维组', '故障',
       0, 0, 0, '链路异常:3,端口CRC:2',
       NOW(), NOW()
FROM dual WHERE NOT EXISTS (SELECT 1 FROM device WHERE device_code='DEV-AGG-002');

INSERT INTO device (device_code, device_name, device_type, device_type_name, brand, model, serial_number, serial_no,
                    campus, building_location, building, machine_room, location,
                    purchase_date, enable_date, warranty_expiry_date,
                    owner_name, management_dept, manage_department, status,
                    total_repair_requests, total_repair_order_count, total_repair_count, fault_reason_stats,
                    create_time, update_time)
SELECT 'DEV-FW-003', '出口防火墙C', 'FIREWALL', '防火墙', 'Hillstone', 'E5650', 'SN-FW-003', 'SN-FW-003',
       '主校区', '网络中心核心机房', '网络中心', '核心机房', '网络中心核心机房',
       '2022-03-15', '2022-04-01', '2028-03-15',
       '王工', '网络运维组', '网络运维组', '正常',
       0, 0, 0, '策略异常:1',
       NOW(), NOW()
FROM dual WHERE NOT EXISTS (SELECT 1 FROM device WHERE device_code='DEV-FW-003');

INSERT INTO device (device_code, device_name, device_type, device_type_name, brand, model, serial_number, serial_no,
                    campus, building_location, building, machine_room, location,
                    purchase_date, enable_date, warranty_expiry_date,
                    owner_name, management_dept, manage_department, status,
                    total_repair_requests, total_repair_order_count, total_repair_count, fault_reason_stats,
                    create_time, update_time)
SELECT 'DEV-AP-004', '无线AP-D', 'AP', '无线AP', 'Huawei', 'AirEngine', 'SN-AP-004', 'SN-AP-004',
       '主校区', '图书馆三层', '图书馆', '弱电间', '图书馆三层',
       '2023-05-20', '2023-06-01', '2027-05-20',
       '李工', '网络运维组', '网络运维组', '维修中',
       0, 0, 0, '离线告警:3,认证失败:1',
       NOW(), NOW()
FROM dual WHERE NOT EXISTS (SELECT 1 FROM device WHERE device_code='DEV-AP-004');

INSERT INTO device (device_code, device_name, device_type, device_type_name, brand, model, serial_number, serial_no,
                    campus, building_location, building, machine_room, location,
                    purchase_date, enable_date, warranty_expiry_date,
                    owner_name, management_dept, manage_department, status,
                    total_repair_requests, total_repair_order_count, total_repair_count, fault_reason_stats,
                    create_time, update_time)
SELECT 'DEV-RT-005', '路由器E', 'ROUTER', '路由器', 'Cisco', 'ISR4331', 'SN-RT-005', 'SN-RT-005',
       '主校区', '教学楼A一层', '教学楼A', '1F机柜', '教学楼A一层',
       '2019-09-10', '2019-10-01', '2024-09-10',
       '王工', '网络运维组', '网络运维组', '故障',
       0, 0, 0, '硬件老化:4,重启循环:2',
       NOW(), NOW()
FROM dual WHERE NOT EXISTS (SELECT 1 FROM device WHERE device_code='DEV-RT-005');

-- =====================
-- 2) 清理并重建演示工单（全链路场景）
-- =====================
DELETE FROM repair_feedback WHERE repair_order_id IN (SELECT id FROM repair_order WHERE order_no LIKE 'RO20260329%');
DELETE FROM repair_record WHERE repair_order_id IN (SELECT id FROM repair_order WHERE order_no LIKE 'RO20260329%');
DELETE FROM repair_order_flow WHERE repair_order_id IN (SELECT id FROM repair_order WHERE order_no LIKE 'RO20260329%');
DELETE FROM business_log WHERE business_no LIKE 'RO20260329%';
DELETE FROM repair_order WHERE order_no LIKE 'RO20260329%';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, audit_time, audit_by, audit_by_employee_no, audit_by_name, assign_time, assign_by, assign_by_employee_no, assign_by_name,
 assign_maintainer_id, assign_maintainer_employee_no, assign_maintainer_name, accept_time, start_repair_time, status, progress,
 need_purchase_parts, parts_description, apply_delay, original_expected_finish_time, delayed_expected_finish_time, expected_finish_time, finish_time,
 confirm_time, user_confirm_result, satisfaction_score, feedback, close_reason, remark, create_time, update_time)
SELECT 'RO202603290001', u.id, u.employee_no, u.real_name, u.phone, u.department, '信息楼2F机房',
       d.id, d.device_code, d.device_name, d.device_type_name, '汇聚交换机链路抖动', '链路异常', '一小时内发生3次网络抖动', '高', 1,
       '2026-03-18 08:10:00', '2026-03-18 08:25:00', a.id, a.employee_no, a.real_name, '2026-03-18 08:35:00', a.id, a.employee_no, a.real_name,
       m1.id, m1.employee_no, m1.real_name, '2026-03-18 08:45:00', '2026-03-18 08:55:00', '已完成', 100,
       1, '更换光模块', 0, '2026-03-18 13:00:00', NULL, '2026-03-18 12:40:00', '2026-03-18 12:35:00',
       '2026-03-18 13:05:00', '已解决', 4, '网络恢复稳定', NULL, '高频故障已处置', '2026-03-18 08:10:00', '2026-03-18 13:05:00'
FROM `user` u JOIN `user` a ON a.employee_no='A2026001'
JOIN `user` m1 ON m1.employee_no='M2026001'
JOIN device d ON d.device_code='DEV-AGG-002'
WHERE u.employee_no='U2026001';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, audit_time, audit_by, audit_by_employee_no, audit_by_name, assign_time, assign_by, assign_by_employee_no, assign_by_name,
 assign_maintainer_id, assign_maintainer_employee_no, assign_maintainer_name, accept_time, start_repair_time, status, progress,
 need_purchase_parts, parts_description, apply_delay, original_expected_finish_time, delayed_expected_finish_time, expected_finish_time, finish_time,
 confirm_time, user_confirm_result, satisfaction_score, feedback, close_reason, remark, create_time, update_time)
SELECT 'RO202603290002', u2.id, u2.employee_no, u2.real_name, u2.phone, u2.department, '图书馆三层',
       d.id, d.device_code, d.device_name, d.device_type_name, '无线AP离线告警', '无线中断', '三层WiFi不可用', '中', 0,
       '2026-03-19 09:20:00', '2026-03-19 09:35:00', a.id, a.employee_no, a.real_name, '2026-03-19 09:45:00', a.id, a.employee_no, a.real_name,
       m2.id, m2.employee_no, m2.real_name, '2026-03-19 10:00:00', '2026-03-19 10:15:00', '维修中', 65,
       0, NULL, 1, '2026-03-19 18:00:00', '2026-03-20 12:00:00', '2026-03-20 11:00:00', NULL,
       NULL, NULL, NULL, NULL, NULL, '延期审批后继续维修', '2026-03-19 09:20:00', '2026-03-20 09:10:00'
FROM `user` u2 JOIN `user` a ON a.employee_no='A2026001'
JOIN `user` m2 ON m2.employee_no='M2026002'
JOIN device d ON d.device_code='DEV-AP-004'
WHERE u2.employee_no='U2026002';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, audit_time, audit_by, audit_by_employee_no, audit_by_name, assign_time, assign_by, assign_by_employee_no, assign_by_name,
 assign_maintainer_id, assign_maintainer_employee_no, assign_maintainer_name, accept_time, start_repair_time, status, progress,
 need_purchase_parts, parts_description, apply_delay, original_expected_finish_time, delayed_expected_finish_time, expected_finish_time, finish_time,
 confirm_time, user_confirm_result, satisfaction_score, feedback, close_reason, remark, create_time, update_time)
SELECT 'RO202603290003', u.id, u.employee_no, u.real_name, u.phone, u.department, '教学楼A一层',
       d.id, d.device_code, d.device_name, d.device_type_name, '路由器重启循环', '硬件故障', '设备过保且不稳定', '高', 1,
       '2026-03-20 08:00:00', '2026-03-20 08:10:00', a.id, a.employee_no, a.real_name, '2026-03-20 08:20:00', a.id, a.employee_no, a.real_name,
       m2.id, m2.employee_no, m2.real_name, '2026-03-20 08:35:00', '2026-03-20 08:45:00', '维修中', 60,
       1, '申请采购电源模块', 0, '2026-03-20 17:00:00', NULL, '2026-03-20 16:30:00', NULL,
       '2026-03-20 12:00:00', '未解决', 2, '仍有中断，退回处理', NULL, '返修场景', '2026-03-20 08:00:00', '2026-03-20 12:00:00'
FROM `user` u JOIN `user` a ON a.employee_no='A2026001'
JOIN `user` m2 ON m2.employee_no='M2026002'
JOIN device d ON d.device_code='DEV-RT-005'
WHERE u.employee_no='U2026002';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, status, progress, need_purchase_parts, apply_delay, original_expected_finish_time, remark, create_time, update_time)
SELECT 'RO202603290004', u.id, u.employee_no, u.real_name, u.phone, u.department, '信息楼2F机房',
       d.id, d.device_code, d.device_name, d.device_type_name, '汇聚交换机端口CRC增长', '端口异常', '暂未分配，等待排班', '中', 0,
       '2026-03-21 07:45:00', '待分配', 30, 0, 0, '2026-03-21 18:00:00', '待分配样例', '2026-03-21 07:45:00', '2026-03-21 07:45:00'
FROM `user` u JOIN device d ON d.device_code='DEV-AGG-002' WHERE u.employee_no='U2026001';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, status, progress, need_purchase_parts, apply_delay, original_expected_finish_time, remark, create_time, update_time)
SELECT 'RO202603290005', u.id, u.employee_no, u.real_name, u.phone, u.department, '图书馆三层',
       d.id, d.device_code, d.device_name, d.device_type_name, 'AP偶发离线', '无线中断', '待管理员审核后分配', '中', 0,
       '2026-03-22 11:00:00', '已提交/待审核', 10, 0, 0, '2026-03-22 20:00:00', '待审核样例', '2026-03-22 11:00:00', '2026-03-22 11:00:00'
FROM `user` u JOIN device d ON d.device_code='DEV-AP-004' WHERE u.employee_no='U2026002';

INSERT INTO repair_order
(order_no, reporter_id, reporter_employee_no, reporter_name, contact_phone, reporter_department, report_location,
 device_id, device_code, device_name, device_type, title, fault_type, description, priority, affect_wide_area_network,
 report_time, audit_time, audit_by, audit_by_employee_no, audit_by_name, assign_time, assign_by, assign_by_employee_no, assign_by_name,
 assign_maintainer_id, assign_maintainer_employee_no, assign_maintainer_name, accept_time, start_repair_time, status, progress,
 need_purchase_parts, apply_delay, original_expected_finish_time, expected_finish_time, finish_time,
 confirm_time, user_confirm_result, satisfaction_score, feedback, close_reason, remark, create_time, update_time)
SELECT 'RO202603290006', u.id, u.employee_no, u.real_name, u.phone, u.department, '教学楼A一层',
       d.id, d.device_code, d.device_name, d.device_type_name, '路由器老化治理', '硬件故障', '完成后管理员关闭', '高', 1,
       '2026-03-22 09:00:00', '2026-03-22 09:10:00', a.id, a.employee_no, a.real_name, '2026-03-22 09:15:00', a.id, a.employee_no, a.real_name,
       m1.id, m1.employee_no, m1.real_name, '2026-03-22 09:25:00', '2026-03-22 09:35:00', '已关闭', 100,
       0, 0, '2026-03-22 18:00:00', '2026-03-22 17:00:00', '2026-03-22 16:20:00',
       '2026-03-22 16:40:00', '已解决', 5, '建议纳入设备更换计划', '设备计划退役', '关闭后不可继续维修样例', '2026-03-22 09:00:00', '2026-03-22 16:40:00'
FROM `user` u JOIN `user` a ON a.employee_no='A2026001'
JOIN `user` m1 ON m1.employee_no='M2026001'
JOIN device d ON d.device_code='DEV-RT-005'
WHERE u.employee_no='U2026001';

-- =====================
-- 3) 流程记录 + 维修记录 + 反馈 + 日志
-- =====================
INSERT INTO repair_order_flow (repair_order_id, from_status, to_status, action, operator_id, operator_employee_no, operator_name, operator_role, remark, operation_time, create_time, update_time)
SELECT ro.id, '已提交/待审核', '待分配', 'AUDIT_PASS', a.id, a.employee_no, a.real_name, 'admin', '审核通过', '2026-03-18 08:25:00', NOW(), NOW()
FROM repair_order ro JOIN `user` a ON a.employee_no='A2026001' WHERE ro.order_no='RO202603290001';
INSERT INTO repair_order_flow (repair_order_id, from_status, to_status, action, operator_id, operator_employee_no, operator_name, operator_role, remark, operation_time, create_time, update_time)
SELECT ro.id, '待分配', '维修中', 'ASSIGN_AND_START', m.id, m.employee_no, m.real_name, 'maintainer', '接单并开始维修', '2026-03-18 08:55:00', NOW(), NOW()
FROM repair_order ro JOIN `user` m ON m.employee_no='M2026001' WHERE ro.order_no='RO202603290001';
INSERT INTO repair_order_flow (repair_order_id, from_status, to_status, action, operator_id, operator_employee_no, operator_name, operator_role, remark, operation_time, create_time, update_time)
SELECT ro.id, '维修中', '已完成', 'FINISH', m.id, m.employee_no, m.real_name, 'maintainer', '完成维修', '2026-03-18 12:35:00', NOW(), NOW()
FROM repair_order ro JOIN `user` m ON m.employee_no='M2026001' WHERE ro.order_no='RO202603290001';

INSERT INTO repair_record (repair_order_id, repair_order_no, device_id, device_code, maintainer_id, maintainer_employee_no, maintainer_name,
                           fault_reason, process_detail, result_detail, is_resolved, used_parts, used_parts_desc, labor_hours, repair_conclusion, repair_time, create_time, update_time)
SELECT ro.id, ro.order_no, ro.device_id, ro.device_code, m.id, m.employee_no, m.real_name,
       ro.fault_type, '更换故障光模块并校验链路', '链路恢复稳定', 1, 1, '光模块', 3, '已恢复', '2026-03-18 12:35:00', NOW(), NOW()
FROM repair_order ro JOIN `user` m ON m.employee_no='M2026001' WHERE ro.order_no='RO202603290001';
INSERT INTO repair_record (repair_order_id, repair_order_no, device_id, device_code, maintainer_id, maintainer_employee_no, maintainer_name,
                           fault_reason, process_detail, result_detail, is_resolved, used_parts, used_parts_desc, labor_hours, repair_conclusion, repair_time, create_time, update_time)
SELECT ro.id, ro.order_no, ro.device_id, ro.device_code, m.id, m.employee_no, m.real_name,
       ro.fault_type, '更换配置后仍有抖动', '未彻底解决，返修中', 0, 1, '电源模块待采购', 4, '返修', '2026-03-20 12:00:00', NOW(), NOW()
FROM repair_order ro JOIN `user` m ON m.employee_no='M2026002' WHERE ro.order_no='RO202603290003';

INSERT INTO repair_feedback (repair_order_id, user_id, user_employee_no, confirm_result, satisfaction_score, feedback_content, confirm_time, create_time, update_time)
SELECT ro.id, u.id, u.employee_no, '已解决', 4, '处理及时，网络恢复', '2026-03-18 13:05:00', NOW(), NOW()
FROM repair_order ro JOIN `user` u ON u.employee_no='U2026001' WHERE ro.order_no='RO202603290001';
INSERT INTO repair_feedback (repair_order_id, user_id, user_employee_no, confirm_result, satisfaction_score, feedback_content, confirm_time, create_time, update_time)
SELECT ro.id, u.id, u.employee_no, '未解决', 2, '仍有间歇中断，需继续处理', '2026-03-20 12:00:00', NOW(), NOW()
FROM repair_order ro JOIN `user` u ON u.employee_no='U2026002' WHERE ro.order_no='RO202603290003';

INSERT INTO business_log (business_type, business_no, biz_type, biz_id, order_no, action, operator_id, operator_employee_no, operator_name, operator_role, content, status, create_time, operation_time)
SELECT 'REPAIR_ORDER', ro.order_no, 'REPAIR_ORDER', ro.id, ro.order_no, 'SUBMIT', u.id, u.employee_no, u.real_name, 'user',
       '提交报修：汇聚交换机链路抖动', '已提交/待审核', '2026-03-18 08:10:00', '2026-03-18 08:10:00'
FROM repair_order ro JOIN `user` u ON u.employee_no='U2026001' WHERE ro.order_no='RO202603290001';
INSERT INTO business_log (business_type, business_no, biz_type, biz_id, order_no, action, operator_id, operator_employee_no, operator_name, operator_role, content, status, create_time, operation_time)
SELECT 'REPAIR_ORDER', ro.order_no, 'REPAIR_ORDER', ro.id, ro.order_no, 'MAINTAINER_DELAY_APPLY', m.id, m.employee_no, m.real_name, 'maintainer',
       '申请延期至次日中午', '申请延期中', '2026-03-19 17:30:00', '2026-03-19 17:30:00'
FROM repair_order ro JOIN `user` m ON m.employee_no='M2026002' WHERE ro.order_no='RO202603290002';
INSERT INTO business_log (business_type, business_no, biz_type, biz_id, order_no, action, operator_id, operator_employee_no, operator_name, operator_role, content, status, create_time, operation_time)
SELECT 'REPAIR_ORDER', ro.order_no, 'REPAIR_ORDER', ro.id, ro.order_no, 'USER_CONFIRM_UNRESOLVED', u.id, u.employee_no, u.real_name, 'user',
       '用户确认未解决并退回处理，满意度2分', '维修中', '2026-03-20 12:00:00', '2026-03-20 12:00:00'
FROM repair_order ro JOIN `user` u ON u.employee_no='U2026002' WHERE ro.order_no='RO202603290003';

INSERT INTO login_log (user_id, username, ip, user_agent, login_status, fail_reason, login_time)
SELECT u.id, u.username, '10.10.1.23', 'Mozilla/5.0', 'FAIL', '验证码错误', '2026-03-21 09:22:00' FROM `user` u WHERE u.employee_no='M2026001';
INSERT INTO login_log (user_id, username, ip, user_agent, login_status, fail_reason, login_time)
SELECT u.id, u.username, '10.10.1.23', 'Mozilla/5.0', 'SUCCESS', NULL, '2026-03-21 09:24:00' FROM `user` u WHERE u.employee_no='M2026001';

-- =====================
-- 4) 设备统计字段与逻辑一致性回填
-- =====================
UPDATE device d SET
  d.total_repair_requests = (SELECT COUNT(*) FROM repair_order ro WHERE ro.device_id = d.id),
  d.total_repair_order_count = (SELECT COUNT(*) FROM repair_order ro WHERE ro.device_id = d.id),
  d.total_repair_count = (SELECT COUNT(*) FROM repair_record rr WHERE rr.device_id = d.id),
  d.last_fault_time = (SELECT MAX(ro.report_time) FROM repair_order ro WHERE ro.device_id = d.id),
  d.update_time = NOW();

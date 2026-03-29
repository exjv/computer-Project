-- Step26: 精简演示数据（数据库重构后）
USE network_repair;

-- 角色
INSERT INTO role (role_code, role_name, role_status, status, role_desc, create_time, update_time)
SELECT 'ADMIN', '系统管理员', 'ENABLED', 1, '系统全局管理', NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code='ADMIN');
INSERT INTO role (role_code, role_name, role_status, status, role_desc, create_time, update_time)
SELECT 'MAINTAINER', '维修人员', 'ENABLED', 1, '接单维修', NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code='MAINTAINER');
INSERT INTO role (role_code, role_name, role_status, status, role_desc, create_time, update_time)
SELECT 'USER', '报修用户', 'ENABLED', 1, '提交报修', NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM role WHERE role_code='USER');

-- 用户（工号唯一）
INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
SELECT 'A2026001','admin','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','系统管理员','13800000001','admin@campus.edu','网络信息中心','admin',1,0,NOW(),NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE employee_no='A2026001');
INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
SELECT 'M2026001','maint1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','王工','13800000004','maint1@campus.edu','网络运维组','maintainer',1,0,NOW(),NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE employee_no='M2026001');
INSERT INTO `user` (employee_no, username, password, real_name, phone, email, department, role, status, third_party_bound_flag, create_time, update_time)
SELECT 'U2026001','user1','$2a$12$BbAoUM7.Zv67b60.4iJ35.budKRVsjdgu1VHLb0sHiWAseMRUYFO.','张三','13800000002','user1@campus.edu','教务处','user',1,0,NOW(),NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE employee_no='U2026001');

-- 用户角色关系
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='A2026001' AND r.role_code='ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='M2026001' AND r.role_code='MAINTAINER'
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);
INSERT INTO user_role (user_id, role_id, create_time)
SELECT u.id, r.id, NOW() FROM `user` u JOIN role r ON u.employee_no='U2026001' AND r.role_code='USER'
WHERE NOT EXISTS (SELECT 1 FROM user_role ur WHERE ur.user_id=u.id AND ur.role_id=r.id);

-- 设备类型 + 设备
INSERT INTO device_type (type_code, type_name, status, sort_no, create_time, update_time)
SELECT 'SWITCH', '交换机', 1, 10, NOW(), NOW() FROM dual
WHERE NOT EXISTS (SELECT 1 FROM device_type WHERE type_code='SWITCH');

INSERT INTO device (device_code, device_name, device_type, device_type_name, brand, model, serial_number, campus, building_location,
                    purchase_date, enable_date, warranty_expiry_date, owner_name, management_dept, status,
                    total_repair_requests, total_repair_order_count, total_repair_count, create_time, update_time)
SELECT 'DEV-CORE-001', '核心交换机A', 'SWITCH', '交换机', 'H3C', 'S5560', 'SN-CORE-001', '主校区', '信息楼机房',
       '2021-03-12', '2021-03-15', '2027-03-12', '王工', '网络运维组', '正常', 0, 0, 0, NOW(), NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM device WHERE device_code='DEV-CORE-001');

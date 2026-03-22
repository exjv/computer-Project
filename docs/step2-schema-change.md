# 第2步：数据库设计与基础数据模型重构说明

## 1. 本步修改文件

- `sql/init.sql`
- `sql/migrations/V2__refactor_core_schema.sql`
- `backend/src/main/java/com/jou/networkrepair/module/user/entity/SysUser.java`
- `backend/src/main/java/com/jou/networkrepair/module/device/entity/NetworkDevice.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/entity/RepairOrder.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/entity/RepairOrderFlow.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/entity/RepairRecord.java`
- `backend/src/main/java/com/jou/networkrepair/module/notice/entity/Notice.java`
- `backend/src/main/java/com/jou/networkrepair/module/log/entity/LoginLog.java`
- `backend/src/main/java/com/jou/networkrepair/module/log/entity/OperationLog.java`
- `backend/src/main/java/com/jou/networkrepair/module/auth/controller/AuthController.java`
- `backend/src/main/java/com/jou/networkrepair/common/config/OperationLogAspect.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderCreateDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairRecordDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairRecordServiceImpl.java`
- 新增 `module/system/entity|mapper|dto|vo` 下模型文件

## 2. 新增表与重构表

### 新增表
- `role`
- `user_role`
- `device_type`
- `repair_feedback`
- `business_log`
- `file_attachment`
- `third_party_bind`
- `dictionary`

### 重构表
- `user`（由原 `sys_user` 结构升级并明确唯一约束）
- `device`（由原 `network_device` 升级）
- `repair_order`
- `repair_order_flow`
- `repair_record`
- `announcement`（对齐公告表命名）
- `operation_log`
- `login_log`

## 3. 字段变化标注

### repair_order
- **新增字段**：`reporter_employee_no`、`reporter_name`、`contact_phone`、`reporter_department`、`report_location`、`device_code`、`device_name`、`device_type`、`fault_type`、`affect_wide_area_network`、`audit_by_employee_no`、`audit_by_name`、`assign_by`、`assign_by_employee_no`、`assign_by_name`、`assign_maintainer_employee_no`、`assign_maintainer_name`、`need_purchase_parts`、`parts_description`、`apply_delay`、`original_expected_finish_time`、`delayed_expected_finish_time`、`user_confirm_result`、`remark`。
- **重构字段**：`order_no`、`priority`、`status`、`confirm_time`、`expected_finish_time`。

### device
- **新增字段**：`device_type_name`、`brand`、`model`、`serial_no`、`campus`、`building`、`machine_room`、`office`、`enable_date`、`warranty_expire_date`、`owner_user_id`、`owner_employee_no`、`owner_name`、`management_dept`、`last_fault_time`、`total_repair_order_count`、`total_repair_count`、`fault_reason_stats`。
- **重构字段**：`device_code`、`device_name`、`device_type`、`status`、`purchase_date`。

## 4. 约束落实

- 工号唯一：`user.employee_no` 唯一约束。
- 工单编号唯一：`repair_order.order_no` 唯一约束。
- 设备编号唯一：`device.device_code` 唯一约束。
- 外键关系：工单、流程、维修记录、反馈、日志、附件、三方绑定均补充到 `user/device/repair_order`。
- 状态字段规范化：主要业务表均使用明确状态字段（如 `role_status`、`status`、`bind_status`）。
- 时间字段完整：核心业务表统一包含 `create_time/update_time` 或业务时间字段。

## 5. 初始化数据方案（精简）

- 初始化最小角色数据：管理员/维修人员/报修用户。
- 初始化最小用户、用户角色关系。
- 初始化设备类型字典 + 1 条设备样例。
- 初始化 1 条工单 + 1 条公告 + 字典优先级样例。


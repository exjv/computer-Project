# 第11步：管理员侧工单动作与流转校验

## 本步完成

1. 新增管理员动作接口：
   - `PUT /api/repair-orders/{id}/audit`（审核/驳回）
   - `PUT /api/repair-orders/{id}/assign`（分配）
   - `PUT /api/repair-orders/{id}/reassign`（改派）
   - `PUT /api/repair-orders/{id}/delay-approve`（延期审批）
   - `PUT /api/repair-orders/{id}/close`（关闭/强制关闭）
2. 状态机与业务校验增强：
   - 未审核不能分配（分配仅允许“待分配”）
   - 未分配不能接单（接单仅允许“待接单”）
   - 未接单不能开始维修（开始维修仅允许“维修人员已接单”）
   - 维修未完成不能进入待验收（提交完工仅允许“维修中”）
   - 用户未确认前不能直接关闭（正常关闭仅允许“已完成”）
   - 强制关闭必须提供关闭原因
   - 已完成/已关闭工单禁止编辑核心字段
3. 关键操作日志：
   - 审核、分配、改派、延期审批、关闭/强制关闭均写入 flow 与 business_log
4. 前端管理员页面增强：
   - 工单列表新增改派、延期审批、关闭/强制关闭操作入口与弹窗

## 涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/repair/controller/RepairOrderController.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/RepairOrderService.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderAuditDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderReassignDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderCloseDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderDelayApproveDTO.java`
- `frontend/src/views/repair/RepairOrderView.vue`
- `docs/step11-admin-repair-actions.md`

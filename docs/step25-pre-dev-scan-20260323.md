# 第25步：正式改造前扫描与实施清单（2026-03-23）

> 目标：在“正式改代码”前，基于当前仓库真实代码与 SQL 结构输出可执行清单，作为后续第 2 步（数据库重构）和后续业务改造的基线。

---

## 1. 当前项目存在的问题清单

### 1.1 当前已有（可用能力）
- 后端主线模块具备认证、用户、设备、工单、公告、日志的基础接口。
- 后端同时存在 `module/v2` 一套自动 CRUD 化模块，覆盖用户/设备/工单/公告/RBAC/日志/附件/字典。
- 前端已有门户、登录、首页、用户、设备、工单、维修记录、日志、个人中心页面。

### 1.2 需要新增（当前缺口）
1. **真实附件上传链路**：当前维修附件主要还是 URL 文本输入/占位 DTO，缺少真正存储与下载闭环。
2. **SLA 与催办闭环**：数据库与接口缺少独立 SLA 规则/事件实体和监控页。
3. **维修调度推荐可解释性**：虽有 `RepairDispatchAlgorithm`，但缺少前端可视化展示推荐因子、冲突提示和人工覆盖记录。
4. **报表中心**：当前导出能力分散在单模块，缺少统一筛选、任务追踪、下载管理页面。

### 1.3 需要重构（真实问题）
1. **前端编译级错误**
   - `LoginView.vue` 重复声明 `step` 和 `selectRole`，且状态流转值不一致（1/2 两套逻辑混用）。
   - `HomeView.vue` 出现大量重复函数定义（`editNotice/saveNotice/switchStatus/deleteNotice` 多次重复），并引用了未定义变量（`workbench/todoLabelMap/statLabelMap/filteredEntries/router`）。
   - `stores/user.js` 中权限判断函数重复粘贴 4 轮。
2. **后端编译级/路由冲突风险**
   - `module/device/controller/DeviceController.java` 存在多个同签名 `@GetMapping("/{id}/detail")` 方法重复定义。
3. **双轨架构问题（module + module/v2）**
   - 同业务域存在两套实体、Mapper、Controller，接口命名和字段口径不统一。
   - 前端实际主要走 `/api/**` 主线，但 v2 接口仍可访问，增加权限与维护风险。
4. **SQL 脚本内部不一致**
   - `sql/init.sql` 中 `device` 表出现重复列定义（如 `campus` 重复）。
   - `init.sql` 的初始化数据插入引用 `sys_user/network_device/notice`，与当前建表 `user/device/announcement` 命名不一致，直接执行会失败。
5. **未联动页面与“伪功能入口”**
   - 页面文件存在但未注册路由：`NoticeView`、`RepairApplyView`、`MyRepairRecordsView`、`MaintainerPendingOrdersView`、`RepairProgressView`。
   - 登录页第三方登录仍为“预留/模拟触发”。

---

## 2. 需要新增/修改的数据表清单

> 说明：以下按“当前已有 / 需要新增 / 需要重构”分类，优先以 `sql/migration/20260322_step2_enterprise_schema.sql` 作为目标骨架。

### 2.1 当前已有
- 用户与权限域：`user`、`role`、`user_role`、`permission`、`role_permission`、`third_party_bind`、`dictionary`。
- 设备域：`device_type`、`device`。
- 工单域：`repair_order`、`repair_order_flow`、`repair_record`、`repair_feedback`。
- 公告与日志域：`announcement`、`operation_log`、`business_log`、`login_log`。
- 附件：`file_attachment`。

### 2.2 需要新增
1. `repair_sla_rule`：SLA 规则（紧急程度、设备类型、时限、升级策略）。
2. `repair_sla_event`：SLA 事件（预警、超时、升级、恢复）。
3. `device_lifecycle_log`：设备生命周期事件（投产、巡检、停机、报废、复用）。
4. `maintainer_skill`：维修员技能标签（网络、无线、安全、机房等）。
5. `maintainer_shift`：班次与值班信息（用于推荐调度）。

### 2.3 需要重构
1. **统一初始化脚本口径**：修复 `init.sql` 的表名/字段名不一致与重复字段。
2. **补齐核心约束与索引**
   - 唯一：`user.job_no`、`repair_order.order_no`、`device.device_no`。
   - 组合索引：
     - `repair_order(current_status, assignee_user_id, submit_time)`
     - `repair_order(device_id, current_status, update_time)`
     - `repair_order_flow(repair_order_id, operation_time)`
     - `operation_log(module, operation_time)`
     - `business_log(biz_type, biz_id, operation_time)`
3. **状态规范化**：统一状态编码（避免中文状态与英文状态混用造成接口分支爆炸）。

---

## 3. 需要新增/修改的后端接口清单

### 3.1 当前已有
- 主线：`/api/auth/*`、`/api/users/*`、`/api/devices/*`、`/api/repair-orders/*`、`/api/repair-records/*`、`/api/notices/*`、`/api/logs/*`、`/api/roles/*`。
- v2：`/api/v2/**` 覆盖用户/设备/工单/公告/RBAC/日志/附件/字典 CRUD。

### 3.2 需要新增
1. `GET /api/workbench/overview`：按角色聚合待办（管理员/维修员/报修用户）。
2. `GET /api/repair-orders/{id}/sla`：查询工单 SLA 目标、剩余时长、风险等级。
3. `POST /api/repair-orders/{id}/urge`：用户催办，写入 `repair_sla_event`。
4. `POST /api/files/upload` + `GET /api/files/{id}/download`：真实附件上传下载。
5. `GET /api/maintainers/recommend`：基于技能、负载、班次输出推荐列表及评分。
6. `GET /api/devices/{id}/lifecycle`、`POST /api/devices/{id}/lifecycle`：设备生命周期事件查询/记录。

### 3.3 需要重构
1. 收敛双轨接口：确定“主线优先 + v2 下线”或“v2 全量接管”单轨方案。
2. 清理重复映射和重复方法（先修复编译后再扩展业务）。
3. 全量接口补齐：
   - 权限控制（`@PreAuthorize` + 细粒度权限码）；
   - 参数校验（`@Validated` + DTO 约束）；
   - 异常处理（统一 `BusinessException` / `GlobalExceptionHandler`）；
   - 审计日志与业务日志落库。
4. 工单状态机严格化：所有状态跳转必须校验“当前状态 + 操作角色 + 前置条件”。

---

## 4. 需要新增/修改的前端页面清单

### 4.1 当前已有（路由已接入）
- `/portal`、`/login`、`/`、`/users`、`/devices`、`/devices/:id`、`/repair-orders`、`/repair-orders/:id`、`/repair-orders/:id/progress`、`/repair-records`、`/logs`、`/profile`。

### 4.2 当前已有但未联动路由
- `NoticeView.vue`
- `RepairApplyView.vue`
- `MyRepairRecordsView.vue`
- `MaintainerPendingOrdersView.vue`
- `RepairProgressView.vue`

### 4.3 需要新增
1. 角色工作台页（管理员/维修员/报修用户差异化看板）。
2. SLA 监控页（超时预警、催办、升级）。
3. 调度推荐页（推荐名单、评分、手工改派理由）。
4. 生命周期页（设备事件时间线、工单关联、状态演进）。
5. 报表中心页（统计筛选 + 导出记录管理）。

### 4.4 需要重构
1. 修复 `LoginView.vue` 和 `HomeView.vue` 的重复定义及未定义变量问题。
2. 统一工单相关重复页面（`RepairOrderProgressView` vs `RepairProgressView`）。
3. 让菜单、路由、权限码三者一一对应，消除假按钮与空页面。
4. 页面动作全部真实联动后端接口，不再使用“预留点击提示”替代真实流程。

---

## 5. 整体改造实施计划

### 阶段 A（P0）：可编译、可运行、单轨化
1. 修复前端编译错误与后端重复映射。
2. 统一主线/ v2 技术路线，冻结另一套，避免双写。
3. 清理并对齐 SQL 初始化脚本，确保本地一键建库可执行。

### 阶段 B（P1）：数据库重构落地
1. 以企业级 schema 为主，补齐 15 张核心业务表字段口径。
2. 增加 SLA、生命周期、班次/技能等表。
3. 迁移脚本幂等化，保留 legacy 迁移通道。

### 阶段 C（P2）：后端流程与权限增强
1. 完成工单状态机合法性校验。
2. 全量接口补齐权限、校验、异常、日志。
3. 上线附件真实上传、调度推荐、SLA 接口。

### 阶段 D（P3）：前端联动与角色协作
1. 全页面路由接入与权限控制。
2. 修复假按钮、伪功能，形成真实闭环。
3. 上线工作台、SLA 监控、生命周期、报表中心。

### 阶段 E（P4）：验收与演示
1. 补齐演示数据、测试脚本、验收清单。
2. 论文/答辩场景化演示流程打通（用户报修→审核→分配→维修→验收→统计）。

---

## 附录 A：当前数据库表清单（按脚本来源）

### `sql/init.sql`
`user`、`role`、`user_role`、`permission`、`role_permission`、`device_type`、`device`、`repair_order`、`repair_order_flow`、`repair_record`、`repair_feedback`、`announcement`、`operation_log`、`business_log`、`login_log`。

### `sql/migration/20260322_step2_enterprise_schema.sql`
`dictionary`、`role`、`user`、`user_role`、`third_party_bind`、`device_type`、`device`、`repair_order`、`repair_order_flow`、`repair_record`、`repair_feedback`、`announcement`、`operation_log`、`business_log`、`file_attachment`。

### `sql/migrations/V2__refactor_core_schema.sql`
`role`、`user_role`、`permission`、`role_permission`、`user`、`device_type`、`device`、`repair_feedback`、`announcement`、`business_log`、`file_attachment`、`third_party_bind`、`dictionary`。

---

## 附录 B：当前后端 Controller / Service / Mapper 清单

### Controller
- 主线：`module/auth`、`module/device`、`module/log`、`module/notice`、`module/repair`、`module/system`、`module/user`
- v2：`module/v2/auth2`、`device2`、`dictionary`、`file2`、`log2`、`notice2`、`rbac`、`repair2`、`user2`

### Service（接口）
- 主线：`module/auth`、`module/repair`
- 公共：`common/security`
- v2：`module/v2/*/service`

### Mapper
- 主线：`module/device/log/notice/repair/system/user`
- v2：`module/v2/*/mapper`

---

## 附录 C：当前前端页面与路由清单

### 路由已注册
`/portal`、`/login`、`/`、`/users`、`/devices`、`/devices/:id`、`/repair-orders`、`/repair-orders/:id`、`/repair-orders/:id/progress`、`/repair-records`、`/logs`、`/profile`

### 页面存在但未注册路由
`NoticeView.vue`、`RepairApplyView.vue`、`MyRepairRecordsView.vue`、`MaintainerPendingOrdersView.vue`、`RepairProgressView.vue`

---

## 附录 D：改造优先级排序
1. **P0**：修复编译错误 + 清理重复映射 + SQL 初始化可执行。
2. **P1**：数据库结构统一（15 核心表 + 约束索引 + 迁移）。
3. **P2**：状态机与 RBAC 完整化（接口全链路合法性校验）。
4. **P3**：前端角色工作台/SLA/生命周期/报表联动。
5. **P4**：测试、演示数据、答辩场景验收。

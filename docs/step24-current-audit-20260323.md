# 第24步：项目现状审计与改造清单（2026-03-23）

> 范围：仅基于当前仓库实际代码与 SQL 脚本审计，不做大改代码；本步仅新增审计文档与 README 链接。

## 1. 当前前后端代码结构扫描

### 1.1 后端结构（Spring Boot）
- `common`：统一响应、异常处理、JWT 过滤器、安全配置、权限服务。
- `module`（当前业务主线）：`auth`、`user`、`device`、`repair`、`notice`、`log`、`system`。
- `module/v2`（代码生成式 V2 目录）：`user2`、`device2`、`repair2`、`notice2`、`rbac`、`log2`、`file2`、`auth2`、`dictionary`。
- 结论：**存在双轨并行（module + module/v2）**，且 API 前缀、数据模型、权限策略不一致，属于“半重构态”。

### 1.2 前端结构（Vue3）
- 路由主入口集中在 `frontend/src/router/index.js`。
- 实际菜单由 `MENU_CONFIG` + `store.permissions` 控制。
- 页面目录中存在多页未注册路由（例如 `NoticeView`、`RepairApplyView`、`MyRepairRecordsView`、`MaintainerPendingOrdersView`、`RepairProgressView`）。
- 结论：**页面资产多于可访问路由，存在未联动页面与功能重复页面。**

---

## 2. 当前“已有模块 / 已有表 / 已有接口 / 已有页面”

## 2.1 已有模块（按“当前已有”列出）

### 当前已有
1. 认证与门户：验证码登录、JWT、角色入口门户、OAuth 预留接口。
2. 用户管理：分页、新增、编辑、删除、重置密码、启停、角色分配、批量导入。
3. 设备管理：设备分页、详情、详情画像、状态修改、统计。
4. 报修工单：提交、审核、分配、改派、接单、维修动作流转、自动派单、导出、反馈分析。
5. 维修记录：分页+CRUD+设备维度统计。
6. 公告管理：门户公告、后台公告增删改查、状态切换。
7. 日志：操作审计与业务日志分页查询。
8. RBAC：具备角色/权限码常量、前端菜单权限过滤、后端方法级权限注解（主线模块）。

### 需要新增
- “分角色工作台”深度能力（管理员/维修员/报修用户待办看板拆分）。
- SLA 配置与超时预警闭环。
- 附件“真实上传存储”（当前大量是 URL/占位方式）。

### 需要重构
- `module` 与 `module/v2` 二选一统一。
- 权限模型统一到“接口 + 按钮 + 数据范围”一套口径。

## 2.2 当前数据库表清单（按现有 SQL 资产汇总）

### 当前已有（`sql/init.sql` + 迁移脚本出现）
- 账号与权限：`user`、`role`、`user_role`、`permission`、`role_permission`。
- 设备域：`device_type`、`device`（迁移中出现 `network_device` 对应映射关系）。
- 工单域：`repair_order`、`repair_order_flow`、`repair_record`、`repair_feedback`。
- 内容与日志：`announcement`/`notice`、`operation_log`、`business_log`、`login_log`。
- 辅助域：`file_attachment`、`third_party_bind`、`dictionary`。

### 需要新增
1. `repair_attachment`（统一工单/维修附件元数据，替代散落字段与 URL 文本）。
2. `repair_sla_rule` + `repair_sla_event`（SLA 配置、超时、升级）。
3. `maintainer_skill` + `maintainer_shift`（维修员技能/班次）。
4. `device_lifecycle_log`（设备状态迁移与关键事件）。

### 需要重构
1. 表命名统一（`user/device/announcement` vs `sys_user/network_device/notice` 混用）。
2. `init.sql` 存在结构不一致风险（示例：`device` 列重复、插入语句引用 `sys_user/network_device`）。
3. 主线与 v2 对应实体字段口径统一（避免双模型写入同库导致数据漂移）。

## 2.3 当前后端接口清单（按 Controller）

### 当前已有（主线）
- 认证：`/api/auth/*`，第三方回调 `/api/auth/oauth/*`。
- 门户：`/api/portal/home`。
- 用户：`/api/users/*`（含 roles、batch、import、reset-password、status、roles）。
- 设备：`/api/devices/*`（page、detail、profile、status、statistics）。
- 工单：`/api/repair-orders/*`（page/my/detail/create/update/delete/action/audit/assign/flows/records/statistics/analytics/export/auto-dispatch 等）。
- 维修记录：`/api/repair-records/*`。
- 公告：`/api/notices/*`。
- 日志：`/api/logs/*`。
- 角色：`/api/roles/*`。

### 当前已有（v2 自动 CRUD）
- `/api/v2/**` 下多组标准 CRUD 接口（user/device/repair/notice/rbac/log/file/auth/dictionary）。

### 需要新增
1. `GET /api/workbench/{role}`：分角色聚合待办。
2. `POST /api/repair-orders/{id}/attachments/upload`：真实文件上传。
3. `GET /api/repair-orders/{id}/sla`、`POST /api/repair-orders/{id}/urge`：SLA/催办。
4. `GET /api/maintainers/recommendations`：技能+负载+班次推荐。
5. `GET /api/devices/{id}/lifecycle`：设备生命周期。

### 需要重构
1. 合并/下线 `/api/v2/**` 或主线 `/api/**` 其中一套。
2. 统一接口返回字段规范（当前同类业务在不同模块返回结构差异较大）。
3. 统一权限注解策略（`hasRole`、`hasPermission`、无注解混用）。

## 2.4 当前前端页面与路由清单

### 当前已有（路由已注册可访问）
- `/portal` 门户页
- `/login` 登录页
- `/` 首页
- `/users` 用户管理
- `/devices` 设备管理
- `/devices/:id` 设备详情
- `/repair-orders` 工单页
- `/repair-orders/:id` 工单详情
- `/repair-orders/:id/progress` 工单进度
- `/repair-records` 维修记录
- `/logs` 日志管理
- `/profile` 个人中心

### 当前已有（页面文件存在但未联动路由）
- `views/notice/NoticeView.vue`
- `views/repair/RepairApplyView.vue`
- `views/repair/MyRepairRecordsView.vue`
- `views/repair/MaintainerPendingOrdersView.vue`
- `views/repair/RepairProgressView.vue`

### 需要新增
1. 统计分析中心页（独立于首页大杂烩）。
2. 权限管理页（角色-权限点可视化）。
3. 设备生命周期页。
4. SLA 监控与催办页。

### 需要重构
1. 合并重复工单相关页（`RepairOrderProgressView` 与 `RepairProgressView`）。
2. 公告管理入口统一（`HomeView` 内公告管理 vs `NoticeView`）。
3. 路由与菜单统一维护（避免“有页面无入口/有入口无能力”）。

---

## 3. 问题清单（基于真实代码，不泛泛而谈）

## 3.1 当前已有的问题
1. **前端存在明显编译错误**：`LoginView.vue` 重复定义 `step/selectRole`，导致 `vite build` 失败。
2. `HomeView.vue` 出现同名函数重复定义（`editNotice/saveNotice/switchStatus/deleteNotice` 多次定义），可维护性差且有潜在编译风险。
3. `user` store 出现多次重复权限函数定义（`hasRole/hasAnyRole/hasPerm/hasAnyPerm` 重复粘贴）。
4. 后端 `DeviceController` 出现多个同路径同方法签名 `@GetMapping("/{id}/detail")` 重复实现，代码已严重污染。
5. 存在主线模块与 `module/v2` 双套接口并存，容易造成前后端调用混乱。
6. 前端存在多页面“存在但无路由入口”，实际功能不可达。
7. OAuth 前端按钮仍是“预留/模拟流程”，非真实第三方登录。

## 3.2 缺失模块
1. 缺少 SLA 规则管理、超时检测、升级闭环。
2. 缺少设备生命周期事件流与审批视图。
3. 缺少统一报表中心（目前仅分散导出接口）。
4. 缺少可视化权限管理页与数据范围策略配置。

## 3.3 伪功能 / 假按钮
1. 登录页、门户页的“微信/QQ 登录（预留）”为模拟调用。
2. 维修员“上传现场照片”使用 URL 输入占位，不是真实上传链路。
3. `CrudPage.vue` 明确写明“该通用组件已停用”，但组件仍在代码树中。

## 3.4 未联动页面
1. `NoticeView` 存在但路由与菜单均未挂接。
2. `RepairApplyView` 存在但实际提交入口已被 `RepairOrderView` 承担。
3. `MyRepairRecordsView` 与 `MaintainerPendingOrdersView` 无路由入口。
4. `RepairProgressView` 与已上线的 `RepairOrderProgressView` 功能重叠。

## 3.5 无权限控制接口（重点）
1. 多数 `/api/v2/**` Controller 无 `@PreAuthorize`，仅依赖“已登录”访问控制，不满足细粒度 RBAC。
2. 主线也存在个别接口权限粒度不一致问题（同域接口使用不同策略）。

## 3.6 无状态校验接口（流程合法性不足）
1. 工单动作接口虽有动作入参，但前端仍存在“快速动作”并发触发风险，缺乏统一幂等键/版本号。
2. 设备状态变更未统一校验“是否存在进行中工单/维修记录”的全局规则（部分接口有校验，部分无）。
3. 维修附件上传流程（占位模式）缺失状态机约束（谁在什么状态可传什么附件）。

---

## 4. 改造清单输出（分“当前已有 / 需要新增 / 需要重构”）

## 4.1 数据表改造清单

### 当前已有
- 已有工单主表、流转、维修记录、反馈、日志、公告、角色权限核心表。

### 需要新增
1. `repair_attachment`
2. `repair_sla_rule`
3. `repair_sla_event`
4. `maintainer_skill`
5. `maintainer_shift`
6. `device_lifecycle_log`

### 需要重构
1. 统一用户/设备/公告主表命名与迁移策略。
2. 拆分 `repair_order` 里的附件文本字段为结构化附件表。
3. 补充索引：`repair_order(status,assign_maintainer_id,report_time)`、`repair_order_flow(repair_order_id,create_time)`、`operation_log(operation_time,module)`。

## 4.2 后端接口改造清单

### 当前已有
- 主线接口覆盖毕业设计基本流程，含自动派单、统计、导出。

### 需要新增
1. 工作台聚合接口（管理员/维修员/用户）。
2. SLA 查询、催办、升级接口。
3. 附件上传/下载接口（MinIO/本地存储抽象）。
4. 设备生命周期查询与事件提交接口。
5. RBAC 权限点管理接口（如后续保留动态权限）。

### 需要重构
1. 合并 `module` 与 `module/v2`。
2. 全量接口补齐权限注解与数据范围校验。
3. 工单动作接口加入版本号/幂等校验。

## 4.3 前端页面改造清单

### 当前已有
- 统一后台框架+核心页面可用（除编译错误影响）。

### 需要新增
1. 统计中心页。
2. SLA 管控页。
3. 设备生命周期页。
4. 权限管理页（如采用动态 RBAC）。

### 需要重构
1. 修复 `LoginView`、`HomeView`、`user store` 重复定义问题。
2. 路由与菜单补齐公告、角色分工作台。
3. 清理重复页与无效组件，统一工单入口。

---

## 5. 整体改造实施计划（可落地顺序）

### P0（立即）——可运行性与单轨化
1. 修复前端编译错误（Login/Home/store）。
2. 清理后端重复方法（DeviceController 重复 detail）。
3. 明确保留“主线 module”或“v2”，冻结另一套为废弃态。

### P1（核心闭环）——权限与流程稳定
1. 统一 RBAC 注解策略，补齐无注解接口。
2. 工单动作接口补齐状态前置校验 + 幂等校验。
3. 前端按钮按后端能力矩阵重排，去除伪按钮。

### P2（能力增强）——SLA + 附件 + 生命周期
1. 上线附件真实上传。
2. 上线 SLA 规则与超时提醒。
3. 上线设备生命周期事件流。

### P3（管理与决策）——统计与报表
1. 独立统计中心页（多维筛选）。
2. 导出任务中心与下载记录。

### P4（体验收敛）——论文演示与验收
1. 统一术语、状态字典、文档。
2. 增补演示脚本与验收 checklist。

---

## 6. 当前后端 Controller / Service / Mapper 清单（仓库扫描）

> 以文件维度列出，便于后续模块分工。

### Controller
- `module/auth`: `AuthController`, `ThirdPartyAuthController`
- `module/device`: `DeviceController`
- `module/log`: `LogController`
- `module/notice`: `NoticeController`, `PortalController`
- `module/repair`: `RepairOrderController`, `RepairRecordController`
- `module/system`: `RoleController`
- `module/user`: `UserController`
- `module/v2/*`: `ThirdPartyBindController`, `DeviceController`, `DeviceTypeController`, `DictionaryController`, `FileAttachmentController`, `BusinessLogController`, `OperationLogV2Controller`, `AnnouncementController`, `RoleController`, `UserRoleController`, `RepairFeedbackController`, `RepairOrderController`, `RepairOrderFlowController`, `RepairRecordController`, `UserController`

### Service（接口层）
- `module/auth`: `CaptchaService`, `ThirdPartyAuthService`
- `module/repair`: `RepairOrderService`, `RepairRecordService`
- `common/security`: `PermissionService`, `RbacPermissionService`
- `module/v2/*`: `ThirdPartyBindService`, `DeviceService`, `DeviceTypeService`, `DictionaryService`, `FileAttachmentService`, `BusinessLogService`, `OperationLogV2Service`, `AnnouncementService`, `RoleService`, `UserRoleService`, `RepairFeedbackService`, `RepairOrderFlowService`, `RepairOrderService`, `RepairRecordService`, `UserService`

### Mapper
- 主线：`DeviceMapper`, `LoginLogMapper`, `OperationLogMapper`, `NoticeMapper`, `RepairOrderMapper`, `RepairOrderFlowMapper`, `RepairRecordMapper`, `UserMapper`, 以及 `module/system/mapper/*`
- V2：`module/v2/*/mapper/*` 对应各子域 Mapper

---

## 7. 改造优先级排序（最终建议）

1. **最高优先级（P0）**：可编译、可启动、单轨 API（否则后续开发风险极高）。
2. **高优先级（P1）**：权限一致性 + 工单状态机一致性（毕业设计答辩核心）。
3. **中优先级（P2）**：附件真实化 + SLA + 生命周期（业务完整度关键加分项）。
4. **中低优先级（P3）**：统计中心与报表中心（管理可视化）。
5. **低优先级（P4）**：界面精修、文档包装、演示脚本。

---

## 8. 本步最少量准备性修改说明

- 新增本审计文档：`docs/step24-current-audit-20260323.md`。
- 不改动业务代码逻辑，不新增业务接口与页面。
- 下一步开始按本清单进入模块开发。

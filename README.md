# 校园网络设备管理与故障报修系统

## 项目简介
本项目是《基于SpringBoot的校园网络设备管理与故障报修系统的设计与实现》本科毕业设计实现，覆盖设备管理、故障报修、工单分配、维修记录、公告管理、日志追踪、权限控制等核心流程。

## 技术架构
- 后端：Spring Boot 2.7 + Spring Security + JWT + MyBatis-Plus + MySQL
- 前端：Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios
- 架构：前后端分离 RESTful API

## 功能模块
- 登录认证与 RBAC 权限控制（admin/user/maintainer）
- 用户管理（查询/新增/编辑/删除/重置密码/启停）
- 设备管理（查询/详情/新增/编辑/删除）
- 报修工单（提交、分配、状态流转、按角色查看）
- 维修记录（查询/新增/编辑/删除）
- 公告管理（查询/详情/新增/编辑/删除）
- 日志管理（登录日志 + 操作日志）
- 首页统计（工单统计 + 设备统计）
- 智能派单算法（基于优先级 + 维修人员负载均衡自动分配）

## 运行步骤
1. 创建数据库并导入：`sql/init.sql`
   - 若执行第 2 步数据库重构，请追加执行：`sql/migration/20260323_step26_refactor_models.sql`
   - 若执行第 4 步登录重构补丁：`sql/migration/20260323_step27_auth_login_refactor.sql`
   - 若执行第 17 步维修记录强化补丁：`sql/migration/20260329_step28_repair_record_enhance.sql`
   - 若使用 Flyway 版本化脚本初始化，可改为执行：`sql/migrations/V2__refactor_core_schema.sql`
   - 精简演示数据：`sql/demo_seed_step26.sql`
2. 修改 `backend/src/main/resources/application.yml` 数据库配置
3. 启动后端：
   ```bash
   cd backend
   mvn spring-boot:run
   ```
4. 启动前端：
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. 访问：`http://localhost:5173/portal`

## 默认账号密码
- 管理员：工号 `A2026001`（或用户名 `admin`）/ 123456
- 普通用户：工号 `U2026001`、`U2026002`（或用户名 `user1`、`user2`）/ 123456
- 维修人员：工号 `M2026001`、`M2026002`（或用户名 `maint1`、`maint2`）/ 123456

> 说明：数据库初始化脚本中密码已采用 BCrypt 加密存储，文档中提供的是原始测试密码，便于演示登录。

## 外键逻辑说明（代码维护）
系统不强制数据库物理外键，逻辑关联由业务代码保证：
- `repair_order.device_id -> network_device.id`
- `repair_order.reporter_id -> sys_user.id`
- `repair_order.assign_maintainer_id -> sys_user.id`
- `repair_record.repair_order_id -> repair_order.id`
- `repair_record.device_id -> network_device.id`
- `repair_record.maintainer_id -> sys_user.id`
- `notice.publisher_id -> sys_user.id`
- `operation_log.user_id/login_log.user_id -> sys_user.id`

## 文档
- 接口说明：`docs/api.md`
- 论文辅助：`docs/thesis-support.md`
- 现状审计与改造清单（2026-03-23）：`docs/step24-current-audit-20260323.md`
- 正式改造前扫描与实施清单（2026-03-23）：`docs/step25-pre-dev-scan-20260323.md`

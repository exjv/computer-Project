# 第3步：RBAC 权限模型与工号机制重构

## 本步完成内容

1. JWT 登录后返回用户角色、权限码、工号信息。
2. 新增权限码体系（接口级 + 页面级统一）。
3. 新增 PermissionService，支持：
   - 从 `user_role -> role_permission -> permission` 读取权限；
   - 数据为空时回退到内置角色权限映射，保证兼容。
4. 用户-角色关联逻辑：用户新增/编辑时同步写入 `user_role`。
5. 接口级权限校验：关键控制器改为 `@PreAuthorize("@permissionService.hasPermission(...)")`。
6. 工号机制：
   - 工号唯一校验接口；
   - 按工号检索用户接口；
   - 登录支持工号账号登录（已兼容）。
7. 前端权限路由数据支持：
   - `userInfo` 返回 `roles`、`permissions`、`routePermissions`；
   - 前端 Store 持久化权限；
   - 路由 meta 权限校验；
   - 侧边菜单按权限显示。

## 本步新增/修改接口

- `GET /api/auth/employee-no/check`
- `GET /api/users/by-employee-no`
- `GET /api/users/employee-no/check`
- `GET /api/roles/page`
- `POST /api/roles`
- `PUT /api/roles/{id}`
- `GET /api/auth/userInfo`（增强返回权限数据）
- `POST /api/auth/login`（增强返回权限数据）

## 本步新增页面

- 无新增页面（本步完成页面级权限控制所需数据结构和路由守卫）

## 本步剩余未完成

- 角色管理前端页（当前仅后端接口就绪）。
- 权限点可视化分配页面。
- 维修照片上传、延期/采购申请的独立业务接口与前端表单。

## 本步验证方式

1. 管理员登录后 `userInfo.permissions` 包含 `user:manage` 等权限码。
2. 无权限访问带 `meta.permission` 的路由时自动跳转首页。
3. 调用用户新增/编辑后检查 `user_role` 是否写入。
4. 用重复工号调用校验接口，返回 `available=false`。


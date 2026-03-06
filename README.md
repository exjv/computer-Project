# 校园网络设备管理与故障报修系统

## 1. 项目简介
本项目是《基于SpringBoot的校园网络设备管理与故障报修系统的设计与实现》本科毕业设计实现，围绕校园网络运维场景，提供设备管理、故障报修、工单分配、维修记录、公告与日志管理。

## 2. 技术架构
- 后端：Spring Boot 2.7 + Spring Security + JWT + MyBatis-Plus + MySQL
- 前端：Vue3 + Vite + Element Plus + Pinia + Vue Router + Axios
- 架构：前后端分离 RESTful API

## 3. 功能模块
- 登录认证与RBAC权限（admin/user/maintainer）
- 用户管理（管理员）
- 设备管理（管理员维护，普通用户可查）
- 报修工单与分配处理
- 维修记录填写与查询
- 公告发布与查看
- 登录日志/操作日志
- 首页统计

## 4. 项目目录树
```
backend/  # SpringBoot后端
frontend/ # Vue3前端
sql/init.sql # 建表+初始化数据
docs/ # 接口与论文辅助文档
```

## 5. 运行步骤
1. 创建数据库并导入：`sql/init.sql`
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
5. 访问：`http://localhost:5173`

## 6. 默认账号密码
- 管理员：admin / 123456
- 普通用户：user1 / 123456，user2 / 123456
- 维修人员：maint1 / 123456，maint2 / 123456

> 说明：演示环境初始化为明文密码，生产环境请替换为 BCrypt 加密值。

## 7. 外键逻辑说明（代码维护）
本系统不强制数据库物理外键，逻辑关联通过业务代码维护：
- `repair_order.device_id -> network_device.id`
- `repair_order.reporter_id -> sys_user.id`
- `repair_order.assign_maintainer_id -> sys_user.id`
- `repair_record.repair_order_id -> repair_order.id`
- `repair_record.device_id -> network_device.id`
- `repair_record.maintainer_id -> sys_user.id`
- `notice.publisher_id -> sys_user.id`
- `operation_log.user_id/login_log.user_id -> sys_user.id`

## 8. 接口说明与论文辅助材料
详见：
- `docs/api.md`
- `docs/thesis-support.md`

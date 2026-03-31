# current-issues.md

## A. 当前已确认的技术一致性问题

1. role 表缺少 remark 字段
- 报错：Unknown column 'remark' in 'field list'
- 相关位置：
  - SysRole
  - SysRoleMapper / MyBatis-Plus 自动映射
  - role 表 schema 不一致

2. PermissionService 仍依赖 permission / role_permission
- final_schema.sql 中缺少这两张表
- 权限模型处于半数据库、半代码常量状态

3. 实体字段与数据库列不一致
- NetworkDevice:
  - warrantyExpireDate <-> warranty_expiry_date
  - brandModel
  - ipAddress
  - macAddress
- RepairOrder:
  - scenePhotoUrls
  - handleDescription
  - delayReason
  - partsRequirement
- FileAttachment:
  - originalFileName
  - filePath
  - fileSize
  - fileHash
  - uploaderEmployeeNo
- ThirdPartyBind:
  - userEmployeeNo
  - unbindTime

4. SQL 脚本不统一
- init.sql 与 final_schema.sql 口径冲突
- 需要统一最终可信脚本

5. module/** 与 module/v2/** 双轨并存
- 需要评估主线依赖并收口

6. 前端死接口
- /repair-orders/auto-dispatch 前端存在，后端主线未实现

7. 前端依赖环境需要重装
- node_modules 不可信
- 需重新安装并验证 build

## B. 当前已确认的业务缺口

1. 前端缺少角色管理正式页面、路由、菜单
2. 公告管理入口未正式接入，NoticeView.vue 存在但未交付
3. 主线缺少数据字典管理
4. 主线缺少设备类型管理入口
5. 图片仍是 URL 登记，不是真实文件上传
6. 第三方登录只是 mock，占位架构未整理清楚
7. 缺少真正消息/通知中心
8. 配件采购只是状态动作，不是结构化流程
9. 统计分析不够独立和企业化
10. 工单状态机规则仍可进一步集中封装
11. 演示数据未统一重构
12. 存在历史残留页面和重复页面

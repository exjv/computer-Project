# 第7步：报修工单完整生命周期骨架重构

## 本步完成

1. 新增工单状态枚举 `RepairOrderStatusEnum`，覆盖完整生命周期骨架状态：
   - 待提交
   - 已提交/待审核
   - 审核通过 / 审核驳回
   - 待分配 / 已分配
   - 待接单 / 维修人员已接单 / 维修中
   - 待采购/待配件
   - 申请延期中 / 延期已批准
   - 待验收/待确认
   - 已完成 / 已关闭 / 已取消
2. 工单服务层重构：
   - 创建工单默认状态改为 `已提交/待审核`
   - 状态集合和合法流转映射改为枚举驱动
   - 自动派单、统计、删除保护等状态判断同步改造
3. 工单列表支持分页+筛选+排序（创建时间/报修时间/优先级/状态）。
4. 工单详情接口继续使用 `/api/repair-orders/{id}`，并新增状态选项接口 `/api/repair-orders/status-options`。
5. 按角色可见范围继续生效（管理员全量、报修人本人、维修人本人）。
6. `repair_order_flow` 流程记录持续联动（提交、审核、分配、接单、进度、完工等）。
7. 前端工单模块重构：
   - 列表页状态体系同步
   - 详情页初版（独立路由）
   - 列表中支持跳转详情查看流程。

## 本步接口变化

- `GET /api/repair-orders/page` 新增排序参数：`sortField`、`sortOrder`
- `GET /api/repair-orders/my` 新增排序参数：`sortField`、`sortOrder`
- `GET /api/repair-orders/status-options` 新增状态选项接口
- `GET /api/repair-orders/{id}` 工单详情（沿用）
- `GET /api/repair-orders/{id}/flows` 流程记录（沿用）

## 本步新增页面

- 新增 `RepairOrderDetailView`（工单详情初版）

## 本步待后续

- 下一步补充具体动作接口与更细粒度状态流转规则（延期、采购、关闭原因等动作化）。

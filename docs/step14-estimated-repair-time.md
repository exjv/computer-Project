# 第14步：预计修复时间计算

## 本步完成

1. 后端预计修复时间计算接口：
   - `GET /api/repair-orders/{id}/estimate-finish-time`
   - 返回：预计完成时间、预测工时、预测依据说明
2. 预测模型因素（经验评分）：
   - 设备类型
   - 故障类型
   - 紧急程度
   - 是否需要采购配件
   - 维修人员历史平均处理时长
3. 实际完成后误差记录：
   - 完工时若存在预测时间，写入 `business_log`（`PREDICTION_ERROR`）
4. 统计模块基础数据：
   - 在 `/repair-orders/statistics` 增加预测准确性指标：
     - 可比较样本数
     - 平均绝对误差小时
     - 4小时内命中数
     - 24小时内命中数
5. 前端展示：
   - 工单详情页展示预计完成时间、预测工时、预测依据
   - 工单列表页展示预测准确性基础统计

## 涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/repair/vo/RepairEstimateVO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/RepairOrderService.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/controller/RepairOrderController.java`
- `frontend/src/views/repair/RepairOrderDetailView.vue`
- `frontend/src/views/repair/RepairOrderView.vue`
- `docs/step14-estimated-repair-time.md`

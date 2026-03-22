# 第13步：工单分配推荐算法

## 本步完成

1. 工单优先级计算（后端）：
   - 基于紧急程度、设备重要性、等待时长、故障影响范围计算优先级分值
2. 维修人员负载计算（后端）：
   - 基于未完成工单数、处理中任务数、历史平均处理时长、设备能力匹配度计算负载分
3. 推荐分配接口：
   - 新增 `GET /api/repair-orders/{id}/assign-recommendations`
   - 返回推荐分、优先级分、负载分、负载明细和推荐原因
4. 前端推荐分配UI：
   - 分配弹窗新增推荐列表
   - 展示推荐理由、负载情况、评分信息
   - 支持管理员手动选择并调整最终分配人

## 涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/repair/algorithm/RepairDispatchAlgorithm.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/vo/AssignmentRecommendationVO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/RepairOrderService.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/controller/RepairOrderController.java`
- `frontend/src/views/repair/RepairOrderView.vue`
- `docs/step13-dispatch-recommendation.md`

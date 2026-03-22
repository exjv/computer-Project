# 第12步：维修人员侧工单动作实现

## 本步完成

1. 维修人员完整流程动作：
   - 查看分配给自己的工单
   - 接单 / 拒单（拒单必须填写原因）
   - 开始维修
   - 更新维修进度（同步工单进度百分比）
   - 上传现场照片（落库附件表）
   - 填写处理说明
   - 填写预计完成时间
   - 申请延期（进入“申请延期中”）
   - 申请采购配件（进入“待采购/待配件”）
   - 提交完工结果（进入“待验收/待确认”）
2. 权限与范围控制：
   - 维修人员动作均校验仅可操作分配给自己的工单
3. 前端页面与面板：
   - 新增“我的待处理工单页”
   - 接单/拒单按钮
   - 维修中操作面板（进度、延期、配件、上传图片、完工提交）

## 本步涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/repair/dto/RepairOrderActionDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `frontend/src/views/repair/MaintainerPendingOrdersView.vue`
- `frontend/src/router/index.js`
- `frontend/src/layout/MainLayout.vue`
- `docs/step12-maintainer-actions.md`

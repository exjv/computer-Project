# 第16步：设备详情页、维修时间线与高故障预警

## 本步完成

1. 设备独立详情页：
   - 新增路由 `/devices/:id/profile` 和独立页面 `DeviceDetailView`
   - 展示基本信息卡片、当前状态、历史维修次数、最近维修记录、维修时间线、故障原因统计、关联工单列表
   - 展示保修状态、高故障预警、建议更换/重点巡检
2. 设备照片/维修留痕：
   - 新增设备附件上传与查询接口（设备照片、故障现场、维修完成）
   - 详情页支持上传与预览，形成设备维度留痕
3. 设备审批场景（报修后审批）
   - 报修创建时增加设备类型判断：核心/防火墙/服务器设备进入管理员审核流程；其他设备可直接进入待分配

## 涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/device/controller/DeviceController.java`
- `backend/src/main/java/com/jou/networkrepair/module/device/dto/DeviceAttachmentDTO.java`
- `backend/src/main/java/com/jou/networkrepair/module/repair/service/impl/RepairOrderServiceImpl.java`
- `frontend/src/views/device/DeviceDetailView.vue`
- `frontend/src/views/device/DeviceView.vue`
- `frontend/src/router/index.js`
- `docs/step16-device-detail-timeline-warning.md`

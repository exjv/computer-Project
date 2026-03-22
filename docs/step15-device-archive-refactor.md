# 第15步：设备管理重构为完整设备档案

## 本步完成

1. 设备档案能力升级（前端）：
   - 设备列表页 + 完整档案详情弹窗
   - 新增/编辑/删除设备档案
   - 设备状态维护弹窗（正常/维修中/停用/报废）
   - 档案字段覆盖：编号、名称、类型、品牌、型号、序列号、校区、楼宇/机房/办公室、采购/启用/保修、责任人、管理部门、状态、最近故障时间、累计报修/维修、故障原因统计、备注
2. 后端校验增强：
   - 设备编号唯一校验（新增、编辑）
   - 新增接口 `GET /api/devices/check-code`
   - 新增接口 `PUT /api/devices/{id}/status`
3. 设备关联信息：
   - 设备档案详情继续展示关联工单与关联维修记录（最近记录）

## 本步涉及文件

- `backend/src/main/java/com/jou/networkrepair/module/device/controller/DeviceController.java`
- `backend/src/main/java/com/jou/networkrepair/module/device/dto/DeviceStatusDTO.java`
- `frontend/src/views/device/DeviceView.vue`
- `docs/step15-device-archive-refactor.md`

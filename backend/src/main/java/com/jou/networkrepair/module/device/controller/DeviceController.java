package com.jou.networkrepair.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceMapper deviceMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final RepairRecordMapper repairRecordMapper;
    private final RepairOrderFlowMapper repairOrderFlowMapper;

    @GetMapping("/page")
    public ApiResult<Page<NetworkDevice>> page(@RequestParam Long current, @RequestParam Long size,
                                               @RequestParam(required = false) String deviceCode,
                                               @RequestParam(required = false) String deviceName,
                                               @RequestParam(required = false) String deviceType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String campus,
                                               @RequestParam(required = false) String buildingLocation) {
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .like(deviceCode != null && !deviceCode.isEmpty(), NetworkDevice::getDeviceCode, deviceCode)
                .like(deviceName != null && !deviceName.isEmpty(), NetworkDevice::getDeviceName, deviceName)
                .eq(deviceType != null && !deviceType.isEmpty(), NetworkDevice::getDeviceType, deviceType)
                .eq(status != null && !status.isEmpty(), NetworkDevice::getStatus, status)
                .eq(campus != null && !campus.isEmpty(), NetworkDevice::getCampus, campus)
                .like(buildingLocation != null && !buildingLocation.isEmpty(), NetworkDevice::getBuildingLocation, buildingLocation)
                .orderByDesc(NetworkDevice::getId);
        return ApiResult.success(deviceMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    public ApiResult<NetworkDevice> get(@PathVariable Long id) { return ApiResult.success(deviceMapper.selectById(id)); }

    @GetMapping("/{id}/profile")
    public ApiResult<Map<String, Object>> profile(@PathVariable Long id) {
        NetworkDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");
        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, id).orderByDesc(RepairOrder::getId));
        List<RepairRecord> records = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, id).orderByDesc(RepairRecord::getId));

        Map<String, Long> reasonStats = records.stream()
                .filter(r -> r.getFaultReason() != null && !r.getFaultReason().trim().isEmpty())
                .collect(Collectors.groupingBy(RepairRecord::getFaultReason, Collectors.counting()));

        long totalRepairs = records.size();
        long totalOrders = orders.size();
        Optional<RepairOrder> recentOrder = orders.stream().max(Comparator.comparing(RepairOrder::getReportTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        boolean isHighFault = totalOrders >= 5;
        boolean needReplace = totalOrders >= 8 || "停用".equals(device.getStatus());
        Long avgHours = calcAverageDurationHours(orders);

        Map<String, Object> data = new HashMap<>();
        data.put("device", device);
        data.put("totalOrders", totalOrders);
        data.put("totalRepairs", totalRepairs);
        data.put("avgRepairHours", avgHours);
        data.put("recentFaultTime", recentOrder.map(RepairOrder::getReportTime).orElse(null));
        data.put("isHighFault", isHighFault);
        data.put("needReplace", needReplace);
        data.put("recentOrders", orders.stream().limit(10).collect(Collectors.toList()));
        data.put("recentRecords", records.stream().limit(10).collect(Collectors.toList()));
        data.put("faultReasonStats", reasonStats);
        return ApiResult.success(data);
    }

    @GetMapping("/{id}/detail")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        NetworkDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");

        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, id).orderByDesc(RepairOrder::getReportTime));
        List<Long> orderIds = orders.stream().map(RepairOrder::getId).collect(Collectors.toList());
        List<RepairOrderFlow> flows = orderIds.isEmpty() ? Collections.emptyList() : repairOrderFlowMapper.selectList(
                new LambdaQueryWrapper<RepairOrderFlow>().in(RepairOrderFlow::getRepairOrderId, orderIds).orderByDesc(RepairOrderFlow::getCreateTime));
        List<RepairRecord> records = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, id).orderByDesc(RepairRecord::getRepairTime));

        Map<String, Long> reasonStats = records.stream()
                .filter(r -> r.getFaultReason() != null && !r.getFaultReason().trim().isEmpty())
                .collect(Collectors.groupingBy(RepairRecord::getFaultReason, Collectors.counting()));

        List<String> photos = orders.stream()
                .map(RepairOrder::getScenePhotoUrls)
                .filter(s -> s != null && !s.trim().isEmpty())
                .flatMap(s -> Arrays.stream(s.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        long activeOrders = orders.stream().filter(o -> !Arrays.asList("已完成", "已关闭", "已取消", "审核驳回").contains(o.getStatus())).count();
        long recent90dFaults = orders.stream().filter(o -> o.getReportTime() != null && o.getReportTime().isAfter(LocalDateTime.now().minusDays(90))).count();
        boolean highFaultWarning = recent90dFaults >= 3 || activeOrders >= 2;
        boolean inWarranty = device.getWarrantyExpiryDate() != null && !device.getWarrantyExpiryDate().isBefore(LocalDate.now());
        long topReasonCount = reasonStats.values().stream().max(Long::compareTo).orElse(0L);
        boolean suggestReplace = orders.size() >= 10 || (!inWarranty && orders.size() >= 6) || topReasonCount >= 5;
        boolean suggestInspect = !suggestReplace && (highFaultWarning || orders.size() >= 5);

        List<Map<String, Object>> orderList = orders.stream().limit(20).map(o -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("title", o.getTitle());
            m.put("status", o.getStatus());
            m.put("priority", o.getPriority());
            m.put("reportTime", o.getReportTime());
            m.put("finishTime", o.getFinishTime());
            return m;
        }).collect(Collectors.toList());
        List<Map<String, Object>> timeline = flows.stream().limit(40).map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("time", f.getCreateTime());
            m.put("fromStatus", f.getFromStatus());
            m.put("toStatus", f.getToStatus());
            m.put("action", f.getAction());
            m.put("remark", f.getRemark());
            m.put("orderId", f.getRepairOrderId());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> data = new HashMap<>();
        data.put("device", device);
        data.put("currentStatus", device.getStatus());
        data.put("historyRepairCount", records.size());
        data.put("recentRepairRecord", records.isEmpty() ? null : records.get(0));
        data.put("repairTimeline", timeline);
        data.put("faultReasonStats", reasonStats);
        data.put("repairPhotos", photos);
        data.put("relatedOrders", orderList);
        data.put("inWarranty", inWarranty);
        data.put("highFaultWarning", highFaultWarning);
        data.put("highFaultThreshold", "近90天报修>=3 或 当前活跃工单>=2");
        data.put("suggestReplace", suggestReplace);
        data.put("suggestInspect", suggestInspect);
        data.put("recommendation", suggestReplace ? "建议更换" : (suggestInspect ? "建议重点巡检" : "维持常规巡检"));
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orders.size());
        stats.put("activeOrders", activeOrders);
        stats.put("recent90dFaults", recent90dFaults);
        stats.put("totalRepairs", records.size());
        data.put("stats", stats);
        return ApiResult.success(data);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> add(@RequestBody NetworkDevice entity) {
        validateDevice(entity, null);
        fillLegacyFields(entity);
        entity.setTotalRepairCount(0);
        entity.setTotalRepairRequests(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.insert(entity); return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NetworkDevice entity) {
        NetworkDevice old = deviceMapper.selectById(id);
        if (old == null) throw new BusinessException("设备不存在");
        validateDevice(entity, id);
        if (Arrays.asList("维修中").contains(old.getStatus()) && Arrays.asList("停用", "报废").contains(entity.getStatus())) {
            throw new BusinessException("维修中的设备不可直接变更为停用/报废，请先结束关联维修工单");
        }
        fillLegacyFields(entity);
        entity.setId(id);
        entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(entity); return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long count = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getDeviceId, id));
        if (count != null && count > 0L) throw new BusinessException("存在关联报修记录，无法删除");
        deviceMapper.deleteById(id); return ApiResult.success("删除成功", null);
    }

    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> statistics() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", deviceMapper.selectCount(null));
        map.put("normal", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().eq(NetworkDevice::getStatus, "正常")));
        map.put("fault", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().eq(NetworkDevice::getStatus, "维修中")));
        return ApiResult.success(map);
    }

    private void validateDevice(NetworkDevice entity, Long id) {
        if (entity.getDeviceCode() == null || entity.getDeviceCode().trim().isEmpty()) throw new BusinessException("设备编号不能为空");
        if (entity.getDeviceName() == null || entity.getDeviceName().trim().isEmpty()) throw new BusinessException("设备名称不能为空");
        if (entity.getDeviceType() == null || entity.getDeviceType().trim().isEmpty()) throw new BusinessException("设备类型不能为空");
        if (entity.getBrand() == null || entity.getBrand().trim().isEmpty()) throw new BusinessException("品牌不能为空");
        if (entity.getModel() == null || entity.getModel().trim().isEmpty()) throw new BusinessException("型号不能为空");
        if (entity.getSerialNumber() == null || entity.getSerialNumber().trim().isEmpty()) throw new BusinessException("序列号不能为空");
        if (entity.getCampus() == null || entity.getCampus().trim().isEmpty()) throw new BusinessException("所属校区不能为空");
        if (entity.getBuildingLocation() == null || entity.getBuildingLocation().trim().isEmpty()) throw new BusinessException("所属楼宇/机房/办公室不能为空");
        if (entity.getOwnerName() == null || entity.getOwnerName().trim().isEmpty()) throw new BusinessException("责任人不能为空");
        if (entity.getManageDepartment() == null || entity.getManageDepartment().trim().isEmpty()) throw new BusinessException("管理部门不能为空");
        if (entity.getStatus() == null || !Arrays.asList("正常", "维修中", "停用", "报废").contains(entity.getStatus())) {
            throw new BusinessException("设备状态仅支持：正常/维修中/停用/报废");
        }
        if (entity.getPurchaseDate() != null && entity.getEnableDate() != null && entity.getEnableDate().isBefore(entity.getPurchaseDate())) {
            throw new BusinessException("启用时间不能早于购买时间");
        }
        if (entity.getWarrantyExpiryDate() != null && entity.getPurchaseDate() != null && entity.getWarrantyExpiryDate().isBefore(entity.getPurchaseDate())) {
            throw new BusinessException("保修截止时间不能早于购买时间");
        }
        Long sameCode = deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>()
                .eq(NetworkDevice::getDeviceCode, entity.getDeviceCode())
                .ne(id != null, NetworkDevice::getId, id));
        if (sameCode != null && sameCode > 0) throw new BusinessException("设备编号已存在");
    }

    private void fillLegacyFields(NetworkDevice entity) {
        entity.setBrandModel((entity.getBrand() == null ? "" : entity.getBrand()) + "/" + (entity.getModel() == null ? "" : entity.getModel()));
        entity.setLocation(entity.getBuildingLocation());
        if (entity.getRepairApprovalRequired() == null) entity.setRepairApprovalRequired(0);
    }

    private Long calcAverageDurationHours(List<RepairOrder> orders) {
        List<Long> durations = orders.stream()
                .filter(o -> o.getStartRepairTime() != null && o.getFinishTime() != null)
                .map(o -> ChronoUnit.HOURS.between(o.getStartRepairTime(), o.getFinishTime()))
                .filter(h -> h >= 0)
                .collect(Collectors.toList());
        if (durations.isEmpty()) return 0L;
        return durations.stream().reduce(0L, Long::sum) / durations.size();
    }
}

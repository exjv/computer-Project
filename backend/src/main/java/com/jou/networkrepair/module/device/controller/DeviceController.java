package com.jou.networkrepair.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/page")
    public ApiResult<Page<NetworkDevice>> page(@RequestParam Long current, @RequestParam Long size,
                                               @RequestParam(required = false) String deviceName,
                                               @RequestParam(required = false) String deviceType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String location) {
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .like(deviceName != null && !deviceName.isEmpty(), NetworkDevice::getDeviceName, deviceName)
                .eq(deviceType != null && !deviceType.isEmpty(), NetworkDevice::getDeviceType, deviceType)
                .eq(status != null && !status.isEmpty(), NetworkDevice::getStatus, status)
                .like(location != null && !location.isEmpty(), NetworkDevice::getLocation, location)
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

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
    public ApiResult<Void> add(@RequestBody NetworkDevice entity) {
        entity.setCreateTime(LocalDateTime.now()); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.insert(entity); return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NetworkDevice entity) {
        entity.setId(id); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(entity); return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
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
        map.put("fault", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().in(NetworkDevice::getStatus, "故障", "维修中")));
        return ApiResult.success(map);
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

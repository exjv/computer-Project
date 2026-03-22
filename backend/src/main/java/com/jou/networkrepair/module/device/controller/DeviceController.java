package com.jou.networkrepair.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.dto.DeviceAttachmentDTO;
import com.jou.networkrepair.module.device.dto.DeviceStatusDTO;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.system.entity.FileAttachment;
import com.jou.networkrepair.module.system.mapper.FileAttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
    private final FileAttachmentMapper fileAttachmentMapper;

    @GetMapping("/page")
    public ApiResult<Page<NetworkDevice>> page(@RequestParam Long current, @RequestParam Long size,
                                               @RequestParam(required = false) String deviceCode,
                                               @RequestParam(required = false) String deviceName,
                                               @RequestParam(required = false) String deviceType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String location) {
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .like(deviceCode != null && !deviceCode.isEmpty(), NetworkDevice::getDeviceCode, deviceCode)
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
        boolean suggestPatrol = totalOrders >= 4 || totalRepairs >= 4;
        boolean inWarranty = device.getWarrantyExpireDate() != null && !device.getWarrantyExpireDate().isBefore(java.time.LocalDate.now());
        Long avgHours = calcAverageDurationHours(orders);
        List<FileAttachment> photos = fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                .eq(FileAttachment::getBusinessType, "DEVICE")
                .eq(FileAttachment::getBusinessId, id)
                .orderByDesc(FileAttachment::getId));

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
        data.put("inWarranty", inWarranty);
        data.put("suggestPatrol", suggestPatrol);
        data.put("photos", photos);
        return ApiResult.success(data);
    }

    @GetMapping("/{id}/attachments")
    public ApiResult<List<FileAttachment>> attachments(@PathVariable Long id, @RequestParam(required = false) String category) {
        NetworkDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");
        LambdaQueryWrapper<FileAttachment> qw = new LambdaQueryWrapper<FileAttachment>()
                .eq(FileAttachment::getBusinessType, "DEVICE")
                .eq(FileAttachment::getBusinessId, id)
                .like(category != null && !category.trim().isEmpty(), FileAttachment::getRemark, "[CATEGORY:" + category + "]");
        return ApiResult.success(fileAttachmentMapper.selectList(qw.orderByDesc(FileAttachment::getId)));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "') || @permissionService.hasPermission('" + PermissionCode.REPAIR_RECORD_WRITE + "')")
    public ApiResult<Void> uploadAttachment(@PathVariable Long id, @RequestBody @Validated DeviceAttachmentDTO dto) {
        NetworkDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");
        FileAttachment attachment = new FileAttachment();
        attachment.setBusinessType("DEVICE");
        attachment.setBusinessId(id);
        attachment.setFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileType(dto.getFileType());
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setRemark("[CATEGORY:" + dto.getCategory() + "] " + (dto.getRemark() == null ? "" : dto.getRemark()));
        fileAttachmentMapper.insert(attachment);
        return ApiResult.success("上传成功", null);
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
    public ApiResult<Void> add(@RequestBody NetworkDevice entity) {
        assertUniqueDeviceCode(entity.getDeviceCode(), null);
        entity.setCreateTime(LocalDateTime.now()); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.insert(entity); return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NetworkDevice entity) {
        assertUniqueDeviceCode(entity.getDeviceCode(), id);
        entity.setId(id); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(entity); return ApiResult.success("修改成功", null);
    }

    @GetMapping("/check-code")
    public ApiResult<Boolean> checkCode(@RequestParam String deviceCode, @RequestParam(required = false) Long excludeId) {
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .eq(NetworkDevice::getDeviceCode, deviceCode);
        if (excludeId != null) qw.ne(NetworkDevice::getId, excludeId);
        Long count = deviceMapper.selectCount(qw);
        return ApiResult.success(count == null || count == 0L);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.DEVICE_MANAGE + "')")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated DeviceStatusDTO dto) {
        NetworkDevice exists = deviceMapper.selectById(id);
        if (exists == null) throw new BusinessException("设备不存在");
        exists.setStatus(dto.getStatus());
        exists.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(exists);
        return ApiResult.success("状态更新成功", null);
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

    private void assertUniqueDeviceCode(String deviceCode, Long excludeId) {
        if (deviceCode == null || deviceCode.trim().isEmpty()) throw new BusinessException("设备编号不能为空");
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .eq(NetworkDevice::getDeviceCode, deviceCode.trim());
        if (excludeId != null) qw.ne(NetworkDevice::getId, excludeId);
        Long count = deviceMapper.selectCount(qw);
        if (count != null && count > 0L) throw new BusinessException("设备编号已存在");
    }
}

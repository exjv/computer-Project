package com.jou.networkrepair.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.dto.DeviceAttachmentDTO;
import com.jou.networkrepair.module.device.dto.DeviceStatusDTO;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderFlowMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import com.jou.networkrepair.module.system.entity.FileAttachment;
import com.jou.networkrepair.module.system.mapper.FileAttachmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private static final Set<String> ACTIVE_ORDER_STATUS = new HashSet<>(Arrays.asList(
            "已提交/待审核", "审核通过", "待分配", "待接单", "维修人员已接单", "维修中", "待采购/待配件", "申请延期中", "延期已批准", "待验收/待确认"
    ));
    private static final Set<String> VALID_ATTACHMENT_CATEGORY = new HashSet<>(Arrays.asList("DEVICE_PHOTO", "FAULT_SCENE", "REPAIR_RESULT"));

    private final DeviceMapper deviceMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final RepairRecordMapper repairRecordMapper;
    private final RepairOrderFlowMapper repairOrderFlowMapper;
    private final FileAttachmentMapper fileAttachmentMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<NetworkDevice>> page(@RequestParam Long current, @RequestParam Long size,
                                               @RequestParam(required = false) String deviceCode,
                                               @RequestParam(required = false) String deviceName,
                                               @RequestParam(required = false) String deviceType,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String campus,
                                               @RequestParam(required = false) String buildingLocation) {
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .like(notBlank(deviceCode), NetworkDevice::getDeviceCode, deviceCode)
                .like(notBlank(deviceName), NetworkDevice::getDeviceName, deviceName)
                .eq(notBlank(deviceType), NetworkDevice::getDeviceType, deviceType)
                .eq(notBlank(status), NetworkDevice::getStatus, status)
                .eq(notBlank(campus), NetworkDevice::getCampus, campus)
                .like(notBlank(buildingLocation), NetworkDevice::getBuildingLocation, buildingLocation)
                .orderByDesc(NetworkDevice::getId);
        return ApiResult.success(deviceMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<NetworkDevice> get(@PathVariable Long id) {
        return ApiResult.success(deviceMapper.selectById(id));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> detail(@PathVariable Long id) {
        NetworkDevice device = deviceMapper.selectById(id);
        if (device == null) throw new BusinessException("设备不存在");

        List<RepairOrder> orders = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getDeviceId, id)
                .orderByDesc(RepairOrder::getReportTime));
        List<Long> orderIds = orders.stream().map(RepairOrder::getId).collect(Collectors.toList());
        List<RepairOrderFlow> flows = orderIds.isEmpty() ? Collections.emptyList() : repairOrderFlowMapper.selectList(
                new LambdaQueryWrapper<RepairOrderFlow>()
                        .in(RepairOrderFlow::getRepairOrderId, orderIds)
                        .orderByAsc(RepairOrderFlow::getCreateTime));
        List<RepairRecord> records = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(RepairRecord::getDeviceId, id)
                .orderByDesc(RepairRecord::getRepairTime));

        List<FileAttachment> attachments = fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                .and(w -> w.eq(FileAttachment::getBusinessType, "DEVICE").or().eq(FileAttachment::getBizType, "DEVICE"))
                .and(w -> w.eq(FileAttachment::getBusinessId, id).or().eq(FileAttachment::getBizId, id))
                .orderByDesc(FileAttachment::getId));

        Map<String, Long> reasonStats = records.stream()
                .filter(r -> notBlank(r.getFaultReason()))
                .collect(Collectors.groupingBy(RepairRecord::getFaultReason, Collectors.counting()));

        long activeOrders = orders.stream().filter(o -> ACTIVE_ORDER_STATUS.contains(o.getStatus())).count();
        long recent90dFaults = orders.stream().filter(o ->
                o.getReportTime() != null
                        && o.getReportTime().isAfter(LocalDateTime.now().minusDays(90))
                        && !"审核驳回".equals(o.getStatus())
                        && !"已取消".equals(o.getStatus())).count();
        boolean highFaultWarning = recent90dFaults >= 3 || activeOrders >= 2;
        boolean inWarranty = warrantyDate(device) != null && !warrantyDate(device).isBefore(LocalDate.now());
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

        List<Map<String, Object>> timeline = flows.stream().limit(60).map(f -> {
            Map<String, Object> m = new HashMap<>();
            m.put("time", f.getCreateTime());
            m.put("fromStatus", f.getFromStatus());
            m.put("toStatus", f.getToStatus());
            m.put("action", f.getAction());
            m.put("remark", f.getRemark());
            m.put("orderId", f.getRepairOrderId());
            return m;
        }).collect(Collectors.toList());

        Map<String, List<FileAttachment>> attachmentByCategory = attachments.stream()
                .collect(Collectors.groupingBy(a -> normalizeCategory(a.getFileType())));

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", orders.size());
        stats.put("activeOrders", activeOrders);
        stats.put("recent90dFaults", recent90dFaults);
        stats.put("totalRepairs", records.size());

        Map<String, Object> data = new HashMap<>();
        data.put("device", device);
        data.put("currentStatus", device.getStatus());
        data.put("historyRepairCount", records.size());
        data.put("recentRepairRecord", records.isEmpty() ? null : records.get(0));
        data.put("repairTimeline", timeline);
        data.put("faultReasonStats", reasonStats);
        data.put("relatedOrders", orderList);
        data.put("stats", stats);

        data.put("attachments", attachments);
        data.put("devicePhotos", attachmentByCategory.getOrDefault("DEVICE_PHOTO", Collections.emptyList()));
        data.put("faultScenePhotos", attachmentByCategory.getOrDefault("FAULT_SCENE", Collections.emptyList()));
        data.put("repairResultPhotos", attachmentByCategory.getOrDefault("REPAIR_RESULT", Collections.emptyList()));

        data.put("inWarranty", inWarranty);
        data.put("highFaultWarning", highFaultWarning);
        data.put("highFaultThreshold", "近90天报修>=3 或 当前活跃工单>=2");
        data.put("suggestReplace", suggestReplace);
        data.put("suggestInspect", suggestInspect);
        data.put("recommendation", suggestReplace ? "建议更换" : (suggestInspect ? "建议重点巡检" : "维持常规巡检"));

        return ApiResult.success(data);
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<List<FileAttachment>> attachments(@PathVariable Long id) {
        assertDeviceExists(id);
        return ApiResult.success(fileAttachmentMapper.selectList(new LambdaQueryWrapper<FileAttachment>()
                .and(w -> w.eq(FileAttachment::getBusinessType, "DEVICE").or().eq(FileAttachment::getBizType, "DEVICE"))
                .and(w -> w.eq(FileAttachment::getBusinessId, id).or().eq(FileAttachment::getBizId, id))
                .orderByDesc(FileAttachment::getId)));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Void> addAttachment(@PathVariable Long id,
                                         @RequestBody @Validated DeviceAttachmentDTO dto,
                                         HttpServletRequest request) {
        assertDeviceExists(id);
        if (!VALID_ATTACHMENT_CATEGORY.contains(dto.getCategory())) {
            throw new BusinessException("图片分类仅支持：DEVICE_PHOTO/FAULT_SCENE/REPAIR_RESULT");
        }

        Long userId = (Long) request.getAttribute("userId");
        LocalDateTime now = LocalDateTime.now();

        FileAttachment attachment = new FileAttachment();
        attachment.setBusinessType("DEVICE");
        attachment.setBusinessId(id);
        attachment.setBizType("DEVICE");
        attachment.setBizId(id);
        attachment.setFileName(dto.getFileName());
        attachment.setOriginalFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileType(dto.getCategory());
        attachment.setRemark(dto.getRemark());
        attachment.setUploaderId(userId);
        attachment.setUploadTime(now);
        attachment.setCreateTime(now);
        attachment.setUpdateTime(now);
        fileAttachmentMapper.insert(attachment);
        return ApiResult.success("上传记录成功", null);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "设备管理", operationType = "新增", operationDesc = "新增设备档案")
    public ApiResult<Void> add(@RequestBody NetworkDevice entity) {
        validateDevice(entity, null);
        fillLegacyFields(entity);
        entity.setTotalRepairCount(0);
        entity.setTotalRepairRequests(0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.insert(entity);
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "设备管理", operationType = "修改", operationDesc = "编辑设备档案")
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
        deviceMapper.updateById(entity);
        return ApiResult.success("修改成功", null);
    }

    @GetMapping("/check-code")
    public ApiResult<Boolean> checkCode(@RequestParam String deviceCode, @RequestParam(required = false) Long excludeId) {
        assertUniqueDeviceCode(deviceCode, excludeId);
        return ApiResult.success(Boolean.TRUE);
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
    @PreAuthorize("hasRole('ADMIN')")
    @Loggable(module = "设备管理", operationType = "删除", operationDesc = "删除设备")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Long count = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getDeviceId, id));
        if (count != null && count > 0L) throw new BusinessException("存在关联报修记录，无法删除");
        deviceMapper.deleteById(id);
        return ApiResult.success("删除成功", null);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> statistics() {
        Map<String, Object> map = new HashMap<>();
        map.put("total", deviceMapper.selectCount(null));
        map.put("normal", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().eq(NetworkDevice::getStatus, "正常")));
        map.put("fault", deviceMapper.selectCount(new LambdaQueryWrapper<NetworkDevice>().eq(NetworkDevice::getStatus, "维修中")));
        return ApiResult.success(map);
    }

    private void validateDevice(NetworkDevice entity, Long id) {
        if (!notBlank(entity.getDeviceCode())) throw new BusinessException("设备编号不能为空");
        if (!notBlank(entity.getDeviceName())) throw new BusinessException("设备名称不能为空");
        if (!notBlank(entity.getDeviceType())) throw new BusinessException("设备类型不能为空");
        if (!notBlank(entity.getBrand())) throw new BusinessException("品牌不能为空");
        if (!notBlank(entity.getModel())) throw new BusinessException("型号不能为空");
        if (!notBlank(entity.getSerialNumber())) throw new BusinessException("序列号不能为空");
        if (!notBlank(entity.getCampus())) throw new BusinessException("所属校区不能为空");
        if (!notBlank(entity.getBuildingLocation())) throw new BusinessException("所属楼宇/机房/办公室不能为空");
        if (!notBlank(entity.getOwnerName())) throw new BusinessException("责任人不能为空");
        if (!notBlank(entity.getManageDepartment())) throw new BusinessException("管理部门不能为空");
        if (entity.getStatus() == null || !Arrays.asList("正常", "维修中", "停用", "报废").contains(entity.getStatus())) {
            throw new BusinessException("设备状态仅支持：正常/维修中/停用/报废");
        }
        if (entity.getPurchaseDate() != null && entity.getEnableDate() != null && entity.getEnableDate().isBefore(entity.getPurchaseDate())) {
            throw new BusinessException("启用时间不能早于购买时间");
        }
        if (warrantyDate(entity) != null && entity.getPurchaseDate() != null && warrantyDate(entity).isBefore(entity.getPurchaseDate())) {
            throw new BusinessException("保修截止时间不能早于购买时间");
        }
        assertUniqueDeviceCode(entity.getDeviceCode(), id);
    }

    private void fillLegacyFields(NetworkDevice entity) {
        entity.setBrandModel((entity.getBrand() == null ? "" : entity.getBrand()) + "/" + (entity.getModel() == null ? "" : entity.getModel()));
        entity.setLocation(entity.getBuildingLocation());
        entity.setManagementDept(entity.getManageDepartment());
        entity.setWarrantyExpireDate(warrantyDate(entity));
        entity.setWarrantyExpiryDate(entity.getWarrantyExpireDate());
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

    private void assertUniqueDeviceCode(String deviceCode, Long excludeId) {
        if (!notBlank(deviceCode)) throw new BusinessException("设备编号不能为空");
        LambdaQueryWrapper<NetworkDevice> qw = new LambdaQueryWrapper<NetworkDevice>()
                .eq(NetworkDevice::getDeviceCode, deviceCode.trim());
        if (excludeId != null) qw.ne(NetworkDevice::getId, excludeId);
        Long count = deviceMapper.selectCount(qw);
        if (count != null && count > 0L) throw new BusinessException("设备编号已存在");
    }

    private void assertDeviceExists(Long id) {
        if (deviceMapper.selectById(id) == null) throw new BusinessException("设备不存在");
    }

    private String normalizeCategory(String fileType) {
        if (VALID_ATTACHMENT_CATEGORY.contains(fileType)) return fileType;
        return "DEVICE_PHOTO";
    }

    private boolean notBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private LocalDate warrantyDate(NetworkDevice device) {
        return device.getWarrantyExpiryDate() != null ? device.getWarrantyExpiryDate() : device.getWarrantyExpireDate();
    }
}

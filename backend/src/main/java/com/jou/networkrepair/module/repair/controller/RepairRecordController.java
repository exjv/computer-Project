package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
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

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/repair-records")
@RequiredArgsConstructor
public class RepairRecordController {
    private final RepairRecordMapper repairRecordMapper;
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<RepairRecord>> page(@RequestParam Long current, @RequestParam Long size,
                                              @RequestParam(required = false) Long deviceId,
                                              @RequestParam(required = false) Long maintainerId,
                                              HttpServletRequest request) {
        String role = (String) request.getAttribute("role"); Long uid = (Long) request.getAttribute("userId");
        LambdaQueryWrapper<RepairRecord> qw = new LambdaQueryWrapper<RepairRecord>()
                .eq(deviceId != null, RepairRecord::getDeviceId, deviceId)
                .eq(maintainerId != null, RepairRecord::getMaintainerId, maintainerId)
                .orderByDesc(RepairRecord::getId);
        if ("maintainer".equals(role)) qw.eq(RepairRecord::getMaintainerId, uid);
        if ("user".equals(role)) qw.inSql(RepairRecord::getRepairOrderId, "select id from repair_order where reporter_id = " + uid);
        return ApiResult.success(repairRecordMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<RepairRecord> get(@PathVariable Long id, HttpServletRequest request) {
        RepairRecord record = repairRecordMapper.selectById(id);
        String role = (String) request.getAttribute("role"); Long uid = (Long) request.getAttribute("userId");
        if ("maintainer".equals(role) && !uid.equals(record.getMaintainerId())) throw new BusinessException("无权查看");
        if ("user".equals(role)) {
            Integer cnt = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getId, record.getRepairOrderId()).eq(RepairOrder::getReporterId, uid));
            if (cnt == 0) throw new BusinessException("无权查看");
        }
        return ApiResult.success(record);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ApiResult<Void> add(@RequestBody RepairRecord record, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long uid = (Long) request.getAttribute("userId");
        if ("maintainer".equals(role)) record.setMaintainerId(uid);
        record.setRepairTime(LocalDateTime.now()); record.setCreateTime(LocalDateTime.now()); record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.insert(record);
        RepairOrder order = repairOrderMapper.selectById(record.getRepairOrderId());
        if (record.getIsResolved() != null && record.getIsResolved() == 1) {
            order.setStatus("已完成"); order.setFinishTime(LocalDateTime.now());
            NetworkDevice dev = new NetworkDevice(); dev.setId(record.getDeviceId()); dev.setStatus("正常"); deviceMapper.updateById(dev);
        } else order.setStatus("处理中");
        order.setUpdateTime(LocalDateTime.now()); repairOrderMapper.updateById(order);
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairRecord record, HttpServletRequest request) {
        RepairRecord old = repairRecordMapper.selectById(id);
        String role = (String) request.getAttribute("role"); Long uid = (Long) request.getAttribute("userId");
        if ("maintainer".equals(role) && !uid.equals(old.getMaintainerId())) throw new BusinessException("仅可修改本人维修记录");
        record.setId(id); record.setUpdateTime(LocalDateTime.now());
        repairRecordMapper.updateById(record);
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) { repairRecordMapper.deleteById(id); return ApiResult.success("删除成功", null); }
}

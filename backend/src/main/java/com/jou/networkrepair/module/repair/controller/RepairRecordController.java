package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import lombok.RequiredArgsConstructor;
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
        return ApiResult.success(repairRecordMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    public ApiResult<RepairRecord> get(@PathVariable Long id) { return ApiResult.success(repairRecordMapper.selectById(id)); }

    @PostMapping
    public ApiResult<Void> add(@RequestBody RepairRecord record) {
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
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairRecord record) { record.setId(id); record.setUpdateTime(LocalDateTime.now()); repairRecordMapper.updateById(record); return ApiResult.success("修改成功", null); }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) { repairRecordMapper.deleteById(id); return ApiResult.success("删除成功", null); }
}

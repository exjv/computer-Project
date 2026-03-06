package com.jou.networkrepair.module.device.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceMapper deviceMapper;
    private final RepairOrderMapper repairOrderMapper;

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

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> add(@RequestBody NetworkDevice entity) {
        entity.setCreateTime(LocalDateTime.now()); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.insert(entity); return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody NetworkDevice entity) {
        entity.setId(id); entity.setUpdateTime(LocalDateTime.now());
        deviceMapper.updateById(entity); return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> delete(@PathVariable Long id) {
        Integer count = repairOrderMapper.selectCount(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getDeviceId, id));
        if (count > 0) throw new BusinessException("存在关联报修记录，无法删除");
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
}

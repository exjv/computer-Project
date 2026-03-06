package com.jou.networkrepair.module.repair.controller;

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

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {
    private final RepairOrderMapper repairOrderMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ApiResult<Page<RepairOrder>> page(@RequestParam Long current, @RequestParam Long size,
                                             @RequestParam(required = false) String status) {
        return ApiResult.success(repairOrderMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<RepairOrder>().eq(status != null && !status.isEmpty(), RepairOrder::getStatus, status).orderByDesc(RepairOrder::getId)));
    }

    @GetMapping("/my")
    public ApiResult<Page<RepairOrder>> my(@RequestParam Long current, @RequestParam Long size, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<>();
        if ("user".equals(role)) qw.eq(RepairOrder::getReporterId, userId);
        if ("maintainer".equals(role)) qw.eq(RepairOrder::getAssignMaintainerId, userId);
        qw.orderByDesc(RepairOrder::getId);
        return ApiResult.success(repairOrderMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/{id}")
    public ApiResult<RepairOrder> get(@PathVariable Long id, HttpServletRequest request) {
        RepairOrder order = repairOrderMapper.selectById(id);
        String role = (String) request.getAttribute("role"); Long uid = (Long) request.getAttribute("userId");
        if ("user".equals(role) && !uid.equals(order.getReporterId())) throw new BusinessException("无权查看");
        if ("maintainer".equals(role) && !uid.equals(order.getAssignMaintainerId())) throw new BusinessException("无权查看");
        return ApiResult.success(order);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ApiResult<Void> add(@RequestBody RepairOrder order, HttpServletRequest request) {
        order.setReporterId((Long) request.getAttribute("userId"));
        order.setOrderNo("RO" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
        order.setStatus("待处理"); order.setReportTime(LocalDateTime.now()); order.setCreateTime(LocalDateTime.now()); order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.insert(order);
        NetworkDevice device = new NetworkDevice(); device.setId(order.getDeviceId()); device.setStatus("故障"); deviceMapper.updateById(device);
        return ApiResult.success("提交成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairOrder req) { req.setId(id); req.setUpdateTime(LocalDateTime.now()); repairOrderMapper.updateById(req); return ApiResult.success("修改成功", null); }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody RepairOrder req) {
        RepairOrder order = repairOrderMapper.selectById(id);
        order.setAssignMaintainerId(req.getAssignMaintainerId()); order.setAssignTime(LocalDateTime.now()); order.setStatus("已分配"); order.setUpdateTime(LocalDateTime.now());
        repairOrderMapper.updateById(order); return ApiResult.success("分配成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody RepairOrder req) {
        RepairOrder order = repairOrderMapper.selectById(id);
        order.setStatus(req.getStatus());
        if ("已完成".equals(req.getStatus())) order.setFinishTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now()); repairOrderMapper.updateById(order);
        return ApiResult.success("状态更新成功", null);
    }

    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> stats(HttpServletRequest request) {
        Long uid = (Long) request.getAttribute("userId"); String role = (String) request.getAttribute("role");
        LambdaQueryWrapper<RepairOrder> base = new LambdaQueryWrapper<>();
        if ("user".equals(role)) base.eq(RepairOrder::getReporterId, uid);
        if ("maintainer".equals(role)) base.eq(RepairOrder::getAssignMaintainerId, uid);
        Map<String, Object> map = new HashMap<>();
        map.put("total", repairOrderMapper.selectCount(base));
        map.put("pending", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "待处理")));
        map.put("processing", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "处理中")));
        map.put("finished", repairOrderMapper.selectCount(base.clone().eq(RepairOrder::getStatus, "已完成")));
        return ApiResult.success(map);
    }
}

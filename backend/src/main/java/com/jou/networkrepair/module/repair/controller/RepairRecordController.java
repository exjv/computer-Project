package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.repair.dto.RepairRecordDTO;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.service.RepairRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/repair-records")
@RequiredArgsConstructor
public class RepairRecordController {
    private final RepairRecordService repairRecordService;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<RepairRecord>> page(@RequestParam Long current, @RequestParam Long size,
                                              @RequestParam(required = false) Long repairOrderId,
                                              @RequestParam(required = false) Long deviceId,
                                              @RequestParam(required = false) Long maintainerId,
                                              @RequestParam(required = false) Integer isResolved,
                                              HttpServletRequest request) {
        return ApiResult.success(repairRecordService.page(current, size, repairOrderId, deviceId, maintainerId, isResolved,
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<RepairRecord> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairRecordService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_RECORD_WRITE + "') || @permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "维修记录", operationType = "新增", operationDesc = "新增维修记录")
    public ApiResult<Void> add(@RequestBody @Validated RepairRecordDTO dto, HttpServletRequest request) {
        repairRecordService.create(dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_RECORD_WRITE + "') || @permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "维修记录", operationType = "修改", operationDesc = "修改维修记录")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated RepairRecordDTO dto, HttpServletRequest request) {
        repairRecordService.update(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "维修记录", operationType = "删除", operationDesc = "删除维修记录")
    public ApiResult<Void> delete(@PathVariable Long id) {
        repairRecordService.delete(id);
        return ApiResult.success("删除成功", null);
    }
}

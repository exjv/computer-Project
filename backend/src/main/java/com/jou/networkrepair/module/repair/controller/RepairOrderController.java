package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderActionDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAttachmentDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.system.entity.FileAttachment;
import com.jou.networkrepair.module.system.mapper.FileAttachmentMapper;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.repair.vo.AssignmentRecommendationVO;
import com.jou.networkrepair.module.repair.vo.DispatchResultVO;
import com.jou.networkrepair.module.repair.vo.RepairEstimateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {
    private final RepairOrderService repairOrderService;
    private final FileAttachmentMapper fileAttachmentMapper;

    @GetMapping("/page")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Page<RepairOrder>> page(@RequestParam Long current, @RequestParam Long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false, defaultValue = "id") String sortField,
                                             @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return ApiResult.success(repairOrderService.page(current, size, status, title, orderNo, priority, sortField, sortOrder));
    }

    @GetMapping("/my")
    public ApiResult<Page<RepairOrder>> my(@RequestParam Long current, @RequestParam Long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String orderNo,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false, defaultValue = "id") String sortField,
                                           @RequestParam(required = false, defaultValue = "desc") String sortOrder,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ApiResult.success(repairOrderService.myPage(current, size, status, orderNo, priority, userId, role, sortField, sortOrder));
    }

    @GetMapping("/{id}")
    public ApiResult<RepairOrder> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_CREATE + "')")
    @Loggable(module = "工单管理", operationType = "新增", operationDesc = "提交报修工单")
    public ApiResult<Void> add(@RequestBody @Validated RepairOrderCreateDTO dto, HttpServletRequest request) {
        repairOrderService.create(dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("提交成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_APPROVE + "')")
    @Loggable(module = "工单管理", operationType = "修改", operationDesc = "编辑工单")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairOrder req) {
        repairOrderService.update(id, req);
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "工单管理", operationType = "删除", operationDesc = "删除工单")
    public ApiResult<Void> delete(@PathVariable Long id) {
        repairOrderService.delete(id);
        return ApiResult.success("删除成功", null);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    @Loggable(module = "工单管理", operationType = "分配", operationDesc = "分配维修人员")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody @Validated RepairOrderAssignDTO dto, HttpServletRequest request) {
        repairOrderService.assign(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("分配成功", null);
    }

    @PutMapping("/{id}/action")
    public ApiResult<Void> action(@PathVariable Long id, @RequestBody @Validated RepairOrderActionDTO dto, HttpServletRequest request) {
        repairOrderService.action(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("操作成功", null);
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_APPROVE + "')")
    public ApiResult<Void> audit(@PathVariable Long id, @RequestBody @Validated RepairOrderAuditDTO dto, HttpServletRequest request) {
        repairOrderService.audit(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("审核处理成功", null);
    }

    @GetMapping("/{id}/flows")
    public ApiResult<List<RepairOrderFlow>> flows(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.flows(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}/business-logs")
    public ApiResult<List<BusinessLog>> businessLogs(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.businessLogs(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @PutMapping("/{id}/reassign")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    public ApiResult<Void> reassign(@PathVariable Long id, @RequestBody @Validated RepairOrderReassignDTO dto, HttpServletRequest request) {
        repairOrderService.reassign(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("改派成功", null);
    }

    @PutMapping("/{id}/delay-approve")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_APPROVE + "')")
    public ApiResult<Void> approveDelay(@PathVariable Long id, @RequestBody @Validated RepairOrderDelayApproveDTO dto, HttpServletRequest request) {
        repairOrderService.approveDelay(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("延期审批完成", null);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    public ApiResult<Void> close(@PathVariable Long id, @RequestBody @Validated RepairOrderCloseDTO dto, HttpServletRequest request) {
        repairOrderService.close(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("关闭处理成功", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_PROGRESS + "') || @permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "工单管理", operationType = "状态更新", operationDesc = "更新工单状态")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated RepairOrderStatusDTO dto, HttpServletRequest request) {
        repairOrderService.updateStatus(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("状态更新成功", null);
    }

    @GetMapping("/statistics")
    public ApiResult<Map<String, Object>> stats(HttpServletRequest request) {
        return ApiResult.success(repairOrderService.stats((Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/feedback/statistics")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Map<String, Object>> feedbackStats() {
        return ApiResult.success(repairOrderService.feedbackStats());
    }

    @GetMapping("/feedback/low-satisfaction")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Page<Map<String, Object>>> lowSatisfactionOrders(@RequestParam Long current,
                                                                      @RequestParam Long size,
                                                                      @RequestParam(required = false, defaultValue = "2") Integer threshold) {
        return ApiResult.success(repairOrderService.lowSatisfactionOrders(current, size, threshold));
    }

    @GetMapping("/feedback/unresolved")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Page<Map<String, Object>>> unresolvedFeedbackOrders(@RequestParam Long current,
                                                                         @RequestParam Long size) {
        return ApiResult.success(repairOrderService.unresolvedFeedbackOrders(current, size));
    }

    @GetMapping("/status-options")
    public ApiResult<List<String>> statusOptions() {
        return ApiResult.success(com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum.labels());
    }

    @PostMapping("/auto-dispatch")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    @Loggable(module = "工单管理", operationType = "自动分配", operationDesc = "执行工单自动分配算法")
    public ApiResult<Map<String, Object>> autoDispatch() {
        List<DispatchResultVO> assignedList = repairOrderService.autoDispatch();
        Map<String, Object> data = new HashMap<>();
        data.put("count", assignedList.size());
        data.put("assignedList", assignedList);
        return ApiResult.success("自动分配完成", data);
    }

    @GetMapping("/{id}/assign-recommendations")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    public ApiResult<List<AssignmentRecommendationVO>> recommendMaintainers(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.recommendMaintainers(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}/estimate-finish-time")
    public ApiResult<RepairEstimateVO> estimateFinishTime(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.estimateFinishTime(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}/attachments")
    public ApiResult<List<FileAttachment>> attachments(@PathVariable Long id, HttpServletRequest request) {
        repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success(fileAttachmentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileAttachment>()
                .eq(FileAttachment::getBusinessType, "REPAIR_ORDER")
                .eq(FileAttachment::getBusinessId, id)
                .orderByDesc(FileAttachment::getId)));
    }

    @PostMapping("/{id}/attachments")
    public ApiResult<Void> addAttachment(@PathVariable Long id, @RequestBody @Validated RepairOrderAttachmentDTO dto, HttpServletRequest request) {
        repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        FileAttachment attachment = new FileAttachment();
        attachment.setBusinessType("REPAIR_ORDER");
        attachment.setBusinessId(id);
        attachment.setFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileType(dto.getFileType());
        attachment.setUploaderId((Long) request.getAttribute("userId"));
        attachment.setUploadTime(java.time.LocalDateTime.now());
        attachment.setRemark(dto.getRemark());
        fileAttachmentMapper.insert(attachment);
        return ApiResult.success("上传记录成功", null);
    }
}

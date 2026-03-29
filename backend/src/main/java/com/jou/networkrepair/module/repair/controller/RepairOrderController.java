package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.repair.dto.RepairOrderAssignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAttachmentDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderAuditDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCloseDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderCreateDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderDelayApproveDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderFeedbackDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderReassignDTO;
import com.jou.networkrepair.module.repair.dto.RepairOrderStatusDTO;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairOrderFlow;
import com.jou.networkrepair.module.repair.enums.RepairOrderStatusEnum;
import com.jou.networkrepair.module.repair.service.RepairOrderService;
import com.jou.networkrepair.module.repair.vo.AssignmentRecommendationVO;
import com.jou.networkrepair.module.repair.vo.RepairEstimateVO;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.system.entity.FileAttachment;
import com.jou.networkrepair.module.system.entity.RepairFeedback;
import com.jou.networkrepair.module.system.mapper.BusinessLogMapper;
import com.jou.networkrepair.module.system.mapper.FileAttachmentMapper;
import com.jou.networkrepair.module.system.mapper.RepairFeedbackMapper;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {
    private final RepairOrderService repairOrderService;
    private final FileAttachmentMapper fileAttachmentMapper;
    private final BusinessLogMapper businessLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final RepairFeedbackMapper repairFeedbackMapper;
    private final RepairOrderMapper repairOrderMapper;

    @GetMapping("/page")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<RepairOrder>> page(@RequestParam Long current,
                                             @RequestParam Long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) String deviceType,
                                             @RequestParam(required = false) String faultType,
                                             @RequestParam(required = false) String reportTimeStart,
                                             @RequestParam(required = false) String reportTimeEnd,
                                             @RequestParam(required = false, defaultValue = "id") String sortField,
                                             @RequestParam(required = false, defaultValue = "desc") String sortOrder,
                                             HttpServletRequest request) {
        return ApiResult.success(repairOrderService.page(current, size, status, title, orderNo, priority, deviceType, faultType,
                parseDateTime(reportTimeStart), parseDateTime(reportTimeEnd), sortField, sortOrder,
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<RepairOrder>> my(@RequestParam Long current,
                                           @RequestParam Long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String orderNo,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false) String deviceType,
                                           @RequestParam(required = false) String faultType,
                                           @RequestParam(required = false) String reportTimeStart,
                                           @RequestParam(required = false) String reportTimeEnd,
                                           @RequestParam(required = false, defaultValue = "id") String sortField,
                                           @RequestParam(required = false, defaultValue = "desc") String sortOrder,
                                           HttpServletRequest request) {
        return ApiResult.success(repairOrderService.page(current, size, status, title, orderNo, priority, deviceType, faultType,
                parseDateTime(reportTimeStart), parseDateTime(reportTimeEnd), sortField, sortOrder,
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<RepairOrder> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @PostMapping
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_CREATE + "')")
    @Loggable(module = "工单管理", operationType = "新增", operationDesc = "提交工单")
    public ApiResult<Void> create(@RequestBody @Validated RepairOrderCreateDTO dto, HttpServletRequest request) {
        repairOrderService.create(dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("提交成功", null);
    }

    @PostMapping("/apply")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<Void> apply(@RequestBody @Validated RepairOrderCreateDTO dto, HttpServletRequest request) {
        repairOrderService.create(dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("报修申请提交成功", null);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RepairOrder req, HttpServletRequest request) {
        repairOrderService.update(id, req, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','USER')")
    public ApiResult<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        repairOrderService.delete(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("删除成功", null);
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<Void> cancel(@PathVariable Long id,
                                  @RequestBody(required = false) Map<String, String> body,
                                  HttpServletRequest request) {
        String remark = body == null ? null : body.get("remark");
        repairOrderService.cancelByUser(id, remark, (Long) request.getAttribute("userId"));
        return ApiResult.success("撤销成功", null);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    @Loggable(module = "工单管理", operationType = "分配", operationDesc = "分配维修人员")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody @Validated RepairOrderAssignDTO dto, HttpServletRequest request) {
        repairOrderService.assign(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("分配成功", null);
    }

    @GetMapping("/{id}/assign-recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<AssignmentRecommendationVO>> assignRecommendations(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.recommendAssignments(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}/estimate-finish-time")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<RepairEstimateVO> estimateFinishTime(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.estimateFinishTime(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> audit(@PathVariable Long id, @RequestBody @Validated RepairOrderAuditDTO dto, HttpServletRequest request) {
        repairOrderService.auditByAdmin(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("审核处理成功", null);
    }

    @PutMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> reassign(@PathVariable Long id, @RequestBody @Validated RepairOrderReassignDTO dto, HttpServletRequest request) {
        repairOrderService.reassignByAdmin(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("改派成功", null);
    }

    @PutMapping("/{id}/delay-approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> approveDelay(@PathVariable Long id, @RequestBody @Validated RepairOrderDelayApproveDTO dto, HttpServletRequest request) {
        repairOrderService.approveDelayByAdmin(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("延期审批完成", null);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<Void> close(@PathVariable Long id, @RequestBody @Validated RepairOrderCloseDTO dto, HttpServletRequest request) {
        repairOrderService.closeByAdmin(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("工单关闭成功", null);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    @Loggable(module = "工单流程", operationType = "状态更新", operationDesc = "更新工单状态")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated RepairOrderStatusDTO dto, HttpServletRequest request) {
        repairOrderService.updateStatus(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("状态更新成功", null);
    }

    @PutMapping("/{id}/feedback")
    @PreAuthorize("hasRole('USER')")
    public ApiResult<Void> feedback(@PathVariable Long id, @RequestBody @Validated RepairOrderFeedbackDTO dto, HttpServletRequest request) {
        repairOrderService.feedbackByUser(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("反馈提交成功", null);
    }

    @PutMapping("/{id}/maintainer/accept")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerAccept(@PathVariable Long id,
                                            @RequestBody(required = false) Map<String, String> body,
                                            HttpServletRequest request) {
        repairOrderService.maintainerAccept(id, body == null ? null : body.get("remark"),
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("接单成功", null);
    }

    @PutMapping("/{id}/maintainer/reject")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerReject(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            HttpServletRequest request) {
        repairOrderService.maintainerReject(id, body == null ? null : body.get("reason"),
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("拒单成功", null);
    }

    @PutMapping("/{id}/maintainer/start")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerStart(@PathVariable Long id,
                                           @RequestBody(required = false) Map<String, String> body,
                                           HttpServletRequest request) {
        repairOrderService.maintainerStart(id, body == null ? null : body.get("remark"),
                (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("开始维修成功", null);
    }

    @PutMapping("/{id}/maintainer/progress")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerProgress(@PathVariable Long id,
                                              @RequestBody @Validated RepairOrderStatusDTO dto,
                                              HttpServletRequest request) {
        repairOrderService.maintainerUpdateProgress(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("进度更新成功", null);
    }

    @PutMapping("/{id}/maintainer/delay-apply")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerDelayApply(@PathVariable Long id,
                                                @RequestBody @Validated RepairOrderStatusDTO dto,
                                                HttpServletRequest request) {
        repairOrderService.maintainerApplyDelay(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("延期申请提交成功", null);
    }

    @PutMapping("/{id}/maintainer/parts-apply")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerPartsApply(@PathVariable Long id,
                                                @RequestBody @Validated RepairOrderStatusDTO dto,
                                                HttpServletRequest request) {
        repairOrderService.maintainerApplyParts(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("配件申请提交成功", null);
    }

    @PutMapping("/{id}/maintainer/finish")
    @PreAuthorize("hasRole('MAINTAINER')")
    public ApiResult<Void> maintainerFinish(@PathVariable Long id,
                                            @RequestBody @Validated RepairOrderStatusDTO dto,
                                            HttpServletRequest request) {
        repairOrderService.maintainerFinish(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("完工提交成功", null);
    }

    @GetMapping("/{id}/flows")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<List<RepairOrderFlow>> flows(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.flows(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> records(@PathVariable Long id, HttpServletRequest request) {
        RepairOrder order = repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("flows", repairOrderService.flows(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
        map.put("businessLogs", businessLogMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<BusinessLog>()
                .and(w -> w.eq(BusinessLog::getBusinessType, "REPAIR_ORDER").or().eq(BusinessLog::getBizType, "REPAIR_ORDER"))
                .and(w -> w.eq(BusinessLog::getBusinessNo, order.getOrderNo()).or().eq(BusinessLog::getBizId, id))
                .orderByAsc(BusinessLog::getId)));
        map.put("operationLogs", operationLogMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<OperationLog>()
                .and(w -> w.like(OperationLog::getRequestUrl, "/repair-orders/" + id)
                        .or().like(OperationLog::getRequestParams, "\"id\":" + id))
                .orderByDesc(OperationLog::getOperationTime)));
        return ApiResult.success(map);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> stats(HttpServletRequest request) {
        return ApiResult.success(repairOrderService.stats((Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/feedback/low-score")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<RepairOrder>> lowScoreOrders(@RequestParam(required = false, defaultValue = "2") Integer maxScore) {
        List<RepairFeedback> feedbacks = repairFeedbackMapper.selectList(new LambdaQueryWrapper<RepairFeedback>()
                .le(RepairFeedback::getSatisfactionScore, maxScore)
                .orderByDesc(RepairFeedback::getConfirmTime)
                .last("limit 200"));
        List<Long> orderIds = feedbacks.stream().map(RepairFeedback::getRepairOrderId).distinct().collect(java.util.stream.Collectors.toList());
        if (orderIds.isEmpty()) return ApiResult.success(java.util.Collections.emptyList());
        return ApiResult.success(repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .in(RepairOrder::getId, orderIds)
                .orderByDesc(RepairOrder::getUpdateTime)));
    }

    @GetMapping("/feedback/unresolved-rework")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<RepairOrder>> unresolvedReworkOrders() {
        return ApiResult.success(repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .eq(RepairOrder::getUserConfirmResult, "未解决")
                .in(RepairOrder::getStatus, java.util.Arrays.asList("维修中", "申请延期中", "延期已批准", "待采购/待配件"))
                .orderByDesc(RepairOrder::getUpdateTime)
                .last("limit 200")));
    }

    @GetMapping("/status-options")
    public ApiResult<List<String>> statusOptions() {
        return ApiResult.success(RepairOrderStatusEnum.labels());
    }

    @GetMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<List<FileAttachment>> attachments(@PathVariable Long id, HttpServletRequest request) {
        repairOrderService.detail(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success(fileAttachmentMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileAttachment>()
                .and(w -> w.eq(FileAttachment::getBusinessType, "REPAIR_ORDER").or().eq(FileAttachment::getBizType, "REPAIR_ORDER"))
                .and(w -> w.eq(FileAttachment::getBusinessId, id).or().eq(FileAttachment::getBizId, id))
                .orderByDesc(FileAttachment::getId)));
    }

    @PostMapping("/{id}/attachments")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Void> addAttachment(@PathVariable Long id, @RequestBody @Validated RepairOrderAttachmentDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        repairOrderService.detail(id, userId, (String) request.getAttribute("role"));
        FileAttachment attachment = new FileAttachment();
        attachment.setBusinessType("REPAIR_ORDER");
        attachment.setBusinessId(id);
        attachment.setBizType("REPAIR_ORDER");
        attachment.setBizId(id);
        attachment.setFileName(dto.getFileName());
        attachment.setFileUrl(dto.getFileUrl());
        attachment.setFileType(dto.getFileType());
        attachment.setUploaderId(userId);
        attachment.setUploadTime(LocalDateTime.now());
        attachment.setRemark(dto.getRemark());
        attachment.setCreateTime(LocalDateTime.now());
        attachment.setUpdateTime(LocalDateTime.now());
        fileAttachmentMapper.insert(attachment);
        return ApiResult.success("上传记录成功", null);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception ignore) {
            return null;
        }
    }
}

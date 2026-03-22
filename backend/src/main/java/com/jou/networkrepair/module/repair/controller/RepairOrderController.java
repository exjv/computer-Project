package com.jou.networkrepair.module.repair.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
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
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repair-orders")
@RequiredArgsConstructor
public class RepairOrderController {
    private final RepairOrderService repairOrderService;
    private final OperationLogMapper operationLogMapper;

    @GetMapping("/page")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Page<RepairOrder>> page(@RequestParam Long current, @RequestParam Long size,
                                             @RequestParam(required = false) String status,
                                             @RequestParam(required = false) String title,
                                             @RequestParam(required = false) String orderNo,
                                             @RequestParam(required = false) String priority,
                                             @RequestParam(required = false) String deviceType,
                                             @RequestParam(required = false) String faultType,
                                             @RequestParam(required = false) Long assignMaintainerId,
                                             @RequestParam(required = false) Integer applyDelay,
                                             @RequestParam(required = false) Integer needPurchaseParts,
                                             @RequestParam(required = false) String reportTimeStart,
                                             @RequestParam(required = false) String reportTimeEnd,
                                             @RequestParam(required = false, defaultValue = "id") String sortField,
                                             @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return ApiResult.success(repairOrderService.page(current, size, status, title, orderNo, priority, deviceType, faultType,
                assignMaintainerId, applyDelay, needPurchaseParts,
                parseDateTime(reportTimeStart), parseDateTime(reportTimeEnd), sortField, sortOrder));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Page<RepairOrder>> my(@RequestParam Long current, @RequestParam Long size,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String orderNo,
                                           @RequestParam(required = false) String priority,
                                           @RequestParam(required = false) String deviceType,
                                           @RequestParam(required = false) String faultType,
                                           @RequestParam(required = false) Integer applyDelay,
                                           @RequestParam(required = false) Integer needPurchaseParts,
                                           @RequestParam(required = false) String reportTimeStart,
                                           @RequestParam(required = false) String reportTimeEnd,
                                           @RequestParam(required = false, defaultValue = "id") String sortField,
                                           @RequestParam(required = false, defaultValue = "desc") String sortOrder,
                                           HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        return ApiResult.success(repairOrderService.myPage(current, size, status, orderNo, priority, deviceType, faultType, applyDelay, needPurchaseParts,
                parseDateTime(reportTimeStart), parseDateTime(reportTimeEnd), userId, role, sortField, sortOrder));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
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


    @GetMapping("/{id}/recommend-maintainers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResult<List<com.jou.networkrepair.module.repair.vo.MaintainerRecommendVO>> recommendMaintainers(@PathVariable Long id) {
        return ApiResult.success(repairOrderService.recommendMaintainers(id));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_ASSIGN + "')")
    @Loggable(module = "工单管理", operationType = "分配", operationDesc = "分配维修人员")
    public ApiResult<Void> assign(@PathVariable Long id, @RequestBody @Validated RepairOrderAssignDTO dto, HttpServletRequest request) {
        repairOrderService.assign(id, dto, (Long) request.getAttribute("userId"));
        return ApiResult.success("分配成功", null);
    }

    @PutMapping("/{id}/action")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    @Loggable(module = "工单流程", operationType = "流转", operationDesc = "执行工单流转动作")
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
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<List<RepairOrderFlow>> flows(@PathVariable Long id, HttpServletRequest request) {
        return ApiResult.success(repairOrderService.flows(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }


    @GetMapping("/{id}/records")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> records(@PathVariable Long id, HttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();
        map.put("flows", repairOrderService.flows(id, (Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
        map.put("logs", operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .like(OperationLog::getRequestUrl, "/repair-orders/" + id)
                .orderByDesc(OperationLog::getOperationTime)));
        return ApiResult.success(map);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_PROGRESS + "') || @permissionService.hasPermission('" + PermissionCode.REPAIR_SUPERVISE + "')")
    @Loggable(module = "工单管理", operationType = "状态更新", operationDesc = "更新工单状态")
    public ApiResult<Void> updateStatus(@PathVariable Long id, @RequestBody @Validated RepairOrderStatusDTO dto, HttpServletRequest request) {
        repairOrderService.updateStatus(id, dto, (Long) request.getAttribute("userId"), (String) request.getAttribute("role"));
        return ApiResult.success("状态更新成功", null);
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER','USER')")
    public ApiResult<Map<String, Object>> stats(HttpServletRequest request) {
        return ApiResult.success(repairOrderService.stats((Long) request.getAttribute("userId"), (String) request.getAttribute("role")));
    }

    @GetMapping("/analytics")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    public ApiResult<Map<String, Object>> analytics(@RequestParam(required = false, defaultValue = "month") String rangeType,
                                                    @RequestParam(required = false) String start,
                                                    @RequestParam(required = false) String end) {
        return ApiResult.success(repairOrderService.analytics(rangeType, start, end));
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

    @GetMapping("/exports/statistics-excel")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    @Loggable(module = "工单管理", operationType = "导出", operationDesc = "导出工单统计报表")
    public void exportStatisticsExcel(@RequestParam(required = false, defaultValue = "month") String rangeType,
                                      @RequestParam(required = false) String start,
                                      @RequestParam(required = false) String end,
                                      HttpServletResponse response) throws Exception {
        Map<String, Object> analytics = repairOrderService.analytics(rangeType, start, end);
        Workbook wb = new XSSFWorkbook();
        Sheet summary = wb.createSheet("工单统计汇总");
        int idx = 0;
        idx = writeRow(summary, idx, "统计维度", String.valueOf(analytics.get("rangeType")));
        idx = writeRow(summary, idx, "统计开始", String.valueOf(analytics.get("rangeStart")));
        idx = writeRow(summary, idx, "统计结束", String.valueOf(analytics.get("rangeEnd")));
        idx = writeRow(summary, idx, "报修数量", String.valueOf(analytics.get("repairCount")));
        idx = writeRow(summary, idx, "已完成工单", String.valueOf(analytics.get("finishedCount")));
        idx = writeRow(summary, idx, "未完成工单", String.valueOf(analytics.get("unfinishedCount")));
        idx = writeRow(summary, idx, "平均维修时长(小时)", String.valueOf(analytics.get("avgRepairHours")));
        idx = writeRow(summary, idx, "延期工单占比(%)", String.valueOf(analytics.get("delayOrderRatio")));
        idx = writeRow(summary, idx, "配件采购工单占比(%)", String.valueOf(analytics.get("partsPurchaseRatio")));

        Sheet trend = wb.createSheet("报修趋势");
        writeHeader(trend, "时间桶", "报修数量", "已完成数量");
        List<Map<String, Object>> trendList = (List<Map<String, Object>>) analytics.get("timeTrend");
        if (trendList != null) {
            int rowNo = 1;
            for (Map<String, Object> v : trendList) {
                Row row = trend.createRow(rowNo++);
                row.createCell(0).setCellValue(String.valueOf(v.get("bucket")));
                row.createCell(1).setCellValue(String.valueOf(v.get("reportCount")));
                row.createCell(2).setCellValue(String.valueOf(v.get("finishedCount")));
            }
        }
        writeWorkbook(response, wb, "order_statistics_report.xlsx");
    }

    @GetMapping("/exports/records-excel")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPAIR_ORDER_VIEW_ALL + "')")
    @Loggable(module = "维修记录", operationType = "导出", operationDesc = "按设备导出维修记录报表")
    public void exportRecordsExcel(@RequestParam(required = false) Long deviceId,
                                   @RequestParam(required = false) String start,
                                   @RequestParam(required = false) String end,
                                   HttpServletResponse response) throws Exception {
        LocalDateTime startTime = parseDateTime(start);
        LocalDateTime endTime = parseDateTime(end);
        List<RepairRecord> records = repairRecordMapper.selectList(new LambdaQueryWrapper<RepairRecord>()
                .eq(deviceId != null, RepairRecord::getDeviceId, deviceId)
                .ge(startTime != null, RepairRecord::getReportTime, startTime)
                .le(endTime != null, RepairRecord::getReportTime, endTime)
                .orderByDesc(RepairRecord::getId));
        Workbook wb = new XSSFWorkbook();
        Sheet sheet = wb.createSheet("维修记录");
        writeHeader(sheet, "工单编号", "设备编号", "第几次报修", "第几次维修", "故障原因", "处理措施", "处理结果", "是否解决", "维修人员", "报修时间", "完成时间", "用户满意度", "反馈结论", "备注");
        int rowNo = 1;
        for (RepairRecord r : records) {
            Row row = sheet.createRow(rowNo++);
            row.createCell(0).setCellValue(str(r.getRepairOrderNo()));
            row.createCell(1).setCellValue(str(r.getDeviceCode()));
            row.createCell(2).setCellValue(r.getRepairSequence() == null ? "" : r.getRepairSequence());
            row.createCell(3).setCellValue(r.getMaintenanceSequence() == null ? "" : r.getMaintenanceSequence());
            row.createCell(4).setCellValue(str(r.getFaultReason()));
            row.createCell(5).setCellValue(str(r.getFixMeasure()));
            row.createCell(6).setCellValue(str(r.getResultDetail()));
            row.createCell(7).setCellValue(r.getIsResolved() != null && r.getIsResolved() == 1 ? "是" : "否");
            row.createCell(8).setCellValue(str(r.getMaintainerName()));
            row.createCell(9).setCellValue(str(r.getReportTime()));
            row.createCell(10).setCellValue(str(r.getFinishTime()));
            row.createCell(11).setCellValue(r.getUserSatisfaction() == null ? "" : r.getUserSatisfaction());
            row.createCell(12).setCellValue(str(r.getRepairConclusion()));
            row.createCell(13).setCellValue(str(r.getRemark()));
        }
        writeWorkbook(response, wb, "repair_records_report.xlsx");
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception ignore) {
            return null;
        }
    }

    private int writeRow(Sheet sheet, int idx, String key, String val) {
        Row row = sheet.createRow(idx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(val == null ? "" : val);
        return idx + 1;
    }

    private void writeHeader(Sheet sheet, String... headers) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) row.createCell(i).setCellValue(headers[i]);
    }

    private void writeWorkbook(HttpServletResponse response, Workbook wb, String fileName) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        wb.write(bos);
        wb.close();
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
        response.getOutputStream().write(bos.toByteArray());
        response.flushBuffer();
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}

package com.jou.networkrepair.module.report.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.device.entity.NetworkDevice;
import com.jou.networkrepair.module.device.mapper.DeviceMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.entity.RepairRecord;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.repair.mapper.RepairRecordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportExportController {
    private final RepairOrderMapper repairOrderMapper;
    private final RepairRecordMapper repairRecordMapper;
    private final DeviceMapper deviceMapper;

    @GetMapping("/repair-orders/export/excel")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPORT_EXPORT + "')")
    @Loggable(module = "报表中心", operationType = "导出", operationDesc = "导出工单统计报表(Excel)")
    public void exportRepairOrdersExcel(@RequestParam(required = false) String startTime,
                                        @RequestParam(required = false) String endTime,
                                        @RequestParam(required = false) Long deviceId,
                                        HttpServletResponse response) throws IOException {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);

        LambdaQueryWrapper<RepairOrder> qw = new LambdaQueryWrapper<RepairOrder>()
                .ge(start != null, RepairOrder::getReportTime, start)
                .le(end != null, RepairOrder::getReportTime, end)
                .orderByDesc(RepairOrder::getReportTime);
        if (deviceId != null) {
            NetworkDevice device = deviceMapper.selectById(deviceId);
            if (device != null) {
                qw.eq(RepairOrder::getDeviceId, deviceId);
            }
        }
        List<RepairOrder> orders = repairOrderMapper.selectList(qw);

        Map<String, Object> analytics = buildOrderSummary(orders);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet summarySheet = workbook.createSheet("工单统计摘要");
            fillOrderSummarySheet(summarySheet, analytics, start, end, deviceId);

            XSSFSheet detailSheet = workbook.createSheet("工单明细");
            fillOrderDetailSheet(detailSheet, orders);

            writeWorkbook(response, workbook, "工单统计报表.xlsx");
        }
    }

    @GetMapping("/repair-records/export/excel")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.REPORT_EXPORT + "')")
    @Loggable(module = "报表中心", operationType = "导出", operationDesc = "导出设备维修记录报表(Excel)")
    public void exportRepairRecordsExcel(@RequestParam(required = false) Long deviceId,
                                         @RequestParam(required = false) String startTime,
                                         @RequestParam(required = false) String endTime,
                                         HttpServletResponse response) throws IOException {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);

        LambdaQueryWrapper<RepairRecord> qw = new LambdaQueryWrapper<RepairRecord>()
                .eq(deviceId != null, RepairRecord::getDeviceId, deviceId)
                .ge(start != null, RepairRecord::getRepairTime, start)
                .le(end != null, RepairRecord::getRepairTime, end)
                .orderByDesc(RepairRecord::getRepairTime);
        List<RepairRecord> records = repairRecordMapper.selectList(qw);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("设备维修记录");
            fillRepairRecordSheet(sheet, records, deviceId, start, end);
            writeWorkbook(response, workbook, "设备维修记录报表.xlsx");
        }
    }

    private void fillOrderSummarySheet(XSSFSheet sheet, Map<String, Object> analytics,
                                       LocalDateTime start, LocalDateTime end, Long deviceId) {
        int rowIdx = 0;
        rowIdx = addSimpleRow(sheet, rowIdx, "统计开始时间", formatDateTime(start));
        rowIdx = addSimpleRow(sheet, rowIdx, "统计结束时间", formatDateTime(end));
        rowIdx = addSimpleRow(sheet, rowIdx, "设备筛选", String.valueOf(deviceId == null ? "全部设备" : deviceId));
        rowIdx++;

        rowIdx = addSimpleRow(sheet, rowIdx, "报修数量", String.valueOf(analytics.getOrDefault("repairCount", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "已完成工单数量", String.valueOf(analytics.getOrDefault("completedCount", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "未完成工单数量", String.valueOf(analytics.getOrDefault("uncompletedCount", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "平均维修时长(h)", String.valueOf(analytics.getOrDefault("avgRepairHours", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "用户满意度均分", String.valueOf(analytics.getOrDefault("satisfactionAvg", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "延期工单占比(%)", String.valueOf(analytics.getOrDefault("delayRatio", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "配件采购工单占比(%)", String.valueOf(analytics.getOrDefault("partsRatio", 0)));
        rowIdx = addSimpleRow(sheet, rowIdx, "预测可对比样本数", String.valueOf(analytics.getOrDefault("predictionComparableCount", 0)));

        autoSize(sheet, 2);
    }

    private Map<String, Object> buildOrderSummary(List<RepairOrder> orders) {
        long total = orders.size();
        long completed = orders.stream()
                .filter(o -> "已完成".equals(o.getStatus()) || "已关闭".equals(o.getStatus()))
                .count();
        long uncompleted = total - completed;
        double avgRepairHours = orders.stream()
                .filter(o -> o.getStartRepairTime() != null && o.getFinishTime() != null && !o.getFinishTime().isBefore(o.getStartRepairTime()))
                .mapToDouble(o -> java.time.Duration.between(o.getStartRepairTime(), o.getFinishTime()).toMinutes() / 60.0)
                .average().orElse(0D);
        long delayCount = orders.stream().filter(o -> o.getApplyDelay() != null && o.getApplyDelay() == 1).count();
        long partsCount = orders.stream().filter(o -> o.getNeedPurchaseParts() != null && o.getNeedPurchaseParts() == 1).count();
        double satisfactionAvg = orders.stream().filter(o -> o.getSatisfactionScore() != null)
                .mapToInt(RepairOrder::getSatisfactionScore)
                .average().orElse(0D);
        long comparableCount = orders.stream().filter(o -> o.getExpectedFinishTime() != null && o.getFinishTime() != null).count();
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("repairCount", total);
        map.put("completedCount", completed);
        map.put("uncompletedCount", uncompleted);
        map.put("avgRepairHours", round(avgRepairHours));
        map.put("satisfactionAvg", round(satisfactionAvg));
        map.put("delayRatio", total == 0 ? 0D : round(delayCount * 100.0 / total));
        map.put("partsRatio", total == 0 ? 0D : round(partsCount * 100.0 / total));
        map.put("predictionComparableCount", comparableCount);
        return map;
    }

    private void fillOrderDetailSheet(XSSFSheet sheet, List<RepairOrder> orders) {
        String[] headers = new String[]{"工单编号", "报修用户", "报修人工号", "设备编号", "设备名称", "设备类型", "故障类型", "紧急程度", "当前状态", "当前进度(%)", "报修时间", "分配维修人员", "实际完成时间", "用户确认结果", "满意度评分"};
        Row head = sheet.createRow(0);
        CellStyle headStyle = sheet.getWorkbook().createCellStyle();
        headStyle.setAlignment(HorizontalAlignment.CENTER);
        for (int i = 0; i < headers.length; i++) {
            Cell c = head.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headStyle);
        }

        int rowIdx = 1;
        for (RepairOrder o : orders) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(o.getOrderNo()));
            row.createCell(1).setCellValue(nullToEmpty(o.getReporterName()));
            row.createCell(2).setCellValue(nullToEmpty(o.getReporterEmployeeNo()));
            row.createCell(3).setCellValue(nullToEmpty(o.getDeviceCode()));
            row.createCell(4).setCellValue(nullToEmpty(o.getDeviceName()));
            row.createCell(5).setCellValue(nullToEmpty(o.getDeviceType()));
            row.createCell(6).setCellValue(nullToEmpty(o.getFaultType()));
            row.createCell(7).setCellValue(nullToEmpty(o.getPriority()));
            row.createCell(8).setCellValue(nullToEmpty(o.getStatus()));
            row.createCell(9).setCellValue(o.getProgress() == null ? 0 : o.getProgress());
            row.createCell(10).setCellValue(formatDateTime(o.getReportTime()));
            row.createCell(11).setCellValue(nullToEmpty(o.getAssignMaintainerName()));
            row.createCell(12).setCellValue(formatDateTime(o.getFinishTime()));
            row.createCell(13).setCellValue(nullToEmpty(o.getUserConfirmResult()));
            row.createCell(14).setCellValue(o.getSatisfactionScore() == null ? "" : String.valueOf(o.getSatisfactionScore()));
        }
        autoSize(sheet, headers.length);
    }

    private void fillRepairRecordSheet(XSSFSheet sheet, List<RepairRecord> records,
                                       Long deviceId, LocalDateTime start, LocalDateTime end) {
        int rowIdx = 0;
        rowIdx = addSimpleRow(sheet, rowIdx, "设备筛选", String.valueOf(deviceId == null ? "全部设备" : deviceId));
        rowIdx = addSimpleRow(sheet, rowIdx, "开始时间", formatDateTime(start));
        rowIdx = addSimpleRow(sheet, rowIdx, "结束时间", formatDateTime(end));
        rowIdx++;

        String[] headers = new String[]{"设备编号", "工单编号", "第几次报修", "第几次维修", "维修人员", "报修时间", "接单时间", "开始维修时间", "完成时间", "故障原因", "维修处理措施", "是否更换配件", "配件信息", "是否延期", "延期原因", "实际工期(h)", "维修结果", "用户确认结果", "用户满意度", "维修照片", "备注"};
        Row head = sheet.createRow(rowIdx++);
        for (int i = 0; i < headers.length; i++) {
            head.createCell(i).setCellValue(headers[i]);
        }

        for (RepairRecord r : records) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(nullToEmpty(r.getDeviceCode()));
            row.createCell(1).setCellValue(nullToEmpty(r.getRepairOrderNo()));
            row.createCell(2).setCellValue(r.getRepairSequence() == null ? "" : String.valueOf(r.getRepairSequence()));
            row.createCell(3).setCellValue(r.getMaintenanceSequence() == null ? "" : String.valueOf(r.getMaintenanceSequence()));
            row.createCell(4).setCellValue(nullToEmpty(r.getMaintainerName()));
            row.createCell(5).setCellValue(formatDateTime(r.getReportTime()));
            row.createCell(6).setCellValue(formatDateTime(r.getAcceptTime()));
            row.createCell(7).setCellValue(formatDateTime(r.getStartRepairTime()));
            row.createCell(8).setCellValue(formatDateTime(r.getFinishTime()));
            row.createCell(9).setCellValue(nullToEmpty(r.getFaultReason()));
            row.createCell(10).setCellValue(nullToEmpty(r.getFixMeasure()));
            row.createCell(11).setCellValue(r.getUsedParts() != null && r.getUsedParts() == 1 ? "是" : "否");
            row.createCell(12).setCellValue(nullToEmpty(r.getUsedPartsDesc()));
            row.createCell(13).setCellValue(r.getDelayApplied() != null && r.getDelayApplied() == 1 ? "是" : "否");
            row.createCell(14).setCellValue(nullToEmpty(r.getDelayReason()));
            row.createCell(15).setCellValue(r.getLaborHours() == null ? "" : String.valueOf(r.getLaborHours()));
            row.createCell(16).setCellValue(nullToEmpty(r.getRepairConclusion()));
            row.createCell(17).setCellValue(nullToEmpty(r.getUserConfirmResult()));
            row.createCell(18).setCellValue(r.getUserSatisfaction() == null ? "" : String.valueOf(r.getUserSatisfaction()));
            row.createCell(19).setCellValue(nullToEmpty(r.getPhotoUrls()));
            row.createCell(20).setCellValue(nullToEmpty(r.getRemark()));
        }
        autoSize(sheet, headers.length);
    }

    private int addSimpleRow(XSSFSheet sheet, int rowIdx, String key, String value) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value == null ? "" : value);
        return rowIdx + 1;
    }

    private void autoSize(XSSFSheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 1024, 256 * 50));
        }
    }

    private void writeWorkbook(HttpServletResponse response, XSSFWorkbook workbook, String filename) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encoded);
        workbook.write(response.getOutputStream());
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (Exception ignore) {
            return null;
        }
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) return "";
        return value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

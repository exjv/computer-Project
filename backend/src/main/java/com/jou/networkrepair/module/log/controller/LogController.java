package com.jou.networkrepair.module.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.log.entity.LoginLog;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.LoginLogMapper;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import com.jou.networkrepair.module.repair.entity.RepairOrder;
import com.jou.networkrepair.module.repair.mapper.RepairOrderMapper;
import com.jou.networkrepair.module.system.entity.BusinessLog;
import com.jou.networkrepair.module.system.mapper.BusinessLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasPermission('" + PermissionCode.LOG_OPERATION_VIEW + "')")
public class LogController {
    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final BusinessLogMapper businessLogMapper;
    private final RepairOrderMapper repairOrderMapper;

    @GetMapping("/audit/page")
    public ApiResult<Page<Map<String, Object>>> auditPage(@RequestParam Long current, @RequestParam Long size,
                                                          @RequestParam(required = false) String username,
                                                          @RequestParam(required = false) String employeeNo,
                                                          @RequestParam(required = false) String operationType,
                                                          @RequestParam(required = false) String loginStatus,
                                                          @RequestParam(required = false) String startTime,
                                                          @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseTime(startTime);
        LocalDateTime et = parseTime(endTime);
        List<Map<String, Object>> rows = new ArrayList<>();

        List<LoginLog> loginLogs = loginLogMapper.selectList(new LambdaQueryWrapper<LoginLog>()
                .like(username != null && !username.trim().isEmpty(), LoginLog::getUsername, username)
                .eq(loginStatus != null && !loginStatus.trim().isEmpty(), LoginLog::getLoginStatus, loginStatus)
                .ge(st != null, LoginLog::getLoginTime, st)
                .le(et != null, LoginLog::getLoginTime, et)
                .orderByDesc(LoginLog::getLoginTime));
        for (LoginLog l : loginLogs) {
            Map<String, Object> m = new HashMap<>();
            m.put("category", "LOGIN");
            m.put("id", l.getId());
            m.put("username", l.getUsername());
            m.put("employeeNo", "");
            m.put("operationType", l.getLoginStatus());
            m.put("time", l.getLoginTime());
            m.put("friendlyText", buildLoginFriendlyText(l));
            rows.add(m);
        }

        List<OperationLog> opLogs = operationLogMapper.selectList(new LambdaQueryWrapper<OperationLog>()
                .like(username != null && !username.trim().isEmpty(), OperationLog::getUsername, username)
                .like(operationType != null && !operationType.trim().isEmpty(), OperationLog::getOperationType, operationType)
                .ge(st != null, OperationLog::getOperationTime, st)
                .le(et != null, OperationLog::getOperationTime, et)
                .orderByDesc(OperationLog::getOperationTime));
        for (OperationLog o : opLogs) {
            if (employeeNo != null && !employeeNo.trim().isEmpty() && (o.getUsername() == null || !o.getUsername().contains(employeeNo))) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("category", "AUDIT");
            m.put("id", o.getId());
            m.put("username", o.getUsername());
            m.put("employeeNo", "");
            m.put("operationType", o.getOperationType());
            m.put("time", o.getOperationTime());
            m.put("friendlyText", buildOperationFriendlyText(o));
            rows.add(m);
        }

        rows.sort((a, b) -> String.valueOf(b.get("time")).compareTo(String.valueOf(a.get("time"))));
        Page<Map<String, Object>> page = subPage(rows, current, size);
        return ApiResult.success(page);
    }

    @GetMapping("/audit/{category}/{id}")
    public ApiResult<Map<String, Object>> auditDetail(@PathVariable String category, @PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        if ("LOGIN".equalsIgnoreCase(category)) {
            LoginLog log = loginLogMapper.selectById(id);
            if (log == null) return ApiResult.success(result);
            result.put("category", "LOGIN");
            result.put("friendlyText", buildLoginFriendlyText(log));
            result.put("raw", log);
            return ApiResult.success(result);
        }
        OperationLog log = operationLogMapper.selectById(id);
        if (log == null) return ApiResult.success(result);
        result.put("category", "AUDIT");
        result.put("friendlyText", buildOperationFriendlyText(log));
        result.put("raw", log);
        return ApiResult.success(result);
    }

    @GetMapping("/business/page")
    public ApiResult<Page<Map<String, Object>>> businessPage(@RequestParam Long current, @RequestParam Long size,
                                                             @RequestParam(required = false) String operatorName,
                                                             @RequestParam(required = false) String operatorEmployeeNo,
                                                             @RequestParam(required = false) String orderNo,
                                                             @RequestParam(required = false) String deviceCode,
                                                             @RequestParam(required = false) String action,
                                                             @RequestParam(required = false) String startTime,
                                                             @RequestParam(required = false) String endTime) {
        LocalDateTime st = parseTime(startTime);
        LocalDateTime et = parseTime(endTime);
        List<BusinessLog> logs = businessLogMapper.selectList(new LambdaQueryWrapper<BusinessLog>()
                .like(operatorName != null && !operatorName.trim().isEmpty(), BusinessLog::getOperatorName, operatorName)
                .like(operatorEmployeeNo != null && !operatorEmployeeNo.trim().isEmpty(), BusinessLog::getOperatorEmployeeNo, operatorEmployeeNo)
                .like(orderNo != null && !orderNo.trim().isEmpty(), BusinessLog::getBusinessNo, orderNo)
                .like(action != null && !action.trim().isEmpty(), BusinessLog::getAction, action)
                .ge(st != null, BusinessLog::getCreateTime, st)
                .le(et != null, BusinessLog::getCreateTime, et)
                .orderByDesc(BusinessLog::getCreateTime));
        Map<String, RepairOrder> orderMap = repairOrderMapper.selectList(new LambdaQueryWrapper<RepairOrder>()
                .in(!logs.isEmpty(), RepairOrder::getOrderNo, logs.stream().map(BusinessLog::getBusinessNo).collect(Collectors.toSet())))
                .stream().collect(Collectors.toMap(RepairOrder::getOrderNo, v -> v, (a, b) -> a));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BusinessLog b : logs) {
            RepairOrder order = orderMap.get(b.getBusinessNo());
            if (deviceCode != null && !deviceCode.trim().isEmpty() && (order == null || order.getDeviceCode() == null || !order.getDeviceCode().contains(deviceCode))) continue;
            Map<String, Object> row = new HashMap<>();
            row.put("id", b.getId());
            row.put("orderNo", b.getBusinessNo());
            row.put("deviceCode", order == null ? null : order.getDeviceCode());
            row.put("operatorName", b.getOperatorName());
            row.put("operatorEmployeeNo", b.getOperatorEmployeeNo());
            row.put("action", b.getAction());
            row.put("time", b.getCreateTime());
            row.put("friendlyText", buildBusinessFriendlyText(b, order));
            row.put("raw", b);
            rows.add(row);
        }
        Page<Map<String, Object>> page = subPage(rows, current, size);
        return ApiResult.success(page);
    }

    @GetMapping("/business/{id}")
    public ApiResult<Map<String, Object>> businessDetail(@PathVariable Long id) {
        BusinessLog b = businessLogMapper.selectById(id);
        if (b == null) return ApiResult.success(new HashMap<>());
        RepairOrder order = repairOrderMapper.selectOne(new LambdaQueryWrapper<RepairOrder>().eq(RepairOrder::getOrderNo, b.getBusinessNo()));
        Map<String, Object> res = new HashMap<>();
        res.put("friendlyText", buildBusinessFriendlyText(b, order));
        res.put("raw", b);
        res.put("order", order);
        return ApiResult.success(res);
    }

    private Page<Map<String, Object>> subPage(List<Map<String, Object>> rows, Long current, Long size) {
        long c = current == null || current < 1 ? 1 : current;
        long s = size == null || size < 1 ? 10 : size;
        int from = (int) ((c - 1) * s);
        int to = Math.min(rows.size(), from + (int) s);
        Page<Map<String, Object>> page = new Page<>(c, s, rows.size());
        if (from >= rows.size()) {
            page.setRecords(Collections.emptyList());
        } else {
            page.setRecords(rows.subList(from, to));
        }
        return page;
    }

    private LocalDateTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(text.trim().replace(" ", "T"));
        } catch (Exception ignore) {
            return null;
        }
    }

    private String buildLoginFriendlyText(LoginLog l) {
        String reason = (l.getFailReason() == null || l.getFailReason().trim().isEmpty()) ? "-" : l.getFailReason();
        if ("SUCCESS".equalsIgnoreCase(l.getLoginStatus())) {
            return String.format("用户 %s 于 %s 登录成功（IP：%s）", safe(l.getUsername()), String.valueOf(l.getLoginTime()), safe(l.getIp()));
        }
        return String.format("用户 %s 于 %s 登录失败，原因：%s（IP：%s）", safe(l.getUsername()), String.valueOf(l.getLoginTime()), reason, safe(l.getIp()));
    }

    private String buildOperationFriendlyText(OperationLog o) {
        return String.format("%s 于 %s 在【%s】执行了【%s】：%s",
                safe(o.getUsername()), String.valueOf(o.getOperationTime()), safe(o.getModule()), safe(o.getOperationType()), safe(o.getOperationDesc()));
    }

    private String buildBusinessFriendlyText(BusinessLog b, RepairOrder order) {
        String device = order == null ? "-" : safe(order.getDeviceCode());
        return String.format("%s（工号%s）于 %s 处理工单%s（设备%s）：%s",
                safe(b.getOperatorName()), safe(b.getOperatorEmployeeNo()), String.valueOf(b.getCreateTime()),
                safe(b.getBusinessNo()), device, safe(b.getContent()));
    }

    private String safe(String text) {
        return text == null ? "-" : text;
    }
}

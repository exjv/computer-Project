package com.jou.networkrepair.module.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.log.entity.BusinessLog;
import com.jou.networkrepair.module.log.entity.LoginLog;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.BusinessLogMapper;
import com.jou.networkrepair.module.log.mapper.LoginLogMapper;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.exception.BusinessException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;
    private final BusinessLogMapper businessLogMapper;

    @GetMapping("/operation/page")
    public ApiResult<Page<OperationLog>> opPage(@RequestParam Long current, @RequestParam Long size) {
        return ApiResult.success(operationLogMapper.selectPage(new Page<>(current, size), null));
    }

    @GetMapping("/login/page")
    public ApiResult<Page<LoginLog>> loginPage(@RequestParam Long current, @RequestParam Long size) {
        return ApiResult.success(loginLogMapper.selectPage(new Page<>(current, size), null));
    }

    @GetMapping("/business/page")
    public ApiResult<Page<BusinessLog>> businessPage(@RequestParam Long current, @RequestParam Long size,
                                                     @RequestParam(required = false) String employeeNo,
                                                     @RequestParam(required = false) String username,
                                                     @RequestParam(required = false) String orderNo,
                                                     @RequestParam(required = false) String actionType,
                                                     @RequestParam(required = false) String dateFrom,
                                                     @RequestParam(required = false) String dateTo) {
        LocalDateTime from = parseDateTime(dateFrom, "dateFrom");
        LocalDateTime to = parseDateTime(dateTo, "dateTo");
        LambdaQueryWrapper<BusinessLog> qw = new LambdaQueryWrapper<BusinessLog>()
                .like(employeeNo != null && !employeeNo.isEmpty(), BusinessLog::getEmployeeNo, employeeNo)
                .like(username != null && !username.isEmpty(), BusinessLog::getUsername, username)
                .like(orderNo != null && !orderNo.isEmpty(), BusinessLog::getOrderNo, orderNo)
                .eq(actionType != null && !actionType.isEmpty(), BusinessLog::getActionType, actionType)
                .ge(from != null, BusinessLog::getCreateTime, from)
                .le(to != null, BusinessLog::getCreateTime, to)
                .orderByDesc(BusinessLog::getId);
        return ApiResult.success(businessLogMapper.selectPage(new Page<>(current, size), qw));
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BusinessException(fieldName + " 格式错误，应为 yyyy-MM-dd HH:mm:ss");
        }
    }
}

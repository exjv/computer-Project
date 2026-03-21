package com.jou.networkrepair.module.log.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.log.entity.LoginLog;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.LoginLogMapper;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {
    private final OperationLogMapper operationLogMapper;
    private final LoginLogMapper loginLogMapper;

    @GetMapping("/operation/page")
    public ApiResult<Page<OperationLog>> opPage(@RequestParam Long current, @RequestParam Long size) {
        return ApiResult.success(operationLogMapper.selectPage(new Page<>(current, size), null));
    }

    @GetMapping("/login/page")
    public ApiResult<Page<LoginLog>> loginPage(@RequestParam Long current, @RequestParam Long size) {
        return ApiResult.success(loginLogMapper.selectPage(new Page<>(current, size), null));
    }
}

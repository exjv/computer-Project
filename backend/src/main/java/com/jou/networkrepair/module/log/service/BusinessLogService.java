package com.jou.networkrepair.module.log.service;

import com.jou.networkrepair.module.log.entity.BusinessLog;
import com.jou.networkrepair.module.log.mapper.BusinessLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class BusinessLogService {
    private final BusinessLogMapper businessLogMapper;

    public void record(Long userId, String employeeNo, String username, String role,
                       String orderNo, String deviceCode, String actionType, String content) {
        BusinessLog log = new BusinessLog();
        log.setUserId(userId);
        log.setEmployeeNo(employeeNo);
        log.setUsername(username);
        log.setRole(role);
        log.setOrderNo(orderNo);
        log.setDeviceCode(deviceCode);
        log.setActionType(actionType);
        log.setContent(content);
        log.setCreateTime(LocalDateTime.now());
        businessLogMapper.insert(log);
    }
}

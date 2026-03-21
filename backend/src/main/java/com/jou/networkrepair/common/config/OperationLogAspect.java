package com.jou.networkrepair.common.config;

import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {
    private final OperationLogMapper operationLogMapper;

    @AfterReturning("@annotation(loggable)")
    public void record(JoinPoint joinPoint, Loggable loggable) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;
        HttpServletRequest request = attrs.getRequest();
        OperationLog log = new OperationLog();
        log.setUserId((Long) request.getAttribute("userId"));
        log.setUsername(String.valueOf(request.getUserPrincipal() == null ? "" : request.getUserPrincipal().getName()));
        log.setModule(loggable.module()); log.setOperationType(loggable.operationType()); log.setOperationDesc(loggable.operationDesc());
        log.setRequestMethod(request.getMethod()); log.setRequestUrl(request.getRequestURI()); log.setIp(request.getRemoteAddr());
        log.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}

package com.jou.networkrepair.module.v2.log2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLogV2 {
    private Long id;
    private String traceId;
    private Long userId;
    private String username;
    private String module;
    private String operationType;
    private String operationDesc;
    private String requestMethod;
    private String requestUrl;
    private String ip;
    private String resultStatus;
    private String errorMessage;
    private Long costMs;
    private LocalDateTime operationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

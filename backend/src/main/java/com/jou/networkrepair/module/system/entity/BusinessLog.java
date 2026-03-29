package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_log")
public class BusinessLog {
    private Long id;

    private String traceId;

    /** 兼容旧字段 */
    private String businessType;
    private String businessNo;

    /** 标准化字段 */
    private String bizType;
    private Long bizId;
    private String orderNo;

    private String action;
    private Long operatorId;
    private String operatorEmployeeNo;
    private String operatorJobNo;
    private String operatorName;
    private String operatorRole;

    private String content;
    private String status;
    private String extJson;

    private LocalDateTime operationTime;
    private LocalDateTime createTime;
}

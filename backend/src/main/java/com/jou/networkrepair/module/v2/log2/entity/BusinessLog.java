package com.jou.networkrepair.module.v2.log2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_log")
public class BusinessLog {
    private Long id;
    private String traceId;
    private String bizType;
    private Long bizId;
    private String orderNo;
    private String action;
    private Long operatorId;
    private String operatorJobNo;
    private String operatorRole;
    private String content;
    private String extJson;
    private LocalDateTime operationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

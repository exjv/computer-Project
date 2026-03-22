package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_log")
public class BusinessLog {
    private Long id;
    private String businessType;
    private String businessNo;
    private String action;
    private Long operatorId;
    private String operatorEmployeeNo;
    private String operatorName;
    private String content;
    private String status;
    private LocalDateTime createTime;
}

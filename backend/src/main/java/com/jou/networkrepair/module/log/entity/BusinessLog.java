package com.jou.networkrepair.module.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("business_log")
public class BusinessLog {
    private Long id;
    private Long userId;
    private String employeeNo;
    private String username;
    private String role;
    private String orderNo;
    private String deviceCode;
    private String actionType;
    private String content;
    private LocalDateTime createTime;
}

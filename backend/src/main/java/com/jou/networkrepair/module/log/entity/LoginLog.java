package com.jou.networkrepair.module.log.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("login_log")
public class LoginLog {
    private Long id;
    private Long userId;
    private String username;
    private String ip;
    private String userAgent;
    private String loginStatus;
    private String failReason;
    private LocalDateTime loginTime;
}

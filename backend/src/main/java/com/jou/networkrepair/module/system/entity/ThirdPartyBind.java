package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("third_party_bind")
public class ThirdPartyBind {
    private Long id;
    private Long userId;
    private String userEmployeeNo;
    private String provider;
    private String openId;
    private String unionId;
    private String bindStatus;
    private LocalDateTime bindTime;
    private LocalDateTime unbindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

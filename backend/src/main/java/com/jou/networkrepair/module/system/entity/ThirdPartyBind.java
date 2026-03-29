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

    /** 兼容字段：provider -> platform */
    private String provider;
    private String platform;

    private String openId;
    private String unionId;

    /** 兼容字段：bindStatus -> status */
    private String bindStatus;
    private Integer status;

    private LocalDateTime bindTime;
    private LocalDateTime unbindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

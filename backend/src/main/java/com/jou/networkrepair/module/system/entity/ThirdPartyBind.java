package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("third_party_bind")
public class ThirdPartyBind {
    private Long id;
    private Long userId;
    @TableField(exist = false)
    private String userEmployeeNo;

    /** 兼容字段：provider -> platform */
    private String provider;
    private String platform;

    private String openId;
    private String unionId;

    /** 兼容字段：bindStatus -> status */
    private Integer bindStatus;
    private Integer status;

    private LocalDateTime bindTime;
    @TableField(exist = false)
    private LocalDateTime unbindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

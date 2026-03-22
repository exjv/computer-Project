package com.jou.networkrepair.module.v2.auth2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("third_party_bind")
public class ThirdPartyBind {
    private Long id;
    private Long userId;
    private String platform;
    private String openId;
    private String unionId;
    private Integer status;
    private LocalDateTime bindTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

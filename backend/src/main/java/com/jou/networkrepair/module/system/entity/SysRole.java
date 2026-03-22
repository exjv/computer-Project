package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role")
public class SysRole {
    private Long id;
    private String roleCode;
    private String roleName;
    private String roleStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

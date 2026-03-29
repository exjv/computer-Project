package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_role")
public class UserRole {
    private Long id;
    private Long userId;
    private Long roleId;

    @TableField("create_by")
    private Long createBy;

    private LocalDateTime createTime;
}

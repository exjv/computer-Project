package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_role")
public class UserRole {
    private Long id;
    private Long userId;
    private Long roleId;
    private LocalDateTime createTime;
}

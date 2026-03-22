
package com.jou.networkrepair.module.v2.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user_role")
public class UserRole {
    private Long id;

    private Long userId;
    private Long roleId;
    private java.time.LocalDateTime createTime;

}

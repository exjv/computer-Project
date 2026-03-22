
package com.jou.networkrepair.module.v2.rbac.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`role`")
public class Role {
    private Long id;

    private String roleCode;
    private String roleName;
    private String roleDesc;
    private Integer status;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}

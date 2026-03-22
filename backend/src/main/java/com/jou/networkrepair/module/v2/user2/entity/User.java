
package com.jou.networkrepair.module.v2.user2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("`user`")
public class User {
    private Long id;

    private String jobNo;
    private String username;
    private String realName;
    private String password;
    private String phone;
    private String email;
    private String department;
    private Integer status;
    private java.time.LocalDateTime lastLoginTime;
    private Integer deleted;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}

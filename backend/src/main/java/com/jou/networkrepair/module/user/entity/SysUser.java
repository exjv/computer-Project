package com.jou.networkrepair.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@TableName("`user`")
public class SysUser {
    private Long id;
    @NotBlank(message = "工号不能为空")
    @Size(max = 30, message = "工号长度不能超过30")
    private String employeeNo;
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;
    private String password;
    @NotBlank(message = "姓名不能为空")
    private String realName;
    private String phone;
    @Email(message = "邮箱格式不正确")
    private String email;
    private String department;
    private String role;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private String wxOpenId;
    private String qqOpenId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

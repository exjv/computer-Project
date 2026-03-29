package com.jou.networkrepair.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@TableName("`user`")
public class SysUser {
    private Long id;

    /** 工号（全局唯一） */
    @NotBlank(message = "工号不能为空")
    @Size(max = 30, message = "工号长度不能超过30")
    @TableField("employee_no")
    private String employeeNo;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过50")
    private String username;

    private String password;

    @NotBlank(message = "姓名不能为空")
    @TableField("real_name")
    private String realName;

    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private String department;

    /** 兼容旧结构（快速角色） */
    private String role;

    /** 1启用 0禁用 */
    private Integer status;

    @TableField("last_login_time")
    private LocalDateTime lastLoginTime;

    @TableField("wx_open_id")
    private String wxOpenId;

    @TableField("qq_open_id")
    private String qqOpenId;

    /** 是否绑定第三方账号：1已绑定 0未绑定 */
    @TableField("third_party_bound_flag")
    private Integer thirdPartyBoundFlag;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}

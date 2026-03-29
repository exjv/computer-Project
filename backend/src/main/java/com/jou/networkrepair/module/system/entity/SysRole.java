package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("role")
public class SysRole {
    private Long id;
    private String roleCode;
    private String roleName;

    /** ENABLED/DISABLED（兼容旧字段） */
    private String roleStatus;

    /** 规范化状态：1启用 0禁用 */
    private Integer status;

    private String roleDesc;
    private String remark;

    @TableField("create_by")
    private Long createBy;
    @TableField("update_by")
    private Long updateBy;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

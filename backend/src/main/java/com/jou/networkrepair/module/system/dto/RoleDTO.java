package com.jou.networkrepair.module.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RoleDTO {
    @NotBlank(message = "角色编码不能为空")
    private String roleCode;
    @NotBlank(message = "角色名称不能为空")
    private String roleName;
    private String roleStatus;
    private String remark;
}

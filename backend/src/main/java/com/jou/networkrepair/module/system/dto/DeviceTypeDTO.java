package com.jou.networkrepair.module.system.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeviceTypeDTO {
    @NotBlank(message = "类型编码不能为空")
    private String typeCode;

    @NotBlank(message = "类型名称不能为空")
    private String typeName;

    private Integer sortNo;

    /** 1启用 0禁用 */
    private Integer status;

    private String remark;
}

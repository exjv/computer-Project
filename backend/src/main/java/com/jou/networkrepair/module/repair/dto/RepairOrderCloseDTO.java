package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RepairOrderCloseDTO {
    @NotNull(message = "请指定是否强制关闭")
    private Boolean forceClose;
    @NotBlank(message = "关闭原因不能为空")
    @Size(max = 500, message = "关闭原因不能超过500字")
    private String closeReason;
}

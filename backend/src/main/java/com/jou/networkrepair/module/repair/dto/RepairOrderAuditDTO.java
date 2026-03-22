package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RepairOrderAuditDTO {
    @NotNull(message = "请传入审核结果")
    private Boolean approved;
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}

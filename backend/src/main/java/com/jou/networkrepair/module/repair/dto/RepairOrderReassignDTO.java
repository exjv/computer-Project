package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RepairOrderReassignDTO {
    @NotNull(message = "请指定维修人员")
    private Long assignMaintainerId;
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}

package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class RepairOrderAssignDTO {
    @NotNull(message = "维修人员不能为空")
    private Long assignMaintainerId;
}

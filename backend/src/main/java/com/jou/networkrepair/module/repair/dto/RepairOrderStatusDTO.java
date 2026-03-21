package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RepairOrderStatusDTO {
    @NotBlank(message = "工单状态不能为空")
    private String status;
}

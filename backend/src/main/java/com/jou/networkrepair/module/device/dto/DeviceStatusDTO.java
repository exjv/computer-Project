package com.jou.networkrepair.module.device.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class DeviceStatusDTO {
    @NotBlank(message = "状态不能为空")
    private String status;
}

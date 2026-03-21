package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RepairOrderCreateDTO {
    @NotNull(message = "设备不能为空")
    private Long deviceId;
    @NotBlank(message = "故障标题不能为空")
    @Size(max = 100, message = "故障标题长度不能超过100")
    private String title;
    @NotBlank(message = "故障描述不能为空")
    private String description;
    @NotBlank(message = "优先级不能为空")
    private String priority;
}

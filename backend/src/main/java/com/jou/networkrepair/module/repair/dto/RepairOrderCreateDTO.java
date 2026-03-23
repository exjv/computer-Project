package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class RepairOrderCreateDTO {
    @NotNull(message = "设备不能为空")
    private Long deviceId;

    @Size(max = 30, message = "报修人工号长度不能超过30")
    private String reporterEmployeeNo;

    @Size(max = 20, message = "联系方式长度不能超过20")
    private String contactPhone;

    @Size(max = 100, message = "所属部门长度不能超过100")
    private String reporterDepartment;

    @Size(max = 150, message = "地点长度不能超过150")
    private String reportLocation;

    @NotBlank(message = "故障描述不能为空")
    @Size(max = 2000, message = "故障描述长度不能超过2000")
    private String description;

    @Size(max = 100, message = "故障标题长度不能超过100")
    private String title;

    @Size(max = 50, message = "故障类型长度不能超过50")
    private String faultType;

    @NotBlank(message = "紧急程度不能为空")
    private String priority;

    private Integer affectWideAreaNetwork;
    private Integer needPurchaseParts;
    private String partsDescription;
    private Integer applyDelay;

    private LocalDateTime originalExpectedFinishTime;
    private LocalDateTime delayedExpectedFinishTime;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

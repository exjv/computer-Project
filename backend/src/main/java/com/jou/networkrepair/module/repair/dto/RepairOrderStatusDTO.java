package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class RepairOrderStatusDTO {
    @NotBlank(message = "工单状态不能为空")
    private String status;

    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;

    private Long assignMaintainerId;
    private Integer needPurchaseParts;
    private String partsDescription;
    private Integer applyDelay;
    private LocalDateTime delayedExpectedFinishTime;
    private LocalDateTime originalExpectedFinishTime;
    private String userConfirmResult;
    private Integer satisfactionScore;
    private String feedback;
    private String closeReason;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

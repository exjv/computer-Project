package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class RepairOrderDelayApproveDTO {
    @NotNull(message = "请传入审批结果")
    private Boolean approved;
    private LocalDateTime delayedExpectedFinishTime;
    @Size(max = 500, message = "备注不能超过500字")
    private String remark;
}

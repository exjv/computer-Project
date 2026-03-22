package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class RepairRecordDTO {
    @NotNull(message = "工单ID不能为空")
    private Long repairOrderId;
    @NotNull(message = "设备ID不能为空")
    private Long deviceId;
    private Long maintainerId;
    @NotBlank(message = "故障原因不能为空")
    private String faultReason;
    @NotBlank(message = "处理过程不能为空")
    private String processDetail;
    @NotBlank(message = "处理结果不能为空")
    private String resultDetail;
    @NotNull(message = "是否解决不能为空")
    private Integer isResolved;
    private Integer usedParts;
    private String usedPartsDesc;
    private Integer delayApplied;
    private String delayReason;
    private Integer laborHours;
    private String repairConclusion;
    private String fixMeasure;
    private String userConfirmResult;
    private Integer userSatisfaction;
    private String photoUrls;
    private String remark;
    private LocalDateTime reportTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;
    private LocalDateTime finishTime;
}

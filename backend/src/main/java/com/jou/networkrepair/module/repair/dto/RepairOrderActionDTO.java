package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
public class RepairOrderActionDTO {
    @NotBlank(message = "操作类型不能为空")
    private String action;
    private String remark;
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;
    private Long assignMaintainerId;
    private String delayedExpectFinishTime;
    private String scenePhotoUrls;
    private String handleDescription;
    private String partsRequirement;
    private Integer satisfactionScore;
    private String feedback;
}

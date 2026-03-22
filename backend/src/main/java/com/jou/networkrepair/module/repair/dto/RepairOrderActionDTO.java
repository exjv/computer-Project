package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RepairOrderActionDTO {
    @NotBlank(message = "操作类型不能为空")
    private String action;
    private String remark;
    @Min(value = 0, message = "进度不能小于0")
    @Max(value = 100, message = "进度不能大于100")
    private Integer progress;
    @Min(value = 1, message = "满意度最低为1")
    @Max(value = 5, message = "满意度最高为5")
    private Integer satisfactionScore;
    @Size(max = 500, message = "反馈意见不能超过500字")
    private String feedbackContent;
}

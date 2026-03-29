package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class RepairOrderFeedbackDTO {
    @NotBlank(message = "确认结果不能为空")
    private String confirmResult;

    @NotNull(message = "满意度不能为空")
    @Min(value = 1, message = "满意度最小为1")
    @Max(value = 5, message = "满意度最大为5")
    private Integer satisfactionScore;

    @NotBlank(message = "反馈意见不能为空")
    @Size(max = 500, message = "反馈意见长度不能超过500")
    private String feedbackContent;

    @Size(max = 500, message = "备注长度不能超过500")
    private String remark;
}

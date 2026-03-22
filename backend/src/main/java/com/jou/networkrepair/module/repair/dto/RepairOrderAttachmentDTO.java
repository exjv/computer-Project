package com.jou.networkrepair.module.repair.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RepairOrderAttachmentDTO {
    @NotBlank(message = "附件名称不能为空")
    private String fileName;
    @NotBlank(message = "附件地址不能为空")
    private String fileUrl;
    private String fileType;
    private String remark;
}

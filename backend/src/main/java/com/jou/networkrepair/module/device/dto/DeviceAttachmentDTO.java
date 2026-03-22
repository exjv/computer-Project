package com.jou.networkrepair.module.device.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class DeviceAttachmentDTO {
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    @NotBlank(message = "文件URL不能为空")
    private String fileUrl;
    @Size(max = 50, message = "文件类型长度不能超过50")
    private String fileType;
    /** DEVICE_PHOTO / FAULT_SCENE / REPAIR_RESULT */
    @NotBlank(message = "图片分类不能为空")
    private String category;
    @Size(max = 255, message = "备注长度不能超过255")
    private String remark;
}

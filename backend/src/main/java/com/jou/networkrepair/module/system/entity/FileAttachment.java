package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_attachment")
public class FileAttachment {
    private Long id;
    private String businessType;
    private Long businessId;
    private String fileName;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private Long uploaderId;
    private String uploaderEmployeeNo;
    private LocalDateTime uploadTime;
    private String remark;
}

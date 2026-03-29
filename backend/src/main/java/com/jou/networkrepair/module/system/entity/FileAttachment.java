package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_attachment")
public class FileAttachment {
    private Long id;

    /** 兼容历史字段 */
    private String businessType;
    private Long businessId;

    /** 标准化业务定位 */
    private String bizType;
    private Long bizId;

    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileUrl;
    private String fileType;
    private Long fileSize;
    private String fileHash;

    private Long uploaderId;
    private String uploaderEmployeeNo;
    private LocalDateTime uploadTime;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

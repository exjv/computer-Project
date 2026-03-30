package com.jou.networkrepair.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField(exist = false)
    private String originalFileName;
    @TableField(exist = false)
    private String filePath;
    private String fileUrl;
    private String fileType;
    @TableField(exist = false)
    private Long fileSize;
    @TableField(exist = false)
    private String fileHash;

    private Long uploaderId;
    @TableField(exist = false)
    private String uploaderEmployeeNo;
    private LocalDateTime uploadTime;
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

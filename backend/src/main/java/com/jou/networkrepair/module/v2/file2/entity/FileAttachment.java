package com.jou.networkrepair.module.v2.file2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_attachment")
public class FileAttachment {
    private Long id;
    private String bizType;
    private Long bizId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private String fileUrl;
    private String fileType;
    private Long uploaderId;
    private LocalDateTime uploadTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

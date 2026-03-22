package com.jou.networkrepair.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("notice")
public class Notice {
    private Long id;
    private String title;
    private String content;
    /** DRAFT/ONLINE/OFFLINE */
    private String status;
    private LocalDateTime publishTime;
    private Long publisherId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

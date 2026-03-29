package com.jou.networkrepair.module.notice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("announcement")
public class Notice {
    private Long id;
    private String title;
    private String content;

    /** DRAFT/ONLINE/OFFLINE/PUBLISHED */
    private String status;

    private LocalDateTime publishTime;
    private LocalDateTime expireTime;
    private Long publisherId;
    private Integer topFlag;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

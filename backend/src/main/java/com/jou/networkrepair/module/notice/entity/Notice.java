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
    private Long publisherId;
    private String status;
    private Integer sortNo;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

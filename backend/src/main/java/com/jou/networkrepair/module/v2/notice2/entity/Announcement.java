
package com.jou.networkrepair.module.v2.notice2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("announcement")
public class Announcement {
    private Long id;

    private String status;
    private Integer deleted;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}

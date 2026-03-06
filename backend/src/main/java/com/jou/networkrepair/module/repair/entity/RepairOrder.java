package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {
    private Long id;
    private String orderNo;
    private Long deviceId;
    private Long reporterId;
    private String title;
    private String description;
    private String priority;
    private String status;
    private Long assignMaintainerId;
    private LocalDateTime reportTime;
    private LocalDateTime assignTime;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

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
    private Integer progress;
    private LocalDateTime reportTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
    private Integer satisfactionScore;
    private String feedback;
    private String closeReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

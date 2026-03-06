package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_record")
public class RepairRecord {
    private Long id;
    private Long repairOrderId;
    private Long deviceId;
    private Long maintainerId;
    private String faultReason;
    private String processDetail;
    private String resultDetail;
    private Integer isResolved;
    private LocalDateTime repairTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

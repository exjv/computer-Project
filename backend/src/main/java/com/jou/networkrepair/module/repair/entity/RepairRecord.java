package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_record")
public class RepairRecord {
    private Long id;
    private Long repairOrderId;
    private String repairOrderNo;
    private Long deviceId;
    private String deviceCode;
    private Long maintainerId;
    private String maintainerEmployeeNo;
    private String maintainerName;
    private String faultReason;
    private String processDetail;
    private String resultDetail;
    private Integer isResolved;
    private Integer usedParts;
    private String usedPartsDesc;
    private Integer laborHours;
    private String repairConclusion;
    private LocalDateTime repairTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableField;
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

    /** 第几次维修（同设备维度） */
    @TableField("repair_count_no")
    private Integer repairSequence;

    /** 兼容历史字段 */
    @TableField(exist = false)
    private Integer maintenanceSequence;

    private LocalDateTime reportTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;
    private LocalDateTime finishTime;

    private Long maintainerId;
    private String maintainerEmployeeNo;
    private String maintainerName;

    private String faultReason;
    private String processDetail;
    private String fixMeasure;
    private String resultDetail;

    private Integer isResolved;
    private Integer usedParts;
    private String usedPartsDesc;
    private Integer delayApplied;
    private String delayReason;

    private Integer laborHours;
    private String repairConclusion;
    private String userConfirmResult;
    private Integer userSatisfaction;
    private String photoUrls;
    private String remark;

    private LocalDateTime repairTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

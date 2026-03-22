package com.jou.networkrepair.module.v2.repair2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("repair_record")
public class RepairRecord {
    private Long id;
    private Long deviceId;
    private Long repairOrderId;
    private Integer repairCountNo;
    private LocalDateTime reportTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;
    private LocalDateTime finishTime;
    private String faultReason;
    private String handlingMeasures;
    private Integer partReplacedFlag;
    private String partInfo;
    private Integer delayedFlag;
    private String delayReason;
    private BigDecimal actualDurationHours;
    private String repairResult;
    private String userConfirmResult;
    private Integer userSatisfactionScore;
    private Long maintainerUserId;
    private String remark;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_order_flow")
public class RepairOrderFlow {
    private Long id;
    private Long repairOrderId;
    private String fromStatus;
    private String toStatus;
    private String action;
    private Long operatorId;
    private String operatorEmployeeNo;
    private String operatorName;
    private String operatorRole;
    private String operationType;
    private String remark;
    private LocalDateTime createTime;
}

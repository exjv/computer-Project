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

    /** 历史字段兼容 */
    private String action;

    /** 标准化操作类型（SUBMIT/AUDIT_PASS/ASSIGN/ACCEPT/START/FINISH/CONFIRM 等） */
    private String operationType;

    private Long operatorId;
    private String operatorEmployeeNo;
    private String operatorName;
    private String operatorRole;
    private String remark;

    private Integer systemRecommendAssignFlag;
    private String extJson;

    private LocalDateTime operationTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}


package com.jou.networkrepair.module.v2.repair2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("repair_order_flow")
public class RepairOrderFlow {
    private Long id;

    private Long repairOrderId;
    private String fromStatus;
    private String toStatus;
    private String operationType;
    private Long operatorId;
    private java.time.LocalDateTime operationTime;
    private String operationDesc;
    private Integer systemRecommendAssignFlag;
    private String extJson;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;

}

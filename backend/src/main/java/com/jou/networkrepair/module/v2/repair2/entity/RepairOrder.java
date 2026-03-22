
package com.jou.networkrepair.module.v2.repair2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("repair_order")
public class RepairOrder {
    private Long id;

    private String orderNo;
    private Long repairUserId;
    private String repairUserJobNo;
    private String contactPhone;
    private String departmentOrLocation;
    private Long deviceId;
    private String currentStatus;
    private Integer progressPercent;
    private java.time.LocalDateTime submitTime;
    private java.time.LocalDateTime createTime;
    private java.time.LocalDateTime updateTime;
    private Integer deleted;

}

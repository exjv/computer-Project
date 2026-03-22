package com.jou.networkrepair.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairOrderModelVO {
    private Long id;
    private String orderNo;
    private String reporterEmployeeNo;
    private String deviceCode;
    private String deviceName;
    private String deviceType;
    private String faultType;
    private String priority;
    private String status;
    private Integer progress;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime finishTime;
}

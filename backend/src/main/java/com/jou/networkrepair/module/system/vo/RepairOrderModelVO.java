package com.jou.networkrepair.module.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RepairOrderModelVO {
    private Long id;
    private String orderNo;

    private Long reporterId;
    private String reporterEmployeeNo;
    private String reporterName;
    private String contactPhone;
    private String reporterDepartment;
    private String reportLocation;

    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;

    private String faultType;
    private String priority;
    private String status;
    private Integer progress;

    private LocalDateTime reportTime;
    private LocalDateTime assignTime;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
}

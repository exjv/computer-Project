package com.jou.networkrepair.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class NetworkDevice {
    private Long id;
    /** 重构字段：设备唯一编码 */
    private String deviceCode;
    /** 重构字段：设备名称 */
    private String deviceName;
    /** 重构字段：设备类型编码 */
    private String deviceType;
    private String brand;
    private String model;
    private String serialNumber;
    private String campus;
    private String buildingLocation;
    private LocalDate enableDate;
    private LocalDate warrantyExpiryDate;
    private String ownerName;
    private String manageDepartment;
    private String brandModel;
    private String ipAddress;
    private String macAddress;
    /** 新增字段：所属校区 */
    private String campus;
    /** 新增字段：楼宇 */
    private String building;
    /** 新增字段：机房 */
    private String machineRoom;
    /** 新增字段：办公室 */
    private String office;
    private String location;
    /** 重构字段：购买时间 */
    private LocalDate purchaseDate;
    /** 新增字段：启用时间 */
    private LocalDate enableDate;
    /** 新增字段：保修截止时间 */
    private LocalDate warrantyExpireDate;
    /** 新增字段：责任人ID */
    private Long ownerUserId;
    /** 新增字段：责任人工号 */
    private String ownerEmployeeNo;
    /** 新增字段：责任人姓名 */
    private String ownerName;
    /** 新增字段：管理部门 */
    private String managementDept;
    /** 重构字段：设备状态 */
    private String status;
    private LocalDateTime lastFaultTime;
    private Integer totalRepairRequests;
    private Integer totalRepairCount;
    private String faultReasonStats;
    private Integer repairApprovalRequired;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

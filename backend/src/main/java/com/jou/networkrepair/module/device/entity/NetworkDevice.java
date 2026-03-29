package com.jou.networkrepair.module.device.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("device")
public class NetworkDevice {
    private Long id;

    /** 设备编号（唯一） */
    @TableField("device_code")
    private String deviceCode;
    private String deviceName;

    /** 设备类型编码 */
    private String deviceType;

    /** 设备类型名称（冗余展示） */
    @TableField("device_type_name")
    private String deviceTypeName;

    private String brand;
    private String model;
    private String serialNumber;

    /** 所属校区 */
    private String campus;
    /** 所属楼宇/机房/办公室 */
    private String buildingLocation;

    private String building;
    private String machineRoom;
    private String office;
    private String location;

    private LocalDate purchaseDate;
    private LocalDate enableDate;

    /** 统一使用 warrantyExpireDate 映射数据库字段 */
    private LocalDate warrantyExpireDate;

    /** 兼容历史代码中 getWarrantyExpiryDate() 调用 */
    @TableField(exist = false)
    private LocalDate warrantyExpiryDate;

    private Long ownerUserId;
    private String ownerEmployeeNo;
    private String ownerName;

    @TableField("management_dept")
    private String managementDept;

    /** 兼容历史字段名 */
    @TableField(exist = false)
    private String manageDepartment;

    private String status;
    private LocalDateTime lastFaultTime;

    @TableField("total_repair_order_count")
    private Integer totalRepairRequests;

    private Integer totalRepairCount;
    private String faultReasonStats;
    private Integer repairApprovalRequired;
    private String remark;

    private String brandModel;
    private String ipAddress;
    private String macAddress;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

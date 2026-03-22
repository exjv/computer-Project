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
    /** 新增字段：设备类型名称（冗余） */
    private String deviceTypeName;
    /** 新增字段：品牌 */
    private String brand;
    /** 新增字段：型号 */
    private String model;
    /** 新增字段：序列号 */
    private String serialNo;
    /** 兼容字段：历史品牌型号 */
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
    /** 新增字段：最近故障时间 */
    private LocalDateTime lastFaultTime;
    /** 新增字段：累计报修次数 */
    private Integer totalRepairOrderCount;
    /** 新增字段：累计维修次数 */
    private Integer totalRepairCount;
    /** 新增字段：历史故障原因统计(JSON) */
    private String faultReasonStats;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {
    private Long id;
    /** 重构字段：工单编号（唯一） */
    private String orderNo;
    private Long deviceId;
    /** 新增字段：设备编号 */
    private String deviceCode;
    /** 新增字段：设备名称 */
    private String deviceName;
    /** 新增字段：设备类型 */
    private String deviceType;
    private Long reporterId;
    /** 新增字段：报修人工号 */
    private String reporterEmployeeNo;
    /** 新增字段：报修人姓名 */
    private String reporterName;
    /** 新增字段：联系方式 */
    private String contactPhone;
    /** 新增字段：所属部门 */
    private String reporterDepartment;
    /** 新增字段：报修地点 */
    private String reportLocation;
    private String title;
    /** 新增字段：故障类型 */
    private String faultType;
    private String description;
    /** 重构字段：紧急程度 */
    private String priority;
    /** 新增字段：是否影响大范围网络 */
    private Integer affectWideAreaNetwork;
    private String status;
    /** 新增字段：审核人 */
    private Long auditBy;
    /** 新增字段：审核人工号 */
    private String auditByEmployeeNo;
    /** 新增字段：审核人姓名 */
    private String auditByName;
    /** 新增字段：分配人 */
    private Long assignBy;
    /** 新增字段：分配人工号 */
    private String assignByEmployeeNo;
    /** 新增字段：分配人姓名 */
    private String assignByName;
    private Long assignMaintainerId;
    /** 新增字段：指派维修人工号 */
    private String assignMaintainerEmployeeNo;
    /** 新增字段：指派维修人姓名 */
    private String assignMaintainerName;
    private Integer progress;
    /** 新增字段：是否需要采购配件 */
    private Integer needPurchaseParts;
    /** 新增字段：配件说明 */
    private String partsDescription;
    /** 新增字段：是否申请延期 */
    private Integer applyDelay;
    /** 新增字段：原预计完成时间 */
    private LocalDateTime originalExpectedFinishTime;
    /** 新增字段：延期后预计完成时间 */
    private LocalDateTime delayedExpectedFinishTime;
    private LocalDateTime reportTime;
    private LocalDateTime auditTime;
    private LocalDateTime assignTime;
    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;
    private LocalDateTime expectedFinishTime;
    private LocalDateTime finishTime;
    /** 重构字段：验收确认时间 */
    private LocalDateTime confirmTime;
    /** 新增字段：用户确认结果 */
    private String userConfirmResult;
    private Integer satisfactionScore;
    private String feedback;
    private String closeReason;
    /** 新增字段：备注 */
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

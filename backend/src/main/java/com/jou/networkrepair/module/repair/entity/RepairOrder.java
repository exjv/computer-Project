package com.jou.networkrepair.module.repair.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_order")
public class RepairOrder {
    private Long id;

    /** 工单编号（唯一） */
    private String orderNo;

    /** 报修用户 */
    private Long reporterId;
    private String reporterEmployeeNo;
    private String reporterName;
    private String contactPhone;
    private String reporterDepartment;
    private String reportLocation;

    /** 设备信息快照 */
    private Long deviceId;
    private String deviceCode;
    private String deviceName;
    private String deviceType;

    private String title;
    private String faultType;
    private String description;

    /** LOW / MEDIUM / HIGH */
    private String priority;
    private Integer affectWideAreaNetwork;

    private LocalDateTime reportTime;
    private LocalDateTime auditTime;
    private Long auditBy;
    private String auditByEmployeeNo;
    private String auditByName;

    private LocalDateTime assignTime;
    private Long assignBy;
    private String assignByEmployeeNo;
    private String assignByName;
    private Long assignMaintainerId;
    private String assignMaintainerEmployeeNo;
    private String assignMaintainerName;

    private LocalDateTime acceptTime;
    private LocalDateTime startRepairTime;

    /** SUBMITTED/AUDIT_PASS/PENDING_ASSIGN/IN_PROGRESS/COMPLETED/CLOSED/CANCELLED */
    private String status;

    /** 当前进度百分比 */
    private Integer progress;

    private Integer needPurchaseParts;
    private String partsDescription;

    private Integer applyDelay;
    private LocalDateTime originalExpectedFinishTime;
    private LocalDateTime delayedExpectedFinishTime;
    private LocalDateTime expectedFinishTime;

    private LocalDateTime finishTime;
    private LocalDateTime confirmTime;
    private String userConfirmResult;
    private Integer satisfactionScore;
    private String feedback;

    private String closeReason;
    private String remark;

    /** 兼容历史字段 */
    @TableField(exist = false)
    private String scenePhotoUrls;
    @TableField(exist = false)
    private String handleDescription;
    @TableField(exist = false)
    private String delayReason;
    @TableField(exist = false)
    private String partsRequirement;

    @TableField("create_by")
    private Long createBy;

    @TableField("update_by")
    private Long updateBy;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

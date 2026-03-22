package com.jou.networkrepair.module.v2.repair2.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("repair_feedback")
public class RepairFeedback {
    private Long id;
    private Long repairOrderId;
    private Long repairRecordId;
    private Long userId;
    private String confirmResult;
    private Integer satisfactionScore;
    private String feedbackContent;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}

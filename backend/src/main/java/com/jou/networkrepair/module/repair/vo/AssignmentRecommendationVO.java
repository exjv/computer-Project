package com.jou.networkrepair.module.repair.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssignmentRecommendationVO {
    private Long maintainerId;
    private String maintainerName;
    private Double recommendationScore;
    private Double priorityScore;
    private Double loadScore;
    private Long unfinishedCount;
    private Long processingCount;
    private Double avgHandleHours;
    private Double skillMatchScore;
    private String recommendReason;
}

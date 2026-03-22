package com.jou.networkrepair.module.repair.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MaintainerRecommendVO {
    private Long maintainerId;
    private String maintainerName;
    private Double recommendationScore;
    private Double priorityScore;
    private Double loadScore;
    private Long unfinishedCount;
    private Long processingCount;
    private Double avgHandleHours;
    private Boolean skillMatched;
    private String reason;
}

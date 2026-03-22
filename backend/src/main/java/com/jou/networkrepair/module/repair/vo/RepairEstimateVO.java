package com.jou.networkrepair.module.repair.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RepairEstimateVO {
    private String estimatedFinishTime;
    private Double estimatedHours;
    private String basis;
}

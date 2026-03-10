package com.jou.networkrepair.module.repair.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DispatchResultVO {
    private String orderNo;
    private Long maintainerId;
    private String message;
}

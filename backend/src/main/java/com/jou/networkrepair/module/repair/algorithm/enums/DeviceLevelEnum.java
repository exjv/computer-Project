package com.jou.networkrepair.module.repair.algorithm.enums;

public enum DeviceLevelEnum {
    CORE(100D),
    AGGREGATION(70D),
    ACCESS(40D),
    TERMINAL(20D);

    private final Double weight;

    DeviceLevelEnum(Double weight) { this.weight = weight; }

    public Double weight() { return weight; }
}

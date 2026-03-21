package com.jou.networkrepair.module.repair.algorithm.enums;

public enum LocationWeightEnum {
    NETWORK_CENTER(100D),
    DATA_CENTER(90D),
    TEACHING_BUILDING(60D),
    DORMITORY(40D),
    OTHER(20D);

    private final Double weight;

    LocationWeightEnum(Double weight) { this.weight = weight; }

    public Double weight() { return weight; }
}

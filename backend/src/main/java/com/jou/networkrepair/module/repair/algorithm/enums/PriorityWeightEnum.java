package com.jou.networkrepair.module.repair.algorithm.enums;

public enum PriorityWeightEnum {
    HIGH("高", 100D),
    MEDIUM("中", 60D),
    LOW("低", 20D);

    private final String code;
    private final Double weight;

    PriorityWeightEnum(String code, Double weight) {
        this.code = code;
        this.weight = weight;
    }

    public static double from(String code) {
        for (PriorityWeightEnum e : values()) {
            if (e.code.equals(code)) return e.weight;
        }
        return LOW.weight;
    }
}

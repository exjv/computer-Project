package com.jou.networkrepair.module.repair.enums;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum RepairOrderStatusEnum {
    DRAFT("待提交"),
    SUBMITTED_PENDING_REVIEW("已提交/待审核"),
    REVIEW_APPROVED("审核通过"),
    REVIEW_REJECTED("审核驳回"),
    PENDING_ASSIGN("待分配"),
    ASSIGNED("已分配"),
    PENDING_ACCEPT("待接单"),
    MAINTAINER_ACCEPTED("维修人员已接单"),
    IN_REPAIR("维修中"),
    PENDING_PARTS("待采购/待配件"),
    DELAY_APPLYING("申请延期中"),
    DELAY_APPROVED("延期已批准"),
    PENDING_CONFIRM("待验收/待确认"),
    FINISHED("已完成"),
    CLOSED("已关闭"),
    CANCELED("已取消");

    private final String label;

    RepairOrderStatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(v -> v.label.equals(value));
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(RepairOrderStatusEnum::getLabel).collect(Collectors.toList());
    }
}

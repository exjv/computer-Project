package com.jou.networkrepair.module.repair.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工单生命周期状态（第7步骨架）
 */
public enum RepairOrderStatusEnum {
    DRAFT("待提交"),
    SUBMITTED("已提交/待审核"),
    AUDIT_APPROVED("审核通过"),
    AUDIT_REJECTED("审核驳回"),
    PENDING_ASSIGN("待分配"),
    ASSIGNED("已分配"),
    PENDING_ACCEPT("待接单"),
    ACCEPTED("维修人员已接单"),
    IN_PROGRESS("维修中"),
    PENDING_PARTS("待采购/待配件"),
    DELAY_APPLYING("申请延期中"),
    DELAY_APPROVED("延期已批准"),
    PENDING_CONFIRM("待验收/待确认"),
    COMPLETED("已完成"),
    CLOSED("已关闭"),
    CANCELED("已取消");

    private final String label;

    RepairOrderStatusEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static boolean containsLabel(String value) {
        return Arrays.stream(values()).anyMatch(v -> v.label.equals(value));
    }

    public static List<String> labels() {
        return Arrays.stream(values()).map(RepairOrderStatusEnum::getLabel).collect(Collectors.toList());
    }

    public static Set<String> terminalStatuses() {
        return Arrays.stream(new RepairOrderStatusEnum[]{COMPLETED, CLOSED, CANCELED})
                .map(RepairOrderStatusEnum::getLabel)
                .collect(Collectors.toSet());
    }
}

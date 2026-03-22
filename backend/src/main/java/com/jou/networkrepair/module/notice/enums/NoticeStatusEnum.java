package com.jou.networkrepair.module.notice.enums;

import com.jou.networkrepair.common.exception.BusinessException;

public enum NoticeStatusEnum {
    DRAFT, ONLINE, OFFLINE;

    public static NoticeStatusEnum of(String value) {
        for (NoticeStatusEnum item : values()) {
            if (item.name().equalsIgnoreCase(value)) return item;
        }
        throw new BusinessException("公告状态不合法");
    }
}

package com.jou.networkrepair.common.constant;

/**
 * 系统权限码定义（页面级 + 接口级统一）
 */
public interface PermissionCode {
    String USER_MANAGE = "user:manage";
    String ROLE_MANAGE = "role:manage";
    String DEVICE_MANAGE = "device:manage";
    String REPAIR_ORDER_APPROVE = "repair:order:approve";
    String REPAIR_ORDER_ASSIGN = "repair:order:assign";
    String REPAIR_ORDER_VIEW_ALL = "repair:order:view:all";
    String REPAIR_ORDER_VIEW_SELF = "repair:order:view:self";
    String REPAIR_ORDER_CREATE = "repair:order:create";
    String REPAIR_ORDER_ACCEPT = "repair:order:accept";
    String REPAIR_ORDER_REJECT = "repair:order:reject";
    String REPAIR_ORDER_PROGRESS = "repair:order:progress";
    String REPAIR_RECORD_WRITE = "repair:record:write";
    String REPAIR_ATTACHMENT_UPLOAD = "repair:attachment:upload";
    String REPAIR_EXPECT_FINISH = "repair:expected-finish:update";
    String REPAIR_DELAY_APPLY = "repair:delay:apply";
    String REPAIR_PARTS_APPLY = "repair:parts:apply";
    String NOTICE_PUBLISH = "notice:publish";
    String STATISTICS_VIEW = "statistics:view";
    String REPORT_EXPORT = "report:export";
    String LOG_OPERATION_VIEW = "log:operation:view";
    String LOG_BUSINESS_VIEW = "log:business:view";
    String REPAIR_SUPERVISE = "repair:supervise";
    String REPAIR_FEEDBACK_CONFIRM = "repair:feedback:confirm";
}

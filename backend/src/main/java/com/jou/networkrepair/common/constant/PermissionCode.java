package com.jou.networkrepair.common.constant;

/**
 * 统一 RBAC 权限码（接口级 + 页面按钮级）
 */
public interface PermissionCode {
    // 系统管理
    String USER_MANAGE = "user:manage";
    String USER_QUERY_BY_EMPLOYEE_NO = "user:query:employee-no";
    String ROLE_MANAGE = "role:manage";

    // 设备管理
    String DEVICE_MANAGE = "device:manage";
    String DEVICE_VIEW = "device:view";

    // 工单流程（管理员）
    String REPAIR_ORDER_APPROVE = "repair:order:approve";
    String REPAIR_ORDER_ASSIGN = "repair:order:assign";
    String REPAIR_ORDER_VIEW_ALL = "repair:order:view:all";
    String REPAIR_SUPERVISE = "repair:supervise";

    // 工单流程（维修人员）
    String REPAIR_ORDER_VIEW_SELF = "repair:order:view:self";
    String REPAIR_ORDER_ACCEPT = "repair:order:accept";
    String REPAIR_ORDER_REJECT = "repair:order:reject";
    String REPAIR_ORDER_PROGRESS = "repair:order:progress";
    String REPAIR_RECORD_VIEW = "repair:record:view";
    String REPAIR_RECORD_WRITE = "repair:record:write";
    String REPAIR_ATTACHMENT_UPLOAD = "repair:attachment:upload";
    String REPAIR_EXPECT_FINISH = "repair:expected-finish:update";
    String REPAIR_DELAY_APPLY = "repair:delay:apply";
    String REPAIR_PARTS_APPLY = "repair:parts:apply";

    // 工单流程（报修用户）
    String REPAIR_ORDER_CREATE = "repair:order:create";
    String REPAIR_PROGRESS_TRACK = "repair:progress:track";
    String REPAIR_FEEDBACK_CONFIRM = "repair:feedback:confirm";

    // 公告、日志、统计、报表
    String NOTICE_PUBLISH = "notice:publish";
    String NOTICE_VIEW = "notice:view";
    String STATISTICS_VIEW = "statistics:view";
    String REPORT_EXPORT = "report:export";
    String LOG_OPERATION_VIEW = "log:operation:view";
    String LOG_BUSINESS_VIEW = "log:business:view";
}

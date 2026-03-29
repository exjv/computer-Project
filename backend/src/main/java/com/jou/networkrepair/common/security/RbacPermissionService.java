package com.jou.networkrepair.common.security;

import com.jou.networkrepair.common.constant.PermissionCode;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RbacPermissionService {

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();
    private static final Map<String, List<String>> ROUTE_ROLE_MAP = new LinkedHashMap<>();

    static {
        ROLE_PERMISSIONS.put("admin", new HashSet<>(Arrays.asList(
                PermissionCode.USER_MANAGE,
                PermissionCode.USER_QUERY_BY_EMPLOYEE_NO,
                PermissionCode.ROLE_MANAGE,
                PermissionCode.DEVICE_MANAGE,
                PermissionCode.DEVICE_VIEW,
                PermissionCode.REPAIR_ORDER_APPROVE,
                PermissionCode.REPAIR_ORDER_ASSIGN,
                PermissionCode.REPAIR_ORDER_VIEW_ALL,
                PermissionCode.REPAIR_RECORD_VIEW,
                PermissionCode.NOTICE_PUBLISH,
                PermissionCode.NOTICE_VIEW,
                PermissionCode.STATISTICS_VIEW,
                PermissionCode.REPORT_EXPORT,
                PermissionCode.LOG_OPERATION_VIEW,
                PermissionCode.LOG_BUSINESS_VIEW,
                PermissionCode.REPAIR_SUPERVISE
        )));

        ROLE_PERMISSIONS.put("maintainer", new HashSet<>(Arrays.asList(
                PermissionCode.DEVICE_VIEW,
                PermissionCode.REPAIR_ORDER_VIEW_SELF,
                PermissionCode.REPAIR_ORDER_ACCEPT,
                PermissionCode.REPAIR_ORDER_REJECT,
                PermissionCode.REPAIR_ORDER_PROGRESS,
                PermissionCode.REPAIR_RECORD_VIEW,
                PermissionCode.REPAIR_RECORD_WRITE,
                PermissionCode.REPAIR_ATTACHMENT_UPLOAD,
                PermissionCode.REPAIR_EXPECT_FINISH,
                PermissionCode.REPAIR_DELAY_APPLY,
                PermissionCode.REPAIR_PARTS_APPLY,
                PermissionCode.NOTICE_VIEW
        )));

        ROLE_PERMISSIONS.put("user", new HashSet<>(Arrays.asList(
                PermissionCode.DEVICE_VIEW,
                PermissionCode.REPAIR_ORDER_CREATE,
                PermissionCode.REPAIR_ORDER_VIEW_SELF,
                PermissionCode.REPAIR_RECORD_VIEW,
                PermissionCode.REPAIR_PROGRESS_TRACK,
                PermissionCode.REPAIR_FEEDBACK_CONFIRM,
                PermissionCode.NOTICE_VIEW
        )));

        ROUTE_ROLE_MAP.put("/", Arrays.asList("admin", "maintainer", "user"));
        ROUTE_ROLE_MAP.put("/users", Collections.singletonList("admin"));
        ROUTE_ROLE_MAP.put("/devices", Arrays.asList("admin", "maintainer", "user"));
        ROUTE_ROLE_MAP.put("/repair-orders", Arrays.asList("admin", "maintainer", "user"));
        ROUTE_ROLE_MAP.put("/repair-records", Arrays.asList("admin", "maintainer"));
        ROUTE_ROLE_MAP.put("/logs", Collections.singletonList("admin"));
        ROUTE_ROLE_MAP.put("/profile", Arrays.asList("admin", "maintainer", "user"));
    }

    public Set<String> permissionsByRoles(Collection<String> roles) {
        Set<String> result = new HashSet<>();
        if (roles == null) return result;
        for (String role : roles) {
            if (role == null) continue;
            Set<String> rolePerm = ROLE_PERMISSIONS.get(normalizeRole(role));
            if (rolePerm != null) result.addAll(rolePerm);
        }
        return result;
    }

    public Set<String> permissionsByRole(String role) {
        return new HashSet<>(ROLE_PERMISSIONS.getOrDefault(normalizeRole(role), Collections.emptySet()));
    }

    public Map<String, List<String>> routeRoleMap() {
        return ROUTE_ROLE_MAP;
    }

    public String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }
}

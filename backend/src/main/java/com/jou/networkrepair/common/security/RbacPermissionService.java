package com.jou.networkrepair.common.security;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class RbacPermissionService {

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put("admin", new HashSet<>(Arrays.asList(
                "dashboard:view", "users:manage", "devices:manage", "repair:all:view",
                "repair:audit", "repair:reject", "repair:assign", "repair:reassign",
                "repair:delay:approve", "repair:close", "repair:stats", "repair:export",
                "repair:record:view", "repair:record:manage", "log:view", "notice:publish",
                "notice:manage", "report:export"
        )));

        ROLE_PERMISSIONS.put("maintainer", new HashSet<>(Arrays.asList(
                "dashboard:view", "devices:view", "repair:assigned:view", "repair:accept",
                "repair:reject:receive", "repair:start", "repair:progress:update", "repair:photo:upload",
                "repair:delay:apply", "repair:parts:apply", "repair:finish", "repair:record:view",
                "repair:record:manage", "notice:view"
        )));

        ROLE_PERMISSIONS.put("user", new HashSet<>(Arrays.asList(
                "dashboard:view", "repair:create", "repair:cancel", "repair:my:view",
                "repair:progress:view", "repair:confirm", "repair:feedback", "devices:view", "notice:view"
        )));
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

    public String normalizeRole(String role) {
        return role == null ? "" : role.trim().toLowerCase(Locale.ROOT);
    }
}

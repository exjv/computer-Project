package com.jou.networkrepair.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.module.system.entity.SysRole;
import com.jou.networkrepair.module.system.entity.RolePermission;
import com.jou.networkrepair.module.system.entity.SysPermission;
import com.jou.networkrepair.module.system.entity.UserRole;
import com.jou.networkrepair.module.system.mapper.RolePermissionMapper;
import com.jou.networkrepair.module.system.mapper.SysPermissionMapper;
import com.jou.networkrepair.module.system.mapper.SysRoleMapper;
import com.jou.networkrepair.module.system.mapper.UserRoleMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

@Component("permissionService")
@RequiredArgsConstructor
public class PermissionService {
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final SysPermissionMapper permissionMapper;
    private final UserMapper userMapper;

    public boolean hasPermission(String permissionCode) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return false;
        HttpServletRequest request = attrs.getRequest();
        Long userId = (Long) request.getAttribute("userId");
        String role = (String) request.getAttribute("role");
        if (userId == null) return false;
        return getPermissionSet(userId, role).contains(permissionCode);
    }

    public Set<String> getPermissionSet(Long userId, String fallbackRole) {
        Set<String> roleCodes = getRoleCodes(userId, fallbackRole);
        Set<String> result = getPermissionSetFromDb(userId);
        if (!result.isEmpty()) return result;
        result = new HashSet<>();
        for (String roleCode : roleCodes) {
            result.addAll(rolePermissionMap().getOrDefault(roleCode, Collections.emptySet()));
        }
        return result;
    }

    public Set<String> getRoleCodes(Long userId, String fallbackRole) {
        List<UserRole> mappings = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        Set<String> roleCodes = new HashSet<>();
        if (!mappings.isEmpty()) {
            for (UserRole mapping : mappings) {
                SysRole role = roleMapper.selectById(mapping.getRoleId());
                if (role != null && role.getRoleCode() != null) {
                    roleCodes.add(role.getRoleCode());
                }
            }
        }
        if (roleCodes.isEmpty()) {
            SysUser user = userMapper.selectById(userId);
            if (user != null && user.getRole() != null) roleCodes.add(user.getRole());
            if (fallbackRole != null && !fallbackRole.trim().isEmpty()) roleCodes.add(fallbackRole);
        }
        return roleCodes;
    }

    private Set<String> getPermissionSetFromDb(Long userId) {
        List<UserRole> mappings = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        if (mappings.isEmpty()) return Collections.emptySet();
        Set<String> result = new HashSet<>();
        for (UserRole mapping : mappings) {
            List<RolePermission> rps = rolePermissionMapper.selectList(new LambdaQueryWrapper<RolePermission>()
                    .eq(RolePermission::getRoleId, mapping.getRoleId()));
            for (RolePermission rp : rps) {
                SysPermission permission = permissionMapper.selectById(rp.getPermissionId());
                if (permission != null && "ENABLED".equals(permission.getStatus())) {
                    result.add(permission.getPermissionCode());
                }
            }
        }
        return result;
    }

    private Map<String, Set<String>> rolePermissionMap() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("admin", new HashSet<>(Arrays.asList(
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

        map.put("maintainer", new HashSet<>(Arrays.asList(
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

        map.put("user", new HashSet<>(Arrays.asList(
                PermissionCode.DEVICE_VIEW,
                PermissionCode.REPAIR_ORDER_CREATE,
                PermissionCode.REPAIR_ORDER_VIEW_SELF,
                PermissionCode.REPAIR_RECORD_VIEW,
                PermissionCode.REPAIR_PROGRESS_TRACK,
                PermissionCode.REPAIR_FEEDBACK_CONFIRM,
                PermissionCode.NOTICE_VIEW
        )));
        return map;
    }
}

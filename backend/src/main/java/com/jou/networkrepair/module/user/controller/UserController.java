package com.jou.networkrepair.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.system.entity.SysRole;
import com.jou.networkrepair.module.system.entity.UserRole;
import com.jou.networkrepair.module.system.mapper.SysRoleMapper;
import com.jou.networkrepair.module.system.mapper.UserRoleMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasPermission('" + PermissionCode.USER_MANAGE + "')")
public class UserController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;

    @GetMapping("/page")
    public ApiResult<Page<SysUser>> page(@RequestParam Long current, @RequestParam Long size,
                                         @RequestParam(required = false) String employeeNo,
                                         @RequestParam(required = false) String username,
                                         @RequestParam(required = false) String role,
                                         @RequestParam(required = false) String phone) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .like(employeeNo != null && !employeeNo.isEmpty(), SysUser::getEmployeeNo, employeeNo)
                .like(username != null && !username.isEmpty(), SysUser::getUsername, username)
                .eq(role != null && !role.isEmpty(), SysUser::getRole, role)
                .like(phone != null && !phone.isEmpty(), SysUser::getPhone, phone)
                .orderByDesc(SysUser::getId);
        return ApiResult.success(userMapper.selectPage(new Page<>(current, size), qw));
    }

    @GetMapping("/by-employee-no")
    public ApiResult<SysUser> byEmployeeNo(@RequestParam String employeeNo) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(null);
        return ApiResult.success(user);
    }

    @PostMapping
    @Loggable(module = "通用", operationType = "新增", operationDesc = "新增数据")
    public ApiResult<Void> add(@RequestBody @Validated SysUser user) {
        checkEmployeeNoUnique(user.getEmployeeNo(), null);
        user.setPassword(passwordEncoder.encode(user.getPassword() == null ? "123456" : user.getPassword()));
        user.setCreateTime(LocalDateTime.now()); user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        bindUserRole(user.getId(), user.getRole());
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @Loggable(module = "通用", operationType = "修改", operationDesc = "修改数据")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated SysUser req) {
        checkEmployeeNoUnique(req.getEmployeeNo(), id);
        req.setId(id); req.setUpdateTime(LocalDateTime.now()); req.setPassword(null);
        userMapper.updateById(req);
        if (req.getRole() != null && !req.getRole().trim().isEmpty()) {
            bindUserRole(id, req.getRole());
        }
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Loggable(module = "通用", operationType = "删除", operationDesc = "删除数据")
    public ApiResult<Void> delete(@PathVariable Long id) { userMapper.deleteById(id); return ApiResult.success("删除成功", null); }

    @PutMapping("/{id}/reset-password")
    public ApiResult<Void> resetPwd(@PathVariable Long id) {
        SysUser user = new SysUser(); user.setId(id); user.setPassword(passwordEncoder.encode("123456")); user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user); return ApiResult.success("重置成功", null);
    }

    @PutMapping("/{id}/status")
    public ApiResult<Void> status(@PathVariable Long id, @RequestBody SysUser req) {
        SysUser user = new SysUser(); user.setId(id); user.setStatus(req.getStatus()); user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user); return ApiResult.success("状态更新成功", null);
    }

    @GetMapping("/list-by-role")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ApiResult<List<SysUser>> listByRole(@RequestParam String role) {
        return ApiResult.success(userMapper.selectList(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, role).eq(SysUser::getStatus, 1)));
    }

    @GetMapping("/employee-no/check")
    public ApiResult<Map<String, Object>> employeeNoCheck(@RequestParam String employeeNo,
                                                           @RequestParam(required = false) Long excludeUserId) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        boolean available = exists == null || (excludeUserId != null && excludeUserId.equals(exists.getId()));
        Map<String, Object> result = new HashMap<>();
        result.put("employeeNo", employeeNo);
        result.put("available", available);
        return ApiResult.success(result);
    }

    private void checkEmployeeNoUnique(String employeeNo, Long id) {
        if (employeeNo == null || employeeNo.trim().isEmpty()) throw new BusinessException("工号不能为空");
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        if (exists != null && (id == null || !id.equals(exists.getId()))) {
            throw new BusinessException("工号已存在");
        }
    }

    private void bindUserRole(Long userId, String roleCode) {
        if (userId == null || roleCode == null || roleCode.trim().isEmpty()) return;
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) return;
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        UserRole ur = new UserRole();
        ur.setUserId(userId);
        ur.setRoleId(role.getId());
        ur.setCreateTime(LocalDateTime.now());
        userRoleMapper.insert(ur);
    }
}

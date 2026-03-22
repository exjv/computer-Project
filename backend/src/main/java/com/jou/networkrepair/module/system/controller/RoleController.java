package com.jou.networkrepair.module.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.system.dto.RoleDTO;
import com.jou.networkrepair.module.system.entity.SysRole;
import com.jou.networkrepair.module.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasPermission('" + PermissionCode.ROLE_MANAGE + "')")
public class RoleController {
    private final SysRoleMapper roleMapper;

    @GetMapping("/page")
    public ApiResult<Page<SysRole>> page(@RequestParam Long current, @RequestParam Long size,
                                         @RequestParam(required = false) String roleCode,
                                         @RequestParam(required = false) String roleName) {
        return ApiResult.success(roleMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<SysRole>()
                        .like(roleCode != null && !roleCode.isEmpty(), SysRole::getRoleCode, roleCode)
                        .like(roleName != null && !roleName.isEmpty(), SysRole::getRoleName, roleName)
                        .orderByDesc(SysRole::getId)));
    }

    @PostMapping
    public ApiResult<Void> create(@RequestBody @Validated RoleDTO dto) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, dto.getRoleCode()));
        if (count != null && count > 0) throw new BusinessException("角色编码已存在");
        SysRole role = new SysRole();
        role.setRoleCode(dto.getRoleCode());
        role.setRoleName(dto.getRoleName());
        role.setRoleStatus(dto.getRoleStatus() == null ? "ENABLED" : dto.getRoleStatus());
        role.setRemark(dto.getRemark());
        role.setCreateTime(LocalDateTime.now());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated RoleDTO dto) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) throw new BusinessException("角色不存在");
        role.setRoleName(dto.getRoleName());
        role.setRoleStatus(dto.getRoleStatus());
        role.setRemark(dto.getRemark());
        role.setUpdateTime(LocalDateTime.now());
        roleMapper.updateById(role);
        return ApiResult.success("修改成功", null);
    }
}

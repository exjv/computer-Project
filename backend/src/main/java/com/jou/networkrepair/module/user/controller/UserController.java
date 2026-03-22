package com.jou.networkrepair.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.constant.PermissionCode;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.system.entity.SysRole;
import com.jou.networkrepair.module.system.entity.ThirdPartyBind;
import com.jou.networkrepair.module.system.entity.UserRole;
import com.jou.networkrepair.module.system.mapper.SysRoleMapper;
import com.jou.networkrepair.module.system.mapper.ThirdPartyBindMapper;
import com.jou.networkrepair.module.system.mapper.UserRoleMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("@permissionService.hasPermission('" + PermissionCode.USER_MANAGE + "')")
public class UserController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleMapper userRoleMapper;
    private final SysRoleMapper roleMapper;
    private final ThirdPartyBindMapper thirdPartyBindMapper;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    @GetMapping("/page")
    public ApiResult<Page<Map<String, Object>>> page(@RequestParam Long current, @RequestParam Long size,
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
        Page<SysUser> raw = userMapper.selectPage(new Page<>(current, size), qw);
        Page<Map<String, Object>> result = new Page<>(raw.getCurrent(), raw.getSize(), raw.getTotal());
        List<Long> userIds = raw.getRecords().stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, List<ThirdPartyBind>> bindMap = userIds.isEmpty() ? new HashMap<>() : thirdPartyBindMapper.selectList(
                new LambdaQueryWrapper<ThirdPartyBind>().in(ThirdPartyBind::getUserId, userIds)
        ).stream().collect(Collectors.groupingBy(ThirdPartyBind::getUserId));
        result.setRecords(raw.getRecords().stream().map(u -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", u.getId());
            row.put("employeeNo", u.getEmployeeNo());
            row.put("username", u.getUsername());
            row.put("realName", u.getRealName());
            row.put("role", u.getRole());
            row.put("phone", u.getPhone());
            row.put("email", u.getEmail());
            row.put("department", u.getDepartment());
            row.put("status", u.getStatus());
            row.put("createTime", u.getCreateTime());
            row.put("lastLoginTime", u.getLastLoginTime());
            row.put("thirdPartyBinds", bindMap.getOrDefault(u.getId(), Collections.emptyList()).stream().map(v -> {
                Map<String, Object> b = new HashMap<>();
                b.put("provider", v.getProvider());
                b.put("openId", v.getOpenId());
                b.put("bindStatus", v.getBindStatus());
                b.put("bindTime", v.getBindTime());
                return b;
            }).collect(Collectors.toList()));
            return row;
        }).collect(Collectors.toList()));
        return ApiResult.success(result);
    }

    @GetMapping("/by-employee-no")
    public ApiResult<SysUser> byEmployeeNo(@RequestParam String employeeNo) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        if (user == null) throw new BusinessException("用户不存在");
        user.setPassword(null);
        return ApiResult.success(user);
    }

    @PostMapping
    @Loggable(module = "用户管理", operationType = "新增", operationDesc = "新增用户")
    public ApiResult<Void> add(@RequestBody @Validated SysUser user) {
        validateUserFields(user, null);
        checkEmployeeNoUnique(user.getEmployeeNo(), null);
        checkRoleValid(user.getRole());
        user.setPassword(passwordEncoder.encode(user.getPassword() == null || user.getPassword().trim().isEmpty() ? "123456" : user.getPassword()));
        user.setCreateTime(LocalDateTime.now()); user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        bindUserRole(user.getId(), user.getRole());
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @Loggable(module = "用户管理", operationType = "修改", operationDesc = "编辑用户")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated SysUser req) {
        validateUserFields(req, id);
        checkEmployeeNoUnique(req.getEmployeeNo(), id);
        if (req.getRole() != null && !req.getRole().trim().isEmpty()) checkRoleValid(req.getRole());
        req.setId(id); req.setUpdateTime(LocalDateTime.now()); req.setPassword(null);
        userMapper.updateById(req);
        if (req.getRole() != null && !req.getRole().trim().isEmpty()) {
            bindUserRole(id, req.getRole());
        }
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Loggable(module = "用户管理", operationType = "删除", operationDesc = "删除用户")
    public ApiResult<Void> delete(@PathVariable Long id) { userMapper.deleteById(id); return ApiResult.success("删除成功", null); }

    @PutMapping("/{id}/reset-password")
    @Loggable(module = "用户管理", operationType = "重置密码", operationDesc = "重置用户密码")
    public ApiResult<Void> resetPwd(@PathVariable Long id) {
        SysUser user = new SysUser(); user.setId(id); user.setPassword(passwordEncoder.encode("123456")); user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user); return ApiResult.success("重置成功", null);
    }

    @PutMapping("/{id}/status")
    @Loggable(module = "用户管理", operationType = "状态变更", operationDesc = "用户启用禁用")
    public ApiResult<Void> status(@PathVariable Long id, @RequestBody SysUser req) {
        SysUser user = new SysUser(); user.setId(id); user.setStatus(req.getStatus()); user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user); return ApiResult.success("状态更新成功", null);
    }

    @PutMapping("/{id}/assign-role")
    @Loggable(module = "用户管理", operationType = "分配角色", operationDesc = "分配用户角色")
    public ApiResult<Void> assignRole(@PathVariable Long id, @RequestParam String role) {
        checkRoleValid(role);
        SysUser user = new SysUser();
        user.setId(id);
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        bindUserRole(id, role);
        return ApiResult.success("角色分配成功", null);
    }

    @PostMapping("/batch")
    @Loggable(module = "用户管理", operationType = "批量新增", operationDesc = "批量新增用户")
    public ApiResult<Map<String, Object>> batchCreate(@RequestBody List<SysUser> users) {
        if (users == null || users.isEmpty()) throw new BusinessException("批量数据不能为空");
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < users.size(); i++) {
            SysUser user = users.get(i);
            try {
                validateUserFields(user, null);
                checkEmployeeNoUnique(user.getEmployeeNo(), null);
                checkRoleValid(user.getRole());
                user.setPassword(passwordEncoder.encode(user.getPassword() == null || user.getPassword().trim().isEmpty() ? "123456" : user.getPassword()));
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userMapper.insert(user);
                bindUserRole(user.getId(), user.getRole());
                success++;
            } catch (Exception e) {
                errors.add("第" + (i + 1) + "行失败：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", users.size());
        result.put("successCount", success);
        result.put("failCount", users.size() - success);
        result.put("errors", errors);
        return ApiResult.success(result);
    }

    @PostMapping("/import-excel")
    @Loggable(module = "用户管理", operationType = "导入", operationDesc = "批量导入用户")
    public ApiResult<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) throw new BusinessException("请上传Excel文件");
        List<String> errors = new ArrayList<>();
        int success = 0;
        Set<String> seenEmployeeNo = new HashSet<>();
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String employeeNo = cell(row, 0);
                String username = cell(row, 1);
                String realName = cell(row, 2);
                String role = cell(row, 3);
                String phone = cell(row, 4);
                String email = cell(row, 5);
                String department = cell(row, 6);
                String statusStr = cell(row, 7);
                try {
                    if (employeeNo.isEmpty() || username.isEmpty() || realName.isEmpty() || role.isEmpty()) {
                        throw new BusinessException("工号/用户名/姓名/角色为必填项");
                    }
                    if (seenEmployeeNo.contains(employeeNo)) throw new BusinessException("导入文件内工号重复：" + employeeNo);
                    seenEmployeeNo.add(employeeNo);
                    checkEmployeeNoUnique(employeeNo, null);
                    if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) throw new BusinessException("手机号格式错误");
                    checkRoleValid(role);
                    SysUser user = new SysUser();
                    user.setEmployeeNo(employeeNo);
                    user.setUsername(username);
                    user.setRealName(realName);
                    user.setRole(role);
                    user.setPhone(phone);
                    user.setEmail(email.isEmpty() ? null : email);
                    user.setDepartment(department);
                    user.setStatus(statusStr.isEmpty() ? 1 : Integer.valueOf(statusStr));
                    user.setPassword(passwordEncoder.encode("123456"));
                    user.setCreateTime(LocalDateTime.now());
                    user.setUpdateTime(LocalDateTime.now());
                    userMapper.insert(user);
                    bindUserRole(user.getId(), role);
                    success++;
                } catch (Exception e) {
                    errors.add("第" + (r + 1) + "行失败：" + e.getMessage());
                }
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failCount", errors.size());
        result.put("errors", errors);
        return ApiResult.success(result);
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

    private void checkRoleValid(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) throw new BusinessException("角色不能为空");
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) throw new BusinessException("角色不合法：" + roleCode);
    }

    private void validateUserFields(SysUser user, Long id) {
        if (user == null) throw new BusinessException("用户数据不能为空");
        if (user.getEmployeeNo() == null || user.getEmployeeNo().trim().isEmpty()) throw new BusinessException("工号不能为空");
        if (user.getRealName() == null || user.getRealName().trim().isEmpty()) throw new BusinessException("姓名不能为空");
        if (user.getRole() == null || user.getRole().trim().isEmpty()) throw new BusinessException("角色不能为空");
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty() && !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
    }

    private String cell(Row row, int idx) {
        if (row.getCell(idx) == null) return "";
        row.getCell(idx).setCellType(org.apache.poi.ss.usermodel.CellType.STRING);
        return row.getCell(idx).getStringCellValue() == null ? "" : row.getCell(idx).getStringCellValue().trim();
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

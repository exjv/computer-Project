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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysRoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final ThirdPartyBindMapper thirdPartyBindMapper;


    @GetMapping("/check-employee-no")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.USER_QUERY_BY_EMPLOYEE_NO + "')")
    public ApiResult<Boolean> checkEmployeeNo(@RequestParam String employeeNo,
                                              @RequestParam(required = false) Long excludeId) {
        Long cnt = userMapper.countByEmployeeNo(employeeNo, excludeId);
        return ApiResult.success(cnt == null || cnt == 0);
    }

    @GetMapping("/by-employee-no/{employeeNo}")
    @PreAuthorize("@permissionService.hasPermission('" + PermissionCode.USER_QUERY_BY_EMPLOYEE_NO + "')")
    public ApiResult<SysUser> getByEmployeeNo(@PathVariable String employeeNo) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        if (user == null) throw new BusinessException("用户不存在");
        return ApiResult.success(user);
    }

    @GetMapping("/roles")
    public ApiResult<List<SysRole>> roleList() {
        return ApiResult.success(roleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, 1).orderByAsc(SysRole::getId)));
    }

    @GetMapping("/page")
    public ApiResult<Page<Map<String, Object>>> page(@RequestParam Long current, @RequestParam Long size,
                                                      @RequestParam(required = false) String employeeNo,
                                                      @RequestParam(required = false) String role,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<SysUser> qw = new LambdaQueryWrapper<SysUser>()
                .like(employeeNo != null && !employeeNo.trim().isEmpty(), SysUser::getEmployeeNo, employeeNo)
                .eq(status != null, SysUser::getStatus, status)
                .and(keyword != null && !keyword.trim().isEmpty(), w -> w
                        .like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword)
                        .or().like(SysUser::getPhone, keyword)
                        .or().like(SysUser::getEmail, keyword)
                        .or().like(SysUser::getDepartment, keyword))
                .orderByDesc(SysUser::getId);

        if (role != null && !role.trim().isEmpty()) {
            SysRole roleEntity = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role));
            if (roleEntity == null) return ApiResult.success(new Page<>(current, size));
            List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleEntity.getId()));
            Set<Long> ids = userRoles.stream().map(UserRole::getUserId).collect(Collectors.toSet());
            if (ids.isEmpty()) return ApiResult.success(new Page<>(current, size));
            qw.in(SysUser::getId, ids);
        }

        Page<SysUser> p = userMapper.selectPage(new Page<>(current, size), qw);
        Page<Map<String, Object>> result = new Page<>(current, size, p.getTotal());
        result.setRecords(buildUserRows(p.getRecords()));
        return ApiResult.success(result);
    }

    @PostMapping
    @Loggable(module = "用户管理", operationType = "新增", operationDesc = "新增用户")
    public ApiResult<Void> add(@RequestBody @Validated SysUser user) {
        validateRequired(user.getEmployeeNo(), "工号不能为空");
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            user.setUsername(user.getEmployeeNo());
        }
        validateRequired(user.getRealName(), "姓名不能为空");
        validateRequired(user.getRole(), "角色不能为空");
        checkRoleValid(user.getRole());
        checkEmployeeNoUnique(user.getEmployeeNo(), null);
        if (user.getPhone() != null && !user.getPhone().trim().isEmpty() && !PHONE_PATTERN.matcher(user.getPhone()).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword() == null || user.getPassword().trim().isEmpty() ? "123456" : user.getPassword()));
        user.setStatus(user.getStatus() == null ? 1 : user.getStatus());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        assignRoles(user.getId(), Collections.singletonList(user.getRole()));
        return ApiResult.success("新增成功", null);
    }

    @PostMapping("/batch")
    @Loggable(module = "用户管理", operationType = "批量新增", operationDesc = "批量新增用户")
    public ApiResult<Map<String, Object>> batchAdd(@RequestBody List<SysUser> users) {
        if (users == null || users.isEmpty()) throw new BusinessException("批量新增数据不能为空");
        int success = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < users.size(); i++) {
            SysUser u = users.get(i);
            int rowNo = i + 1;
            try {
                validateRequired(u.getEmployeeNo(), "工号不能为空");
                if (u.getUsername() == null || u.getUsername().trim().isEmpty()) {
                    u.setUsername(u.getEmployeeNo());
                }
                validateRequired(u.getRealName(), "姓名不能为空");
                validateRequired(u.getRole(), "角色不能为空");
                checkRoleValid(u.getRole());
                if (!seen.add(u.getEmployeeNo())) throw new BusinessException("工号重复（批次内重复）");
                checkEmployeeNoUnique(u.getEmployeeNo(), null);
                if (u.getPhone() != null && !u.getPhone().trim().isEmpty() && !PHONE_PATTERN.matcher(u.getPhone()).matches()) {
                    throw new BusinessException("手机号格式不正确");
                }
                u.setPassword(passwordEncoder.encode(u.getPassword() == null || u.getPassword().trim().isEmpty() ? "123456" : u.getPassword()));
                u.setStatus(u.getStatus() == null ? 1 : u.getStatus());
                u.setCreateTime(LocalDateTime.now());
                u.setUpdateTime(LocalDateTime.now());
                userMapper.insert(u);
                assignRoles(u.getId(), Collections.singletonList(u.getRole()));
                success++;
            } catch (Exception e) {
                Map<String, Object> err = new HashMap<>();
                err.put("rowNo", rowNo);
                err.put("employeeNo", u.getEmployeeNo());
                err.put("message", e.getMessage());
                errors.add(err);
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", success);
        result.put("failCount", users.size() - success);
        result.put("errors", errors);
        return ApiResult.success(result);
    }

    @PostMapping("/import")
    @Loggable(module = "用户管理", operationType = "Excel导入", operationDesc = "Excel批量导入用户")
    public ApiResult<Map<String, Object>> importExcel(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请上传Excel文件");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new BusinessException("仅支持 .xls/.xlsx 文件");
        }
        List<SysUser> rows = parseExcel(file);
        return batchAdd(rows);
    }

    @PutMapping("/{id}")
    @Loggable(module = "用户管理", operationType = "修改", operationDesc = "编辑用户")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated SysUser req) {
        validateRequired(req.getEmployeeNo(), "工号不能为空");
        if (req.getUsername() == null || req.getUsername().trim().isEmpty()) {
            req.setUsername(req.getEmployeeNo());
        }
        validateRequired(req.getRealName(), "姓名不能为空");
        validateRequired(req.getRole(), "角色不能为空");
        checkRoleValid(req.getRole());
        checkEmployeeNoUnique(req.getEmployeeNo(), id);
        if (req.getPhone() != null && !req.getPhone().trim().isEmpty() && !PHONE_PATTERN.matcher(req.getPhone()).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        req.setId(id);
        req.setUpdateTime(LocalDateTime.now());
        req.setPassword(null);
        userMapper.updateById(req);
        assignRoles(id, Collections.singletonList(req.getRole()));
        return ApiResult.success("修改成功", null);
    }

    @DeleteMapping("/{id}")
    @Loggable(module = "用户管理", operationType = "删除", operationDesc = "删除用户")
    public ApiResult<Void> delete(@PathVariable Long id) {
        userMapper.deleteById(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, id));
        return ApiResult.success("删除成功", null);
    }

    @PutMapping("/{id}/reset-password")
    @Loggable(module = "用户管理", operationType = "重置密码", operationDesc = "重置用户密码")
    public ApiResult<Void> resetPwd(@PathVariable Long id) {
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(passwordEncoder.encode("123456Aa"));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResult.success("重置成功，默认密码为123456Aa", null);
    }

    @PutMapping("/{id}/status")
    @Loggable(module = "用户管理", operationType = "状态变更", operationDesc = "启用/禁用用户")
    public ApiResult<Void> status(@PathVariable Long id, @RequestBody SysUser req) {
        if (req.getStatus() == null) throw new BusinessException("状态不能为空");
        SysUser user = new SysUser();
        user.setId(id);
        user.setStatus(req.getStatus());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return ApiResult.success("状态更新成功", null);
    }

    @PutMapping("/{id}/roles")
    @Loggable(module = "用户管理", operationType = "分配角色", operationDesc = "分配用户角色")
    public ApiResult<Void> assignRolesApi(@PathVariable Long id, @RequestBody Map<String, List<String>> body) {
        List<String> roles = body.get("roles");
        if (roles == null || roles.isEmpty()) throw new BusinessException("请至少选择一个角色");
        assignRoles(id, roles);
        return ApiResult.success("角色分配成功", null);
    }

    @GetMapping("/list-by-role")
    @PreAuthorize("hasAnyRole('ADMIN','MAINTAINER')")
    public ApiResult<List<SysUser>> listByRole(@RequestParam String role) {
        SysRole roleEntity = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, role));
        if (roleEntity == null) return ApiResult.success(Collections.emptyList());
        List<Long> userIds = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getRoleId, roleEntity.getId()))
                .stream().map(UserRole::getUserId).collect(Collectors.toList());
        if (userIds.isEmpty()) return ApiResult.success(Collections.emptyList());
        return ApiResult.success(userMapper.selectList(new LambdaQueryWrapper<SysUser>().in(SysUser::getId, userIds).eq(SysUser::getStatus, 1)));
    }

    private void assignRoles(Long userId, List<String> roleCodes) {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleCode, roleCodes).eq(SysRole::getStatus, 1));
        if (roles.size() != new HashSet<>(roleCodes).size()) {
            throw new BusinessException("存在不合法角色");
        }
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));
        for (SysRole role : roles) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(role.getId());
            ur.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(ur);
        }
    }

    private List<Map<String, Object>> buildUserRows(List<SysUser> users) {
        if (users == null || users.isEmpty()) return new ArrayList<>();
        List<Long> userIds = users.stream().map(SysUser::getId).collect(Collectors.toList());
        Map<Long, List<String>> roleMap = new HashMap<>();
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().in(UserRole::getUserId, userIds));
        if (!userRoles.isEmpty()) {
            Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
            Map<Long, String> roleIdCodeMap = roleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds))
                    .stream().collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));
            for (UserRole ur : userRoles) {
                roleMap.computeIfAbsent(ur.getUserId(), k -> new ArrayList<>()).add(roleIdCodeMap.get(ur.getRoleId()));
            }
        }

        Map<Long, List<String>> bindMap = new HashMap<>();
        List<ThirdPartyBind> binds = thirdPartyBindMapper.selectList(new LambdaQueryWrapper<ThirdPartyBind>()
                .in(ThirdPartyBind::getUserId, userIds)
                .eq(ThirdPartyBind::getStatus, 1));
        for (ThirdPartyBind bind : binds) {
            bindMap.computeIfAbsent(bind.getUserId(), k -> new ArrayList<>()).add(bind.getPlatform());
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", user.getId());
            row.put("employeeNo", user.getEmployeeNo());
            row.put("username", user.getUsername());
            row.put("realName", user.getRealName());
            row.put("roles", roleMap.getOrDefault(user.getId(), new ArrayList<>()));
            row.put("phone", user.getPhone());
            row.put("email", user.getEmail());
            row.put("department", user.getDepartment());
            row.put("status", user.getStatus());
            row.put("createTime", user.getCreateTime());
            row.put("lastLoginTime", user.getLastLoginTime());
            row.put("thirdPartyBinds", bindMap.getOrDefault(user.getId(), new ArrayList<>()));
            rows.add(row);
        }
        return rows;
    }

    private List<SysUser> parseExcel(MultipartFile file) {
        try (InputStream is = file.getInputStream(); Workbook wb = new XSSFWorkbook(is)) {
            Sheet sheet = wb.getSheetAt(0);
            if (sheet == null) throw new BusinessException("Excel内容为空");
            List<SysUser> list = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                SysUser user = new SysUser();
                String employeeNo = text(row.getCell(0));
                String username = text(row.getCell(1));
                String realName = text(row.getCell(2));
                String role = text(row.getCell(3));
                String phone = text(row.getCell(4));
                String email = text(row.getCell(5));
                String department = text(row.getCell(6));
                String status = text(row.getCell(7));

                user.setEmployeeNo(employeeNo);
                user.setUsername((username == null || username.isEmpty()) ? employeeNo : username);
                user.setRealName(realName);
                user.setRole(role);
                user.setPhone(phone);
                user.setEmail(email);
                user.setDepartment(department);
                user.setStatus("0".equals(status) ? 0 : 1);
                list.add(user);
            }
            return list;
        } catch (IOException e) {
            throw new BusinessException("读取Excel失败");
        }
    }

    private String text(Cell cell) {
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
    }

    private void validateRequired(String value, String msg) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(msg);
    }

    private void checkEmployeeNoUnique(String employeeNo, Long id) {
        if (employeeNo == null || employeeNo.trim().isEmpty()) throw new BusinessException("工号不能为空");
        Long existsCount = userMapper.countByEmployeeNo(employeeNo, id);
        if (existsCount != null && existsCount > 0) {
            throw new BusinessException("工号已存在");
        }
    }

    private void checkRoleValid(String roleCode) {
        if (roleCode == null || roleCode.trim().isEmpty()) throw new BusinessException("角色不能为空");
        SysRole role = roleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
        if (role == null) throw new BusinessException("角色不合法：" + roleCode);
    }

}

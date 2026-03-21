package com.jou.networkrepair.module.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.constant.Loggable;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

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

    @PostMapping
    @Loggable(module = "通用", operationType = "新增", operationDesc = "新增数据")
    public ApiResult<Void> add(@RequestBody @Validated SysUser user) {
        checkEmployeeNoUnique(user.getEmployeeNo(), null);
        user.setPassword(passwordEncoder.encode(user.getPassword() == null ? "123456" : user.getPassword()));
        user.setCreateTime(LocalDateTime.now()); user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return ApiResult.success("新增成功", null);
    }

    @PutMapping("/{id}")
    @Loggable(module = "通用", operationType = "修改", operationDesc = "修改数据")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody @Validated SysUser req) {
        checkEmployeeNoUnique(req.getEmployeeNo(), id);
        req.setId(id); req.setUpdateTime(LocalDateTime.now()); req.setPassword(null);
        userMapper.updateById(req);
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

    @PostMapping("/import")
    public ApiResult<String> importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new BusinessException("导入文件不能为空");
        int success = 0;
        int skip = 0;
        Set<String> roleSet = new HashSet<>(Arrays.asList("admin", "user", "maintainer"));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.contains("employeeNo")) continue;
                String[] arr = line.split(",", -1);
                if (arr.length < 5) { skip++; continue; }
                String employeeNo = arr[0].trim();
                String username = arr[1].trim();
                String realName = arr[2].trim();
                String role = arr[3].trim();
                String phone = arr[4].trim();
                String email = arr.length > 5 ? arr[5].trim() : null;
                if (employeeNo.isEmpty() || username.isEmpty() || realName.isEmpty() || !roleSet.contains(role)) { skip++; continue; }
                if (!phone.isEmpty() && !phone.matches("^1\\d{10}$")) { skip++; continue; }
                if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo)) != null) { skip++; continue; }
                if (userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) != null) { skip++; continue; }
                SysUser user = new SysUser();
                user.setEmployeeNo(employeeNo);
                user.setUsername(username);
                user.setRealName(realName);
                user.setRole(role);
                user.setPhone(phone.isEmpty() ? null : phone);
                user.setEmail(email == null || email.isEmpty() ? null : email);
                user.setStatus(1);
                user.setPassword(passwordEncoder.encode("123456"));
                user.setCreateTime(LocalDateTime.now());
                user.setUpdateTime(LocalDateTime.now());
                userMapper.insert(user);
                success++;
            }
        }
        return ApiResult.success("导入完成：成功 " + success + " 条，跳过 " + skip + " 条", null);
    }

    @GetMapping("/import/template")
    public ResponseEntity<String> importTemplate() {
        String csv = "employeeNo,username,realName,role,phone,email\n" +
                "U2026999,testuser,测试用户,user,13800009999,test@campus.edu\n";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=user-import-template.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    private void checkEmployeeNoUnique(String employeeNo, Long id) {
        if (employeeNo == null || employeeNo.trim().isEmpty()) throw new BusinessException("工号不能为空");
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        if (exists != null && (id == null || !id.equals(exists.getId()))) {
            throw new BusinessException("工号已存在");
        }
    }
}

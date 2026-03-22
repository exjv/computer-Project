package com.jou.networkrepair.module.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.security.PermissionService;
import com.jou.networkrepair.common.utils.JwtUtil;
import com.jou.networkrepair.module.auth.dto.LoginDTO;
import com.jou.networkrepair.module.auth.dto.UpdatePasswordDTO;
import com.jou.networkrepair.module.auth.service.CaptchaService;
import com.jou.networkrepair.module.auth.vo.LoginVO;
import com.jou.networkrepair.module.log.entity.LoginLog;
import com.jou.networkrepair.module.log.entity.OperationLog;
import com.jou.networkrepair.module.log.mapper.LoginLogMapper;
import com.jou.networkrepair.module.log.mapper.OperationLogMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final CaptchaService captchaService;
    private final PermissionService permissionService;

    @GetMapping("/captcha")
    public ApiResult<Map<String, String>> captcha() {
        return ApiResult.success(captchaService.generate());
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@RequestBody @Validated LoginDTO dto, HttpServletRequest request) {
        if (!Arrays.asList("admin", "maintainer", "user").contains(dto.getRole())) {
            saveLoginLog(null, dto.getAccount(), "FAIL_ROLE_SELECT", "角色选择错误", request);
            throw new BusinessException("角色选择错误");
        }
        if (!captchaService.verify(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            saveLoginLog(null, dto.getAccount(), "FAIL_CAPTCHA", "验证码错误或已失效", request);
            throw new BusinessException("验证码错误或已失效");
        }
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .and(w -> w.eq(SysUser::getUsername, dto.getAccount()).or().eq(SysUser::getEmployeeNo, dto.getAccount())));
        if (user == null) {
            saveLoginLog(null, dto.getAccount(), "FAIL_ACCOUNT", "账号不存在", request);
            throw new BusinessException("账号不存在");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            saveLoginLog(user.getId(), user.getUsername(), "FAIL_PASSWORD", "密码错误", request);
            throw new BusinessException("密码错误");
        }
        if (!dto.getRole().equals(user.getRole())) {
            saveLoginLog(user.getId(), user.getUsername(), "FAIL_NON_ROLE_ACCOUNT", "非本角色账号禁止登录", request);
            throw new BusinessException("非本角色账号禁止登录");
        }
        if (user.getStatus() == 0) {
            saveLoginLog(user.getId(), user.getUsername(), "FAIL_DISABLED", "账号被禁用", request);
            throw new BusinessException("账号被禁用");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(user.getId(), user.getUsername(), "SUCCESS", null, request);
        return ApiResult.success(new LoginVO(
                jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()),
                user.getRole(),
                user.getUsername(),
                user.getEmployeeNo(),
                permissionService.getRoleCodes(user.getId(), user.getRole()),
                permissionService.getPermissionSet(user.getId(), user.getRole())
        ));
    }

    @GetMapping("/oauth/{provider}/url")
    public ApiResult<Map<String, String>> oauthUrl(@PathVariable String provider) {
        if (!"wechat".equals(provider) && !"qq".equals(provider)) throw new BusinessException("不支持的第三方平台");
        Map<String, String> result = new HashMap<>();
        result.put("provider", provider);
        result.put("authorizeUrl", "/api/auth/oauth/" + provider + "/callback?code=demo-code");
        result.put("tip", "当前为企业级预留接口，待接入真实OAuth");
        return ApiResult.success(result);
    }

    @GetMapping("/userInfo")
    public ApiResult<Map<String, Object>> userInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId()); map.put("username", user.getUsername()); map.put("realName", user.getRealName());
        map.put("employeeNo", user.getEmployeeNo());
        map.put("phone", user.getPhone()); map.put("email", user.getEmail()); map.put("role", user.getRole());
        map.put("roles", permissionService.getRoleCodes(user.getId(), user.getRole()));
        map.put("permissions", permissionService.getPermissionSet(user.getId(), user.getRole()));
        Map<String, Object> routePermissions = new HashMap<>();
        routePermissions.put("canManageUser", permissionService.hasPermission("user:manage"));
        routePermissions.put("canManageRole", permissionService.hasPermission("role:manage"));
        routePermissions.put("canManageDevice", permissionService.hasPermission("device:manage"));
        routePermissions.put("canViewLogs", permissionService.hasPermission("log:operation:view"));
        routePermissions.put("canApproveOrder", permissionService.hasPermission("repair:order:approve"));
        map.put("routePermissions", routePermissions);
        return ApiResult.success(map);
    }

    @GetMapping("/employee-no/check")
    public ApiResult<Map<String, Object>> checkEmployeeNo(@RequestParam String employeeNo,
                                                          @RequestParam(required = false) Long excludeUserId) {
        SysUser exists = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmployeeNo, employeeNo));
        boolean available = exists == null || (excludeUserId != null && excludeUserId.equals(exists.getId()));
        Map<String, Object> map = new HashMap<>();
        map.put("employeeNo", employeeNo);
        map.put("available", available);
        map.put("userId", exists == null ? null : exists.getId());
        return ApiResult.success(map);
    }

    @PutMapping("/updatePassword")
    public ApiResult<Void> updatePassword(@RequestBody @Validated UpdatePasswordDTO body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userMapper.selectById(userId);
        if (!captchaService.verify(body.getCaptchaKey(), body.getCaptchaCode())) {
            recordPasswordChangeLog(user, "FAIL_CAPTCHA", "验证码错误或已失效", request);
            throw new BusinessException("验证码错误或已失效");
        }
        if (!body.getNewPassword().equals(body.getConfirmNewPassword())) {
            recordPasswordChangeLog(user, "FAIL_CONFIRM", "两次新密码不一致", request);
            throw new BusinessException("两次新密码不一致");
        }
        if (body.getNewPassword().length() < 8) {
            recordPasswordChangeLog(user, "FAIL_STRENGTH", "新密码长度不能小于8位", request);
            throw new BusinessException("新密码长度不能小于8位");
        }
        if (!body.getNewPassword().matches("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d\\W_]{8,64}$")) {
            recordPasswordChangeLog(user, "FAIL_STRENGTH", "新密码必须包含字母和数字", request);
            throw new BusinessException("新密码必须包含字母和数字");
        }
        if (body.getOldPassword().equals(body.getNewPassword())) {
            recordPasswordChangeLog(user, "FAIL_REPEAT", "新旧密码不能一致", request);
            throw new BusinessException("新旧密码不能一致");
        }
        if (!passwordEncoder.matches(body.getOldPassword(), user.getPassword())) {
            recordPasswordChangeLog(user, "FAIL_OLD_PASSWORD", "旧密码错误", request);
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(body.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        recordPasswordChangeLog(user, "SUCCESS", "密码修改成功", request);
        return ApiResult.success("修改成功", null);
    }

    @PutMapping("/updateProfile")
    public ApiResult<Void> updateProfile(@RequestBody SysUser req, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userMapper.selectById(userId);
        user.setRealName(req.getRealName()); user.setPhone(req.getPhone()); user.setEmail(req.getEmail());
        userMapper.updateById(user);
        return ApiResult.success("修改成功", null);
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout() { return ApiResult.success("退出成功", null); }

    private void saveLoginLog(Long userId, String username, String loginStatus, String failReason, HttpServletRequest request) {
        LoginLog log = new LoginLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setIp(request.getRemoteAddr());
        log.setUserAgent(request.getHeader("User-Agent"));
        log.setLoginStatus(loginStatus);
        log.setFailReason(failReason);
        log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    private void recordPasswordChangeLog(SysUser user, String result, String desc, HttpServletRequest request) {
        OperationLog log = new OperationLog();
        log.setUserId(user == null ? null : user.getId());
        log.setUsername(user == null ? null : user.getUsername());
        log.setModule("安全中心");
        log.setOperationType("修改密码");
        log.setOperationDesc(desc);
        log.setRequestMethod(request.getMethod());
        log.setRequestUrl(request.getRequestURI());
        log.setRequestParams("result=" + result);
        log.setResponseCode("SUCCESS".equals(result) ? "200" : "400");
        log.setTraceId("PWD" + System.currentTimeMillis());
        log.setIp(request.getRemoteAddr());
        log.setOperationTime(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}

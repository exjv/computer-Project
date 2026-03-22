package com.jou.networkrepair.module.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.security.RbacPermissionService;
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
import com.jou.networkrepair.module.v2.auth2.entity.ThirdPartyBind;
import com.jou.networkrepair.module.v2.auth2.mapper.ThirdPartyBindMapper;
import com.jou.networkrepair.module.v2.rbac.entity.Role;
import com.jou.networkrepair.module.v2.rbac.entity.UserRole;
import com.jou.networkrepair.module.v2.rbac.mapper.RoleMapper;
import com.jou.networkrepair.module.v2.rbac.mapper.UserRoleMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private static final Pattern PASSWORD_STRENGTH = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final LoginLogMapper loginLogMapper;
    private final OperationLogMapper operationLogMapper;
    private final CaptchaService captchaService;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final RbacPermissionService rbacPermissionService;
    private final ThirdPartyBindMapper thirdPartyBindMapper;

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
        List<String> roleCodes = queryRoleCodes(user);
        String selectedRole = rbacPermissionService.normalizeRole(dto.getRole());
        if (!roleCodes.contains(selectedRole)) {
            saveLoginLog(user.getId(), user.getUsername(), "FAIL_ROLE", request.getRemoteAddr());
            throw new BusinessException("角色选择错误或无权限使用该入口");
        }
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(user.getId(), user.getUsername(), "SUCCESS", request.getRemoteAddr());
        return ApiResult.success(new LoginVO(jwtUtil.generateToken(user.getId(), user.getUsername(), selectedRole, roleCodes), selectedRole, user.getUsername()));
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
        List<String> roleCodes = queryRoleCodes(user);
        Set<String> permissions = rbacPermissionService.permissionsByRoles(roleCodes);
        List<ThirdPartyBind> binds = thirdPartyBindMapper.selectList(new LambdaQueryWrapper<ThirdPartyBind>()
                .eq(ThirdPartyBind::getUserId, userId)
                .eq(ThirdPartyBind::getStatus, 1));
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId()); map.put("username", user.getUsername()); map.put("realName", user.getRealName());
        map.put("phone", user.getPhone()); map.put("email", user.getEmail());
        map.put("role", request.getAttribute("role"));
        map.put("roles", roleCodes);
        map.put("permissions", permissions);
        map.put("thirdPartyBinds", binds.stream().map(ThirdPartyBind::getPlatform).collect(Collectors.toList()));
        return ApiResult.success(map);
    }

    @PutMapping("/updatePassword")
    public ApiResult<Void> updatePassword(@RequestBody @Validated UpdatePasswordDTO body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String ip = request.getRemoteAddr();
        if (userId == null) {
            saveLoginLog(null, "UNKNOWN", "FAIL_PWD_ILLEGAL_USER", ip);
            throw new BusinessException("非法请求，请重新登录后再试");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            saveLoginLog(userId, "UNKNOWN", "FAIL_PWD_USER_NOT_FOUND", ip);
            throw new BusinessException("用户不存在，无法修改密码");
        }

        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        String confirmPassword = body.get("confirmPassword");
        String captchaKey = body.get("captchaKey");
        String captchaCode = body.get("captchaCode");

        if (isBlank(oldPassword) || isBlank(newPassword) || isBlank(confirmPassword)) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_PARAM_EMPTY", ip);
            throw new BusinessException("旧密码、新密码、确认密码均不能为空");
        }
        if (isBlank(captchaKey) || isBlank(captchaCode)) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_CAPTCHA_EMPTY", ip);
            throw new BusinessException("验证码不能为空");
        }
        if (!captchaService.verify(captchaKey, captchaCode)) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_CAPTCHA", ip);
            throw new BusinessException("验证码错误或已失效");
        }
        if (!newPassword.equals(confirmPassword)) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_CONFIRM", ip);
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (!PASSWORD_STRENGTH.matcher(newPassword).matches()) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_WEAK", ip);
            throw new BusinessException("密码强度不足：至少8位且包含字母和数字");
        }
        if (oldPassword.equals(newPassword)) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_SAME", ip);
            throw new BusinessException("新旧密码不能一致");
        }
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            saveLoginLog(userId, user.getUsername(), "FAIL_PWD_OLD", ip);
            throw new BusinessException("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(userId, user.getUsername(), "SUCCESS_PWD_UPDATE", ip);
        return ApiResult.success("密码修改成功", null);
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private List<String> queryRoleCodes(SysUser user) {
        if (user == null) return new ArrayList<>();
        List<UserRole> userRoles = userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, user.getId()));
        Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).collect(Collectors.toSet());
        Set<String> roleCodes = new HashSet<>();
        if (!roleIds.isEmpty()) {
            List<Role> roles = roleMapper.selectList(new LambdaQueryWrapper<Role>().in(Role::getId, roleIds));
            roleCodes.addAll(roles.stream().map(Role::getRoleCode).map(rbacPermissionService::normalizeRole).collect(Collectors.toSet()));
        }
        if (roleCodes.isEmpty() && user.getRole() != null && !user.getRole().trim().isEmpty()) {
            roleCodes.add(rbacPermissionService.normalizeRole(user.getRole()));
        }
        return new ArrayList<>(roleCodes);
    }
}

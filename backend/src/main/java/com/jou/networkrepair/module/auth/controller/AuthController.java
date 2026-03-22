package com.jou.networkrepair.module.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.JwtUtil;
import com.jou.networkrepair.module.auth.dto.LoginDTO;
import com.jou.networkrepair.module.auth.service.CaptchaService;
import com.jou.networkrepair.module.auth.vo.LoginVO;
import com.jou.networkrepair.module.log.entity.LoginLog;
import com.jou.networkrepair.module.log.mapper.LoginLogMapper;
import com.jou.networkrepair.module.user.entity.SysUser;
import com.jou.networkrepair.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
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
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public ApiResult<Map<String, String>> captcha() {
        return ApiResult.success(captchaService.generate());
    }

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@RequestBody @Validated LoginDTO dto, HttpServletRequest request) {
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
            saveLoginLog(user.getId(), user.getUsername(), "FAIL_ROLE", "角色选择错误或无权限使用该入口", request);
            throw new BusinessException("角色选择错误或无权限使用该入口");
        }
        if (user.getStatus() == 0) throw new BusinessException("账号已禁用");
        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);
        saveLoginLog(user.getId(), user.getUsername(), "SUCCESS", null, request);
        return ApiResult.success(new LoginVO(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()), user.getRole(), user.getUsername()));
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
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId()); map.put("username", user.getUsername()); map.put("realName", user.getRealName());
        map.put("phone", user.getPhone()); map.put("email", user.getEmail()); map.put("role", user.getRole());
        return ApiResult.success(map);
    }

    @PutMapping("/updatePassword")
    public ApiResult<Void> updatePassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        SysUser user = userMapper.selectById(userId);
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || oldPassword.trim().isEmpty()) throw new BusinessException("旧密码不能为空");
        if (newPassword == null || newPassword.trim().isEmpty()) throw new BusinessException("新密码不能为空");
        if (newPassword.length() < 6) throw new BusinessException("新密码长度不能小于6位");
        if (oldPassword.equals(newPassword)) throw new BusinessException("新旧密码不能一致");
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) throw new BusinessException("旧密码错误");
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
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
}

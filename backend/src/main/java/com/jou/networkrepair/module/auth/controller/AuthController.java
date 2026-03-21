package com.jou.networkrepair.module.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.common.utils.JwtUtil;
import com.jou.networkrepair.module.auth.dto.LoginDTO;
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

    @PostMapping("/login")
    public ApiResult<LoginVO> login(@RequestBody @Validated LoginDTO dto, HttpServletRequest request) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        String status = "SUCCESS";
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            status = "FAIL";
            saveLoginLog(null, dto.getUsername(), status, request.getRemoteAddr());
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) throw new BusinessException("账号已禁用");
        saveLoginLog(user.getId(), user.getUsername(), status, request.getRemoteAddr());
        return ApiResult.success(new LoginVO(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()), user.getRole(), user.getUsername()));
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

    private void saveLoginLog(Long userId, String username, String loginStatus, String ip) {
        LoginLog log = new LoginLog();
        log.setUserId(userId); log.setUsername(username); log.setIp(ip); log.setLoginStatus(loginStatus); log.setLoginTime(LocalDateTime.now());
        loginLogMapper.insert(log);
    }
}

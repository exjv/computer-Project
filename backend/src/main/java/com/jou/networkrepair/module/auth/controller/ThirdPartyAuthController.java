package com.jou.networkrepair.module.auth.controller;

import com.jou.networkrepair.common.api.ApiResult;
import com.jou.networkrepair.module.auth.dto.ThirdPartyBindDTO;
import com.jou.networkrepair.module.auth.service.ThirdPartyAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class ThirdPartyAuthController {

    private final ThirdPartyAuthService thirdPartyAuthService;

    /**
     * OAuth 回调占位接口：当前仅返回模拟 openId/unionId。
     */
    @GetMapping("/{provider}/callback")
    public ApiResult<Map<String, String>> callback(@PathVariable String provider, @RequestParam String code) {
        return ApiResult.success(thirdPartyAuthService.callback(provider, code));
    }

    /**
     * 绑定第三方账号（预留）。
     */
    @PostMapping("/{provider}/bind")
    public ApiResult<Void> bind(@PathVariable String provider, @RequestBody @Validated ThirdPartyBindDTO dto, HttpServletRequest request) {
        thirdPartyAuthService.bind((Long) request.getAttribute("userId"), provider, dto);
        return ApiResult.success("绑定成功（预留实现）", null);
    }

    /**
     * 解绑第三方账号（预留）。
     */
    @DeleteMapping("/{provider}/unbind")
    public ApiResult<Void> unbind(@PathVariable String provider, HttpServletRequest request) {
        thirdPartyAuthService.unbind((Long) request.getAttribute("userId"), provider);
        return ApiResult.success("解绑成功", null);
    }
}

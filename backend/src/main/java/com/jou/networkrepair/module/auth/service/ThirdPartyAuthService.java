package com.jou.networkrepair.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jou.networkrepair.common.exception.BusinessException;
import com.jou.networkrepair.module.auth.dto.ThirdPartyBindDTO;
import com.jou.networkrepair.module.v2.auth2.entity.ThirdPartyBind;
import com.jou.networkrepair.module.v2.auth2.mapper.ThirdPartyBindMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ThirdPartyAuthService {

    private final ThirdPartyBindMapper thirdPartyBindMapper;

    public Map<String, String> callback(String provider, String code) {
        validateProvider(provider);
        if (code == null || code.trim().isEmpty()) throw new BusinessException("第三方授权码不能为空");
        Map<String, String> map = new HashMap<>();
        map.put("provider", provider);
        map.put("openId", buildMockId(provider, code, "OPEN"));
        map.put("unionId", buildMockId(provider, code, "UNION"));
        map.put("tip", "当前为OAuth预留回调，后续接入真实授权平台");
        return map;
    }

    public void bind(Long userId, String provider, ThirdPartyBindDTO dto) {
        validateProvider(provider);
        if (userId == null) throw new BusinessException("未登录，无法绑定第三方账号");
        String openId = buildMockId(provider, dto.getCode(), "OPEN");
        String unionId = buildMockId(provider, dto.getCode(), "UNION");

        ThirdPartyBind exists = thirdPartyBindMapper.selectOne(new LambdaQueryWrapper<ThirdPartyBind>()
                .eq(ThirdPartyBind::getPlatform, provider.toUpperCase())
                .eq(ThirdPartyBind::getOpenId, openId));
        if (exists != null && !userId.equals(exists.getUserId())) {
            throw new BusinessException("该第三方账号已被其他用户绑定");
        }

        ThirdPartyBind myBind = thirdPartyBindMapper.selectOne(new LambdaQueryWrapper<ThirdPartyBind>()
                .eq(ThirdPartyBind::getUserId, userId)
                .eq(ThirdPartyBind::getPlatform, provider.toUpperCase()));

        if (myBind == null) {
            ThirdPartyBind bind = new ThirdPartyBind();
            bind.setUserId(userId);
            bind.setPlatform(provider.toUpperCase());
            bind.setOpenId(openId);
            bind.setUnionId(unionId);
            bind.setStatus(1);
            bind.setBindTime(LocalDateTime.now());
            bind.setCreateTime(LocalDateTime.now());
            bind.setUpdateTime(LocalDateTime.now());
            thirdPartyBindMapper.insert(bind);
        } else {
            myBind.setOpenId(openId);
            myBind.setUnionId(unionId);
            myBind.setStatus(1);
            myBind.setBindTime(LocalDateTime.now());
            myBind.setUpdateTime(LocalDateTime.now());
            thirdPartyBindMapper.updateById(myBind);
        }
    }

    public void unbind(Long userId, String provider) {
        validateProvider(provider);
        ThirdPartyBind bind = thirdPartyBindMapper.selectOne(new LambdaQueryWrapper<ThirdPartyBind>()
                .eq(ThirdPartyBind::getUserId, userId)
                .eq(ThirdPartyBind::getPlatform, provider.toUpperCase())
                .eq(ThirdPartyBind::getStatus, 1));
        if (bind == null) throw new BusinessException("未找到可解绑的第三方账号");
        bind.setStatus(0);
        bind.setUpdateTime(LocalDateTime.now());
        thirdPartyBindMapper.updateById(bind);
    }

    private void validateProvider(String provider) {
        if (!"wechat".equalsIgnoreCase(provider) && !"qq".equalsIgnoreCase(provider)) {
            throw new BusinessException("不支持的第三方平台");
        }
    }

    private String buildMockId(String provider, String code, String prefix) {
        String raw = provider.toUpperCase() + ":" + code + ":" + prefix;
        return prefix + "_" + UUID.nameUUIDFromBytes(raw.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
    }
}

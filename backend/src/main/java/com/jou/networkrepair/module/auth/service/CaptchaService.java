package com.jou.networkrepair.module.auth.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class CaptchaService {
    private static class CaptchaValue {
        private final String code;
        private final LocalDateTime expireAt;

        private CaptchaValue(String code, LocalDateTime expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, CaptchaValue> cache = new ConcurrentHashMap<>();

    public Map<String, String> generate() {
        String key = UUID.randomUUID().toString().replace("-", "");
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(1000, 9999));
        cache.put(key, new CaptchaValue(code, LocalDateTime.now().plusMinutes(5)));
        Map<String, String> result = new ConcurrentHashMap<>();
        result.put("captchaKey", key);
        result.put("captchaCode", code);
        result.put("expireTip", "验证码5分钟有效");
        return result;
    }

    public boolean verify(String key, String code) {
        CaptchaValue value = cache.get(key);
        if (value == null || LocalDateTime.now().isAfter(value.expireAt)) {
            cache.remove(key);
            return false;
        }
        cache.remove(key);
        return value.code.equalsIgnoreCase(code);
    }
}

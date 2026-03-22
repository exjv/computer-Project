package com.jou.networkrepair.module.auth.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
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
        result.put("captchaImage", buildSvgBase64(code));
        result.put("expireTip", "验证码5分钟有效");
        return result;
    }

    private String buildSvgBase64(String code) {
        int n1 = ThreadLocalRandom.current().nextInt(10, 90);
        int n2 = ThreadLocalRandom.current().nextInt(10, 90);
        int n3 = ThreadLocalRandom.current().nextInt(10, 90);
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='140' height='44'>"
                + "<rect width='140' height='44' fill='#f5f7fa'/>"
                + "<line x1='0' y1='" + n1 + "' x2='140' y2='" + (n1 / 2) + "' stroke='#dcdfe6'/>"
                + "<line x1='0' y1='" + n2 + "' x2='140' y2='" + (n2 / 2) + "' stroke='#e4e7ed'/>"
                + "<line x1='0' y1='" + n3 + "' x2='140' y2='" + (n3 / 2) + "' stroke='#ebeef5'/>"
                + "<text x='20' y='30' font-size='24' font-family='Arial' fill='#303133' letter-spacing='4'>" + code + "</text>"
                + "</svg>";
        return "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
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

package com.jou.networkrepair.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expire}")
    private Long expire;

    public String generateToken(Long userId, String username, String role, List<String> roles) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("username", username);
        map.put("role", role);
        map.put("roles", roles);
        Date now = new Date();
        return Jwts.builder().setClaims(map).setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expire * 1000))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    public Claims parseToken(String token) { return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody(); }
}

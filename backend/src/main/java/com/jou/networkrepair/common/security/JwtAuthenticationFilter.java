package com.jou.networkrepair.common.security;

import com.jou.networkrepair.common.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parseToken(auth.substring(7));
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                List<String> roles = claims.get("roles", List.class);
                if (roles == null || roles.isEmpty()) {
                    roles = new ArrayList<>();
                    roles.add(role);
                }
                List<GrantedAuthority> authorities = roles.stream()
                        .filter(r -> r != null && !String.valueOf(r).trim().isEmpty())
                        .map(r -> (GrantedAuthority) () -> "ROLE_" + String.valueOf(r).trim().toUpperCase())
                        .collect(Collectors.toList());
                UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                        username, null, authorities);
                token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(token);
                request.setAttribute("userId", ((Number) claims.get("userId")).longValue());
                request.setAttribute("role", role);
                request.setAttribute("roles", roles);
            } catch (Exception ignored) {}
        }
        chain.doFilter(request, response);
    }
}

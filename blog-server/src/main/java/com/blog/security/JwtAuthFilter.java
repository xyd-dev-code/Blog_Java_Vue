package com.blog.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtUtil jwtUtil;
    private final JwtBlacklist blacklist;

    public JwtAuthFilter(JwtUtil jwtUtil, JwtBlacklist blacklist) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        String token = jwtUtil.resolve(header);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // 先 parse 拿到 jti,再按 jti 查黑名单(跟 logout 时 revoke 的 key 一致)
                Claims c = jwtUtil.parse(token);
                String jti = c.getId();
                if (jti != null && blacklist.isRevoked(jti)) {
                    // 已撤销,直接放过去但不设认证上下文,后续 SecurityConfig 会 401
                    chain.doFilter(request, response);
                    return;
                }
                Long userId = Long.parseLong(c.getSubject());
                String username = c.get("username", String.class);
                String role = c.get("role", String.class);
                LoginUser login = new LoginUser(userId, username, role);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        login, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Invalid JWT: {}", ex.getClass().getSimpleName());
            }
        }
        chain.doFilter(request, response);
    }
}

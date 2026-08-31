package com.blog.security;

import io.jsonwebtoken.Claims;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, JwtBlacklist blacklist, UserMapper userMapper) {
        this.jwtUtil = jwtUtil;
        this.blacklist = blacklist;
        this.userMapper = userMapper;
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
                if (jti != null && !blacklist.isRevoked(jti)) {
                    Long userId = Long.parseLong(c.getSubject());
                    User user = userMapper.selectById(userId);
                    if (user != null && Integer.valueOf(1).equals(user.getStatus()) && jwtUtil.matchesCredentials(c, user)) {
                        LoginUser login = new LoginUser(userId, user.getUsername(), user.getRole());
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                login, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Invalid JWT: {}", ex.getClass().getSimpleName());
            }
        }
        chain.doFilter(request, response);
    }
}

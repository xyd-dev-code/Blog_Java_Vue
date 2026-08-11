package com.blog.security;

import com.blog.config.BlogProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    private final BlogProperties props;
    private final JwtAuthFilter jwtAuthFilter;
    private final boolean devProfile;

    public SecurityConfig(BlogProperties props,
                          JwtAuthFilter jwtAuthFilter,
                          @Value("${spring.profiles.active:dev}") String activeProfile) {
        this.props = props;
        this.jwtAuthFilter = jwtAuthFilter;
        // Swagger UI 暴露 API 全貌,生产上属于攻击面 — 只允许 dev profile 访问
        this.devProfile = "dev".equalsIgnoreCase(activeProfile)
                || activeProfile != null && activeProfile.toLowerCase().contains("dev");
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // cost=12(每登录约 250ms),离线下 ~25 倍慢于默认 10,目前 admin 登录量可承受
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration cfg = new CorsConfiguration();
        List<String> origins = props.getCors().getAllowedOrigins();
        if (origins != null) {
            origins.forEach(o -> {
                if (o == null || o.isBlank()) return;
                // 显式 origin,不退化成 pattern,避免 Origin: null / file:// 这类回退匹配
                cfg.addAllowedOrigin(o);
            });
        }
        cfg.addAllowedHeader("Authorization");
        cfg.addAllowedHeader("Content-Type");
        cfg.addAllowedHeader("X-Requested-With");
        cfg.addAllowedHeader("X-Forwarded-For");
        cfg.addAllowedHeader("X-Real-IP");
        cfg.addAllowedMethod(HttpMethod.GET);
        cfg.addAllowedMethod(HttpMethod.POST);
        cfg.addAllowedMethod(HttpMethod.PUT);
        cfg.addAllowedMethod(HttpMethod.DELETE);
        cfg.addAllowedMethod(HttpMethod.PATCH);
        cfg.addAllowedMethod(HttpMethod.OPTIONS);
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return new CorsFilter(src);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(c -> c.disable())
            .cors(c -> {})
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/site/**",
                        "/api/v1/home",
                        "/api/v1/ping",
                        "/error",
                        "/favicon.ico"
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET,
                        "/api/v1/articles/**",
                        "/api/v1/categories/**",
                        "/api/v1/tags/**",
                        "/api/v1/archives/**",
                        "/api/v1/projects/**",
                        "/api/v1/project-categories/**",
                        "/api/v1/friend-links/**",
                        "/api/v1/tools/**",
                        "/api/v1/tool-categories/**",
                        "/api/v1/subscribe/**"
                    ).permitAll()
                    .requestMatchers(HttpMethod.POST,
                        "/api/v1/comments",
                        "/api/v1/friend-links/apply",
                        "/api/v1/upload/avatar",
                        "/api/v1/share/**",
                        "/api/v1/subscribe"
                    ).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/comments/**").permitAll();
                if (devProfile) {
                    auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    ).permitAll();
                }
                auth.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                    .anyRequest().authenticated();
            })
            .exceptionHandling(eh -> eh
                // 匿名/未通过 JwtAuthFilter 校验 -> 401,前端收到会登出跳登录
                .authenticationEntryPoint((req, res, ex) -> {
                    res.setStatus(401);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\",\"data\":null}");
                })
                // 已登录但角色不够 -> 真 403,表示真没权限
                .accessDeniedHandler((req, res, ex) -> {
                    res.setStatus(403);
                    res.setContentType("application/json;charset=UTF-8");
                    res.getWriter().write("{\"code\":403,\"message\":\"无权限访问\",\"data\":null}");
                })
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

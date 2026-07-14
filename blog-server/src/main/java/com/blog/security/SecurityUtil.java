package com.blog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static LoginUser current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUser u)) return null;
        return u;
    }

    public static LoginUser require() {
        LoginUser u = current();
        if (u == null) throw new com.blog.common.BizException(401, "请先登录");
        return u;
    }
}

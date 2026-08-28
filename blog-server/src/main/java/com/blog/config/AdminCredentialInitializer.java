package com.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 用一次性环境变量初始化新库管理员密码。
 *
 * <p>init.sql 不再包含任何可登录的默认密码。只有当数据库密码仍为禁用标记时，
 * 本初始化器才会读取 BLOG_ADMIN_INITIAL_PASSWORD、写入 BCrypt 摘要；已有密码永不覆盖。</p>
 */
@Component
@Order(10)
public class AdminCredentialInitializer implements ApplicationRunner {
    public static final String DISABLED_PASSWORD = "!BOOTSTRAP_REQUIRED!";

    private static final Logger log = LoggerFactory.getLogger(AdminCredentialInitializer.class);

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String initialPassword;

    public AdminCredentialInitializer(UserMapper userMapper,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${BLOG_ADMIN_USERNAME:admin}") String username,
                                      @Value("${BLOG_ADMIN_INITIAL_PASSWORD:}") String initialPassword) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (admin == null || !DISABLED_PASSWORD.equals(admin.getPassword())) {
            return;
        }

        if (initialPassword == null || initialPassword.isBlank()) {
            log.warn("[AdminBootstrap] 管理员账号尚未设置密码。请通过 BLOG_ADMIN_INITIAL_PASSWORD 注入一次性强密码后重启。");
            return;
        }
        if (initialPassword.length() < 12) {
            throw new IllegalStateException("BLOG_ADMIN_INITIAL_PASSWORD 至少需要 12 个字符");
        }

        User update = new User();
        update.setId(admin.getId());
        update.setPassword(passwordEncoder.encode(initialPassword));
        userMapper.updateById(update);
        log.info("[AdminBootstrap] 管理员初始密码已写入数据库。请立即移除 BLOG_ADMIN_INITIAL_PASSWORD 环境变量。");
    }
}

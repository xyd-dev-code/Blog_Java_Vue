package com.blog.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 启动时幂等修复历史后台回复中的旧头像快照。
 *
 * <p>不新增数据库表或字段。旧部署升级后会自动把可识别的后台回复同步为
 * 当前管理员资料，避免旧头像文件删除后返回 404。</p>
 */
@Component
@Order(20)
public class AdminReplyProfileInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminReplyProfileInitializer.class);

    private final UserMapper userMapper;
    private final CommentService commentService;

    public AdminReplyProfileInitializer(UserMapper userMapper, CommentService commentService) {
        this.userMapper = userMapper;
        this.commentService = commentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            List<User> admins = userMapper.selectList(new LambdaQueryWrapper<User>()
                    .eq(User::getRole, "ADMIN")
                    .eq(User::getStatus, 1));
            int affected = 0;
            for (User admin : admins) {
                affected += commentService.syncAdminReplyProfile(admin, admin);
            }
            if (affected > 0) {
                log.info("[AdminReplyProfile] 已同步历史后台回复资料: count={}", affected);
            }
        } catch (Exception e) {
            // 公开接口仍会动态使用当前管理员头像；数据回填失败不阻止应用启动。
            log.warn("[AdminReplyProfile] 历史回复资料同步失败: errType={}",
                    e.getClass().getSimpleName());
        }
    }
}

package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.EmailSubscription;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface EmailSubscriptionMapper extends BaseMapper<EmailSubscription> {

    /**
     * 按邮箱精确查询，绕过 @TableLogic 逻辑删除过滤。
     * 用于订阅预检：已被退订/删除(deleted=1)的邮箱对 selectOne 不可见，
     * 若直接 INSERT 会撞唯一索引 uk_email 导致 500，故此处必须能看到“隐形”记录。
     */
    @Select("SELECT * FROM email_subscription WHERE email = #{email}")
    EmailSubscription selectRawByEmail(@Param("email") String email);

    /**
     * 按邮箱激活/重置订阅记录，绕过 @TableLogic（强制 deleted=0）。
     * 用于：待确认重发确认信、退订后重新订阅、离线直接确认。
     */
    @Update("UPDATE email_subscription SET status = #{status}, deleted = 0, token = #{token}, "
            + "source = #{source}, create_time = #{createTime}, confirm_time = #{confirmTime} "
            + "WHERE email = #{email}")
    int updateRawByEmail(@Param("email") String email,
                         @Param("status") int status,
                         @Param("token") String token,
                         @Param("source") String source,
                         @Param("createTime") LocalDateTime createTime,
                         @Param("confirmTime") LocalDateTime confirmTime);

    /**
     * 真删除（绕过 @TableLogic），用于后台“删除”操作（彻底移除记录）。
     * 区别于“退订”：退订仅把 status 置为已退订(2)并保留记录。
     */
    @Delete("DELETE FROM email_subscription WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}

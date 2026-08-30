package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 同步能够可靠识别的历史后台回复资料；不依赖新增的数据库字段。
     */
    @Update("""
            UPDATE comment
               SET nickname = #{newNickname},
                   email = #{newEmail},
                   avatar = #{newAvatar}
             WHERE parent_id > 0
               AND (ip IS NULL OR ip = '')
               AND (ua IS NULL OR ua = '')
               AND (
                    (#{oldEmail} <> '' AND email = #{oldEmail})
                    OR (#{oldNickname} <> '' AND nickname = #{oldNickname})
                    OR (#{username} <> '' AND nickname = #{username})
               )
            """)
    int syncAdminReplyProfile(@Param("oldEmail") String oldEmail,
                              @Param("oldNickname") String oldNickname,
                              @Param("username") String username,
                              @Param("newNickname") String newNickname,
                              @Param("newEmail") String newEmail,
                              @Param("newAvatar") String newAvatar);
}

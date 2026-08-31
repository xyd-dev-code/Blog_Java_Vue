package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @org.apache.ibatis.annotations.Select("""
            <script>
            SELECT parent_id AS parentId, COUNT(*) AS count FROM comment
            WHERE deleted = 0 AND target_type = #{type} AND article_id = #{articleId}
            <if test="!includePending">AND status = 1</if>
            AND parent_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY parent_id
            </script>
            """)
    java.util.List<com.blog.vo.CommentReplyCount> countReplies(@Param("ids") java.util.List<Long> ids,
            @Param("type") String type, @Param("articleId") Long articleId,
            @Param("includePending") boolean includePending);

    @org.apache.ibatis.annotations.Select("SELECT * FROM comment WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    Comment lockById(@Param("id") Long id);

    @Update("UPDATE comment SET like_count = GREATEST(0, COALESCE(like_count, 0) + #{delta}) WHERE id = #{id} AND deleted = 0")
    int adjustLikes(@Param("id") Long id, @Param("delta") int delta);

    @Update("UPDATE comment SET report_count = COALESCE(report_count, 0) + 1 WHERE id = #{id} AND deleted = 0")
    int incrementReports(@Param("id") Long id);

    @org.apache.ibatis.annotations.Select("""
            <script>
            SELECT article_id AS articleId, COUNT(*) AS count FROM comment
            WHERE target_type = 'ARTICLE' AND status = 1 AND deleted = 0 AND article_id IN
            <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
            GROUP BY article_id
            </script>
            """)
    java.util.List<com.blog.vo.ArticleCommentCount> countApprovedByArticles(@Param("ids") java.util.List<Long> ids);

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

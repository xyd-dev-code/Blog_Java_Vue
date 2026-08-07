package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.ShareLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShareLogMapper extends BaseMapper<ShareLog> {

    @Select("SELECT id FROM share_log WHERE article_id=#{articleId} AND channel=#{channel} " +
            "AND ip=#{ip} AND share_date=#{date} LIMIT 1")
    Long findDupe(@Param("articleId") Long articleId,
                  @Param("channel") String channel,
                  @Param("ip") String ip,
                  @Param("date") java.time.LocalDate date);

    @Update("UPDATE article SET share_count = share_count + 1 WHERE id = #{id}")
    int incrShareCount(@Param("id") Long id);

    @Select("SELECT share_count FROM article WHERE id = #{id}")
    Integer getShareCount(@Param("id") Long id);
}
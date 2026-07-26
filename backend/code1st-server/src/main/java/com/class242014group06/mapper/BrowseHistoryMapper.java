package com.class242014group06.mapper;

import com.class242014group06.entity.BrowseHistory;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface BrowseHistoryMapper {

    @Insert("INSERT INTO browse_history (user_id, user_role, target_type, target_id, target_title, target_cover, target_path, create_time) " +
            "VALUES (#{userId}, #{userRole}, #{targetType}, #{targetId}, #{targetTitle}, #{targetCover}, #{targetPath}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(BrowseHistory history);

    /**
     * 查询指定用户的浏览记录 — 按浏览时间倒序，同一天内同一目标去重只保留最新一次
     */
    @Select("SELECT bh.* FROM browse_history bh " +
            "WHERE bh.user_id = #{userId} AND bh.user_role = #{userRole} " +
            "AND bh.id IN (" +
            "  SELECT MAX(t.id) FROM browse_history t " +
            "  WHERE t.user_id = #{userId} AND t.user_role = #{userRole} " +
            "  GROUP BY t.target_type, t.target_id" +
            ") " +
            "ORDER BY bh.create_time DESC LIMIT 50")
    List<BrowseHistory> listByUser(@Param("userId") Long userId, @Param("userRole") String userRole);

    @Delete("DELETE FROM browse_history WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM browse_history WHERE user_id = #{userId} AND user_role = #{userRole}")
    int deleteAllByUser(@Param("userId") Long userId, @Param("userRole") String userRole);
}

package com.class242014group06.mapper;

import com.class242014group06.entity.FriendRelation;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface FriendRelationMapper {

    @Insert("INSERT INTO friend_relation (user_id, user_role, friend_id, friend_role, status, created_at, updated_at) " +
            "VALUES (#{userId}, #{userRole}, #{friendId}, #{friendRole}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(FriendRelation relation);

    @Select("SELECT * FROM friend_relation WHERE id = #{id}")
    FriendRelation getById(Long id);

    /** 查询两个用户之间的好友关系（不限方向） */
    @Select("SELECT * FROM friend_relation WHERE " +
            "(user_id = #{userId} AND friend_id = #{friendId}) " +
            "OR (user_id = #{friendId} AND friend_id = #{userId}) LIMIT 1")
    FriendRelation findRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    /** 更新好友关系状态 */
    @Update("UPDATE friend_relation SET status = #{status}, updated_at = NOW() WHERE id = #{id}")
    void updateStatus(@Param("id") Long id, @Param("status") Integer status);

    /** 查询用户的好友列表（已接受的） */
    List<FriendRelation> listFriends(@Param("userId") Long userId);

    /** 查询待处理的好友申请（别人发给我的） */
    List<FriendRelation> listPendingRequests(@Param("userId") Long userId);

    /** 查询我发出的好友申请 */
    List<FriendRelation> listMyRequests(@Param("userId") Long userId);

    /** 删除用户的所有好友关系（双向） */
    @Delete("DELETE FROM friend_relation WHERE user_id = #{userId} OR friend_id = #{userId}")
    void deleteByUser(@Param("userId") Long userId);

    /** 检查是否为好友 */
    @Select("SELECT COUNT(*) FROM friend_relation WHERE status = 1 AND " +
            "((user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId}))")
    int isFriend(@Param("userId") Long userId, @Param("friendId") Long friendId);
}

package com.class242014group06.mapper;

import com.class242014group06.entity.ChatConversation;
import com.class242014group06.entity.ChatMessage;
import com.class242014group06.entity.ChatParticipant;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMapper {

    // ==================== 会话 ====================

    @Insert("INSERT INTO chat_conversation (conversation_type, title, last_message, last_message_time, created_at, updated_at) " +
            "VALUES (#{conversationType}, #{title}, #{lastMessage}, #{lastMessageTime}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertConversation(ChatConversation conversation);

    @Select("SELECT * FROM chat_conversation WHERE id = #{id}")
    ChatConversation getConversationById(Long id);

    @Update("UPDATE chat_conversation SET last_message = #{lastMessage}, last_message_time = NOW(), updated_at = NOW() WHERE id = #{conversationId}")
    void updateLastMessage(@Param("conversationId") Long conversationId, @Param("lastMessage") String lastMessage);

    // ==================== 参与者 ====================

    @Insert("INSERT INTO chat_participant (conversation_id, user_id, user_role, user_name, joined_at) " +
            "VALUES (#{conversationId}, #{userId}, #{userRole}, #{userName}, NOW())")
    void insertParticipant(ChatParticipant participant);

    @Select("SELECT * FROM chat_participant WHERE conversation_id = #{conversationId}")
    List<ChatParticipant> getParticipantsByConversation(Long conversationId);

    /** 查找两个用户之间已有的私聊会话 */
    @Select("SELECT c.* FROM chat_conversation c " +
            "INNER JOIN chat_participant p1 ON c.id = p1.conversation_id AND p1.user_id = #{userId} AND p1.user_role = #{userRole} " +
            "INNER JOIN chat_participant p2 ON c.id = p2.conversation_id AND p2.user_id = #{otherId} AND p2.user_role = #{otherRole} " +
            "WHERE c.conversation_type = 'PRIVATE' LIMIT 1")
    ChatConversation findPrivateConversation(@Param("userId") Long userId, @Param("userRole") String userRole,
                                              @Param("otherId") Long otherId, @Param("otherRole") String otherRole);

    /** 查询用户的会话列表（带最后消息） */
    List<ChatConversation> listUserConversations(@Param("userId") Long userId, @Param("userRole") String userRole);

    /** 获取会话中对方参与者的信息 */
    ChatParticipant getOtherParticipant(@Param("conversationId") Long conversationId,
                                        @Param("userId") Long userId, @Param("userRole") String userRole);

    /** 获取会话中对方参与者的ID（一对一会话） */
    @Select("SELECT * FROM chat_participant WHERE conversation_id = #{conversationId} AND NOT (user_id = #{userId} AND user_role = #{userRole}) LIMIT 1")
    ChatParticipant getOtherParticipantAnno(@Param("conversationId") Long conversationId,
                                             @Param("userId") Long userId, @Param("userRole") String userRole);

    // ==================== 消息 ====================

    @Insert("INSERT INTO chat_message (conversation_id, sender_id, sender_role, sender_name, content, message_type, is_read, created_at) " +
            "VALUES (#{conversationId}, #{senderId}, #{senderRole}, #{senderName}, #{content}, #{messageType}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE conversation_id = #{conversationId} ORDER BY created_at ASC")
    List<ChatMessage> getMessagesByConversation(Long conversationId);

    /** 标记会话中对方发来的消息为已读 */
    @Update("UPDATE chat_message SET is_read = 1 WHERE conversation_id = #{conversationId} AND is_read = 0 AND NOT (sender_id = #{userId} AND sender_role = #{userRole})")
    void markAsRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("userRole") String userRole);

    /** 获取用户某会话的未读消息数 */
    @Select("SELECT COUNT(*) FROM chat_message WHERE conversation_id = #{conversationId} AND is_read = 0 AND NOT (sender_id = #{userId} AND sender_role = #{userRole})")
    int countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId, @Param("userRole") String userRole);

    /** 检查两个用户是否已有私聊会话 */
    @Select("SELECT COUNT(*) FROM chat_participant p1 " +
            "INNER JOIN chat_participant p2 ON p1.conversation_id = p2.conversation_id " +
            "INNER JOIN chat_conversation c ON c.id = p1.conversation_id AND c.conversation_type = 'PRIVATE' " +
            "WHERE p1.user_id = #{userId} AND p1.user_role = #{userRole} " +
            "AND p2.user_id = #{otherId} AND p2.user_role = #{otherRole}")
    int existsPrivateConversation(@Param("userId") Long userId, @Param("userRole") String userRole,
                                   @Param("otherId") Long otherId, @Param("otherRole") String otherRole);
}

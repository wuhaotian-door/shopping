package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.ChatSendDTO;
import com.class242014group06.entity.*;
import com.class242014group06.mapper.*;
import com.class242014group06.service.ChatService;
import com.class242014group06.vo.ChatMessageVO;
import com.class242014group06.vo.ConversationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private FriendRelationMapper friendRelationMapper;

    @Override
    public List<ConversationVO> listConversations(Long userId, String userRole) {
        List<ChatConversation> conversations = chatMapper.listUserConversations(userId, userRole);
        List<ConversationVO> result = new ArrayList<>();

        for (ChatConversation conv : conversations) {
            ChatParticipant other = chatMapper.getOtherParticipantAnno(conv.getId(), userId, userRole);
            int unread = chatMapper.countUnread(conv.getId(), userId, userRole);
            result.add(ConversationVO.builder()
                    .conversationId(conv.getId())
                    .otherUserId(other != null ? other.getUserId() : null)
                    .otherUserRole(other != null ? other.getUserRole() : null)
                    .otherUserName(other != null ? other.getUserName() : null)
                    .otherName(other != null ? other.getUserName() : null)
                    .lastMessage(conv.getLastMessage())
                    .lastMessageTime(conv.getLastMessageTime())
                    .unreadCount(unread)
                    .build());
        }
        return result;
    }

    @Override
    public List<ChatMessageVO> getMessages(Long conversationId, Long userId, String userRole) {
        // 标记已读
        chatMapper.markAsRead(conversationId, userId, userRole);

        List<ChatMessage> messages = chatMapper.getMessagesByConversation(conversationId);
        List<ChatMessageVO> result = new ArrayList<>();
        for (ChatMessage msg : messages) {
            result.add(ChatMessageVO.builder()
                    .id(msg.getId())
                    .conversationId(msg.getConversationId())
                    .senderId(msg.getSenderId())
                    .senderRole(msg.getSenderRole())
                    .senderName(msg.getSenderName())
                    .content(msg.getContent())
                    .messageType(msg.getMessageType())
                    .isRead(msg.getIsRead())
                    .createdAt(msg.getCreatedAt())
                    .mine(msg.getSenderId().equals(userId) && msg.getSenderRole().equals(userRole))
                    .build());
        }
        return result;
    }

    @Override
    @Transactional
    public ChatMessageVO sendMessage(Long senderId, String senderRole, ChatSendDTO dto) {
        String senderName = getUserName(senderId, senderRole);
        Long conversationId;

        if (dto.getConversationId() != null) {
            // 在已有会话中发送
            conversationId = dto.getConversationId();
        } else {
            // 查找或创建私聊会话
            Long receiverId = dto.getReceiverId();
            String receiverRole = dto.getReceiverRole();

            // 检查对话权限
            checkChatPermission(senderId, senderRole, receiverId, receiverRole);

            ChatConversation existing = chatMapper.findPrivateConversation(
                    senderId, senderRole, receiverId, receiverRole);
            if (existing != null) {
                conversationId = existing.getId();
            } else {
                // 创建新会话
                ChatConversation conv = ChatConversation.builder()
                        .conversationType("PRIVATE")
                        .build();
                chatMapper.insertConversation(conv);
                conversationId = conv.getId();

                // 添加两个参与者
                ChatParticipant p1 = ChatParticipant.builder()
                        .conversationId(conversationId).userId(senderId).userRole(senderRole).userName(senderName)
                        .build();
                ChatParticipant p2 = ChatParticipant.builder()
                        .conversationId(conversationId).userId(receiverId).userRole(receiverRole)
                        .userName(getUserName(receiverId, receiverRole))
                        .build();
                chatMapper.insertParticipant(p1);
                chatMapper.insertParticipant(p2);
            }
        }

        // 插入消息
        ChatMessage msg = ChatMessage.builder()
                .conversationId(conversationId).senderId(senderId)
                .senderRole(senderRole).senderName(senderName)
                .content(dto.getContent()).messageType("TEXT")
                .build();
        chatMapper.insertMessage(msg);

        // 更新会话最后消息
        String preview = dto.getContent().length() > 50 ? dto.getContent().substring(0, 50) + "..." : dto.getContent();
        chatMapper.updateLastMessage(conversationId, preview);

        return ChatMessageVO.builder()
                .id(msg.getId()).conversationId(conversationId)
                .senderId(senderId).senderRole(senderRole).senderName(senderName)
                .content(dto.getContent()).messageType("TEXT")
                .isRead(0).createdAt(msg.getCreatedAt()).mine(true)
                .build();
    }

    @Override
    public Object searchUserByUsername(Long searcherId, String searcherRole, String username) {
        // 先在 user 表中搜索
        User user = userMapper.getByUsername(username);
        if (user != null) {
            // 不能搜索自己
            if (searcherId.equals(user.getId()) && "USER".equals(searcherRole)) {
                return null;
            }
            int isFriend = 1;
            if ("USER".equals(searcherRole) && "USER".equals(searcherRole)) {
                isFriend = friendRelationMapper.isFriend(searcherId, user.getId());
            }
            Map<String, Object> result = new HashMap<>();
            result.put("id", user.getId());
            result.put("name", user.getName());
            result.put("username", user.getUsername());
            result.put("role", "USER");
            result.put("isFriend", isFriend);
            return result;
        }
        // 再在 inheritor 表中搜索
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("id", inheritor.getId());
            result.put("name", inheritor.getName());
            result.put("username", inheritor.getUsername());
            result.put("role", "INHERITOR");
            result.put("isFriend", 1); // 传承人与用户间可直接对话
            return result;
        }
        return null;
    }

    /** 检查聊天权限 */
    private void checkChatPermission(Long senderId, String senderRole, Long receiverId, String receiverRole) {
        // ADMIN 可以和任何人对话（双向）
        if ("ADMIN".equals(senderRole) || "ADMIN".equals(receiverRole)) {
            return;
        }
        // USER ↔ USER 需要好友关系
        if ("USER".equals(senderRole) && "USER".equals(receiverRole)) {
            if (friendRelationMapper.isFriend(senderId, receiverId) == 0) {
                throw new RuntimeException("需要先加为好友才能发起对话");
            }
        }
        // INHERITOR ↔ USER 可以直接对话
        // INHERITOR ↔ INHERITOR 可以直接对话
    }

    private String getUserName(Long userId, String role) {
        try {
            if ("USER".equals(role)) {
                User u = userMapper.getById(userId);
                return u != null ? u.getName() : "用户" + userId;
            } else if ("INHERITOR".equals(role)) {
                Inheritor i = inheritorMapper.getById(userId);
                return i != null ? i.getName() : "传承人" + userId;
            } else if ("ADMIN".equals(role)) {
                Admin a = adminMapper.getById(userId);
                return a != null ? a.getName() : "管理员" + userId;
            }
        } catch (Exception e) {
            log.warn("获取用户姓名失败: {}", e.getMessage());
        }
        return role + "_" + userId;
    }
}

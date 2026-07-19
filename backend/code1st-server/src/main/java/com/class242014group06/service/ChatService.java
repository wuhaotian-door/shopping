package com.class242014group06.service;

import com.class242014group06.dto.ChatSendDTO;
import com.class242014group06.vo.ChatMessageVO;
import com.class242014group06.vo.ConversationVO;

import java.util.List;

public interface ChatService {

    /** 获取用户的所有会话列表 */
    List<ConversationVO> listConversations(Long userId, String userRole);

    /** 获取会话的消息列表 */
    List<ChatMessageVO> getMessages(Long conversationId, Long userId, String userRole);

    /** 发送消息（自动创建或查找会话） */
    ChatMessageVO sendMessage(Long senderId, String senderRole, ChatSendDTO dto);

    /** 通过用户名搜索用户（在user和inheritor表中查找，用于开始新对话） */
    Object searchUserByUsername(Long searcherId, String searcherRole, String username);
}

package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO implements Serializable {
    private Long conversationId;
    /** 对方用户ID */
    private Long otherUserId;
    /** 对方用户角色 */
    private String otherUserRole;
    /** 对方用户名 */
    private String otherUserName;
    /** 对方姓名 */
    private String otherName;
    /** 最后一条消息预览 */
    private String lastMessage;
    /** 最后消息时间 */
    private LocalDateTime lastMessageTime;
    /** 未读消息数 */
    private Integer unreadCount;
}

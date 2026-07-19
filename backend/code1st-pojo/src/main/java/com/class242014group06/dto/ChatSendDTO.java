package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ChatSendDTO implements Serializable {
    /** 目标会话ID（已有会话则传此字段） */
    private Long conversationId;
    /** 接收方ID（新建会话时传此字段） */
    private Long receiverId;
    /** 接收方角色 */
    private String receiverRole;
    /** 消息内容 */
    private String content;
}

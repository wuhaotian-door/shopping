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
public class ChatMessageVO implements Serializable {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private String content;
    private String messageType;
    private Integer isRead;
    private LocalDateTime createdAt;
    /** 是否为当前用户发送 */
    private Boolean mine;
}

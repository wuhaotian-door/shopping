package com.class242014group06.entity;

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
public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderRole;
    private String senderName;
    private String content;
    private String messageType;
    private Integer isRead;
    private LocalDateTime createdAt;
}

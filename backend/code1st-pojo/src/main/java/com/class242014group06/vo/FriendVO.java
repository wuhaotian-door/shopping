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
public class FriendVO implements Serializable {
    /** 好友关系ID（用于接受/拒绝操作） */
    private Long relationId;
    private Long userId;
    private String userName;
    private String name;
    private String role;
    /** 好友关系状态: 0-待接受, 1-已接受, 2-已拒绝 */
    private Integer status;
    private LocalDateTime createdAt;
}

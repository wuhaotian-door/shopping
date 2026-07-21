package com.class242014group06.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FriendApplyDTO implements Serializable {
    /** 目标用户ID */
    private Long targetUserId;
    /** 目标用户角色 */
    private String targetRole;
}

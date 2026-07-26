package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户浏览记录 — 按 user_id + user_role 隔离
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrowseHistory {
    private Long id;
    private Long userId;
    private String userRole;
    private String targetType;   // product / news / project
    private Long targetId;
    private String targetTitle;
    private String targetCover;
    private String targetPath;
    private LocalDateTime createTime;
}

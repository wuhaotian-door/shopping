package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 页面浏览记录
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageView {
    private Long id;
    private String path;
    private String ip;
    private LocalDateTime createTime;
}

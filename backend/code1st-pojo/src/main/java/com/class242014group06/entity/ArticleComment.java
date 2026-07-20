package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 文章评论实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleComment implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 评论用户ID */
    private Long userId;

    /** 评论用户名 */
    private String userName;

    /** 评论内容 */
    private String content;

    /** 父评论ID，NULL表示顶级评论 */
    private Long parentId;

    /** 软删除标记：0正常 1已删除 */
    private Integer isDeleted;

    /** 创建时间 */
    private LocalDateTime createTime;
}
package com.class242014group06.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 非遗知识库文章实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 作者ID（传承人） */
    private Long authorId;

    /** 文章标题 */
    private String title;

    /** 分类 */
    private String category;

    /** 文章内容 */
    private String content;

    /** 封面图 */
    private String coverUrl;

    /** 状态：0-草稿 1-已发布 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}

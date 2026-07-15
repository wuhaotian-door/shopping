package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文章返回对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeArticleVO implements Serializable {

    private Long id;

    /** 作者ID */
    private Long authorId;

    /** 作者姓名 */
    private String authorName;

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
}

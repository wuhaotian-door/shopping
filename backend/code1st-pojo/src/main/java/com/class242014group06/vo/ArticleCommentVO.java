package com.class242014group06.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章评论返回视图
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /** 文章ID */
    private Long articleId;

    /** 评论用户ID */
    private Long userId;

    /** 评论用户角色：USER/INHERITOR/ADMIN */
    private String userRole;

    /** 评论用户名 */
    private String userName;

    /** 评论内容 */
    private String content;

    /** 父评论ID */
    private Long parentId;

    /** 被回复的用户名 */
    private String replyToUserName;

    /** 子评论列表 */
    private List<ArticleCommentVO> children;

    /** 软删除标记：0正常 1已删除 */
    private Integer isDeleted;

    /** 评论时间 */
    private LocalDateTime createTime;
}
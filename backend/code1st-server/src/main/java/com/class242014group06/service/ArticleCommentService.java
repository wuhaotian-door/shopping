package com.class242014group06.service;

import com.class242014group06.dto.ArticleCommentDTO;
import com.class242014group06.vo.ArticleCommentVO;

import java.util.List;

public interface ArticleCommentService {

    /**
     * 提交文章评论
     */
    void addComment(Long userId, Long articleId, ArticleCommentDTO dto);

    /**
     * 查询文章的所有评论（树形结构）
     */
    List<ArticleCommentVO> listByArticleId(Long articleId);

    /**
     * 统计文章评论数
     */
    Long countByArticleId(Long articleId);

    /**
     * 删除自己的评论（软删除）
     */
    void deleteComment(Long userId, Long commentId);
}
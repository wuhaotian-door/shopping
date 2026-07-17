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
     * 查询文章的所有评论
     */
    List<ArticleCommentVO> listByArticleId(Long articleId);

    /**
     * 统计文章评论数
     */
    Long countByArticleId(Long articleId);
}
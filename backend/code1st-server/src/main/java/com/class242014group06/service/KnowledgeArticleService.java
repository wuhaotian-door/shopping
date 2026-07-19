package com.class242014group06.service;

import com.class242014group06.dto.KnowledgeArticleDTO;
import com.class242014group06.vo.KnowledgeArticleVO;

import java.util.List;

public interface KnowledgeArticleService {

    /**
     * 创建文章
     */
    void create(Long authorId, KnowledgeArticleDTO dto);

    /**
     * 更新文章
     */
    void update(Long articleId, Long authorId, KnowledgeArticleDTO dto);

    /**
     * 删除文章
     */
    void delete(Long articleId, Long authorId);

    /**
     * 查询自己的文章列表
     */
    List<KnowledgeArticleVO> listByAuthor(Long authorId);

    /**
     * 查询所有已发布文章（首页展示）
     */
    List<KnowledgeArticleVO> listPublished();

    /**
     * 管理员查询所有文章（含未发布）
     */
    List<KnowledgeArticleVO> listAll();

    /**
     * 管理员删除任意文章
     */
    void deleteByAdmin(Long id);

    /**
     * 根据ID查询文章（公开）
     */
    KnowledgeArticleVO getById(Long id);
}

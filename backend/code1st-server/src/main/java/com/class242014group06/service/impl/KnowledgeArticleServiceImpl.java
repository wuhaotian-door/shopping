package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.dto.KnowledgeArticleDTO;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.KnowledgeArticle;
import com.class242014group06.exception.BaseException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.KnowledgeArticleMapper;
import com.class242014group06.service.KnowledgeArticleService;
import com.class242014group06.vo.KnowledgeArticleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeArticleServiceImpl implements KnowledgeArticleService {

    @Autowired
    private KnowledgeArticleMapper articleMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Override
    public void create(Long authorId, KnowledgeArticleDTO dto) {
        KnowledgeArticle article = new KnowledgeArticle();
        BeanUtils.copyProperties(dto, article);
        article.setAuthorId(authorId);
        article.setViewCount(0);
        if (article.getStatus() == null) {
            article.setStatus(0); // 默认草稿
        }
        articleMapper.insert(article);
    }

    @Override
    public void update(Long articleId, Long authorId, KnowledgeArticleDTO dto) {
        KnowledgeArticle existing = articleMapper.getById(articleId);
        if (existing == null) {
            throw new BaseException(MessageConstant.ARTICLE_NOT_FOUND);
        }
        if (!existing.getAuthorId().equals(authorId)) {
            throw new BaseException(MessageConstant.ARTICLE_NO_PERMISSION);
        }
        KnowledgeArticle article = new KnowledgeArticle();
        BeanUtils.copyProperties(dto, article);
        article.setId(articleId);
        articleMapper.update(article);
    }

    @Override
    public void delete(Long articleId, Long authorId) {
        KnowledgeArticle existing = articleMapper.getById(articleId);
        if (existing == null) {
            throw new BaseException(MessageConstant.ARTICLE_NOT_FOUND);
        }
        if (!existing.getAuthorId().equals(authorId)) {
            throw new BaseException(MessageConstant.ARTICLE_NO_PERMISSION);
        }
        articleMapper.deleteById(articleId);
    }

    @Override
    public List<KnowledgeArticleVO> listByAuthor(Long authorId) {
        List<KnowledgeArticle> articles = articleMapper.getByAuthorId(authorId);
        Inheritor author = inheritorMapper.getById(authorId);
        String authorName = author != null ? author.getName() : "";
        return articles.stream()
                .map(a -> toVO(a, authorName))
                .collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeArticleVO> listAll() {
        List<KnowledgeArticle> articles = articleMapper.listAll();
        return articles.stream()
                .map(a -> {
                    Inheritor author = inheritorMapper.getById(a.getAuthorId());
                    String authorName = author != null ? author.getName() : "";
                    return toVO(a, authorName);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByAdmin(Long id) {
        KnowledgeArticle existing = articleMapper.getById(id);
        if (existing == null) {
            throw new BaseException(MessageConstant.ARTICLE_NOT_FOUND);
        }
        articleMapper.deleteById(id);
    }

    @Override
    public void batchDelete(java.util.List<Long> ids) {
        for (Long id : ids) {
            deleteByAdmin(id);
        }
    }

    @Override
    public KnowledgeArticleVO getById(Long id) {
        KnowledgeArticle article = articleMapper.getById(id);
        if (article == null) {
            throw new BaseException(MessageConstant.ARTICLE_NOT_FOUND);
        }
        Inheritor author = inheritorMapper.getById(article.getAuthorId());
        String authorName = author != null ? author.getName() : "";
        return toVO(article, authorName);
    }

    @Override
    public List<KnowledgeArticleVO> listPublished() {
        List<KnowledgeArticle> articles = articleMapper.listPublished();
        return articles.stream()
                .map(a -> {
                    Inheritor author = inheritorMapper.getById(a.getAuthorId());
                    String authorName = author != null ? author.getName() : "";
                    return toVO(a, authorName);
                })
                .collect(Collectors.toList());
    }

    private KnowledgeArticleVO toVO(KnowledgeArticle article, String authorName) {
        return KnowledgeArticleVO.builder()
                .id(article.getId())
                .authorId(article.getAuthorId())
                .authorName(authorName)
                .title(article.getTitle())
                .category(article.getCategory())
                .content(article.getContent())
                .coverUrl(article.getCoverUrl())
                .status(article.getStatus())
                .viewCount(article.getViewCount())
                .createTime(article.getCreateTime())
                .build();
    }
}

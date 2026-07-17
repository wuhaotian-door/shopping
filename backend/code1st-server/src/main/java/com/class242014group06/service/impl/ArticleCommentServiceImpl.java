package com.class242014group06.service.impl;

import com.class242014group06.dto.ArticleCommentDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.ArticleComment;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.exception.OrderBusinessException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.ArticleCommentMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.ArticleCommentService;
import com.class242014group06.vo.ArticleCommentVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleCommentServiceImpl implements ArticleCommentService {

    @Autowired
    private ArticleCommentMapper commentMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public void addComment(Long userId, Long articleId, ArticleCommentDTO dto) {
        // 参数校验
        if (dto.getContent() == null || dto.getContent().trim().isEmpty()) {
            throw new OrderBusinessException("评论内容不能为空");
        }

        // 跨表查询用户名（支持普通用户/传承人/管理员评论）
        String userName = "";
        User user = userMapper.getById(userId);
        if (user != null) {
            userName = user.getName();
        } else {
            Inheritor inheritor = inheritorMapper.getById(userId);
            if (inheritor != null) {
                userName = inheritor.getName();
            } else {
                Admin admin = adminMapper.getById(userId);
                if (admin != null) {
                    userName = admin.getName();
                }
            }
        }

        ArticleComment comment = ArticleComment.builder()
                .articleId(articleId)
                .userId(userId)
                .userName(userName)
                .content(dto.getContent().trim())
                .createTime(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);
    }

    @Override
    public List<ArticleCommentVO> listByArticleId(Long articleId) {
        List<ArticleComment> comments = commentMapper.listByArticleId(articleId);
        return comments.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public Long countByArticleId(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }

    private ArticleCommentVO toVO(ArticleComment comment) {
        return ArticleCommentVO.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .content(comment.getContent())
                .createTime(comment.getCreateTime())
                .build();
    }
}
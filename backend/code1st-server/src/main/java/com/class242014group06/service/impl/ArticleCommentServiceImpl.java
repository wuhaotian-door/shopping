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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

        // 校验父评论（如果传了 parentId）
        if (dto.getParentId() != null) {
            ArticleComment parent = commentMapper.findById(dto.getParentId());
            if (parent == null || !parent.getArticleId().equals(articleId)) {
                throw new OrderBusinessException("父评论不存在");
            }
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
                .parentId(dto.getParentId())
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .build();
        commentMapper.insert(comment);
    }

    @Override
    public List<ArticleCommentVO> listByArticleId(Long articleId) {
        List<ArticleComment> comments = commentMapper.listByArticleId(articleId);
        List<ArticleCommentVO> flatVos = comments.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
        return buildTree(flatVos);
    }

    @Override
    public Long countByArticleId(Long articleId) {
        return commentMapper.countByArticleId(articleId);
    }

    @Override
    public void deleteComment(Long userId, Long commentId) {
        ArticleComment comment = commentMapper.findById(commentId);
        if (comment == null) {
            throw new OrderBusinessException("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new OrderBusinessException("无权删除他人评论");
        }
        commentMapper.deleteById(commentId);
    }

    /**
     * 将扁平评论列表构建为树形结构
     */
    private List<ArticleCommentVO> buildTree(List<ArticleCommentVO> flatList) {
        Map<Long, ArticleCommentVO> map = new LinkedHashMap<>();
        List<ArticleCommentVO> roots = new ArrayList<>();

        // 第一遍：建立 ID 映射并初始化 children
        for (ArticleCommentVO vo : flatList) {
            vo.setChildren(new ArrayList<>());
            map.put(vo.getId(), vo);
        }

        // 第二遍：设置 replyToUserName 并挂到父节点
        for (ArticleCommentVO vo : flatList) {
            if (vo.getParentId() != null) {
                ArticleCommentVO parent = map.get(vo.getParentId());
                if (parent != null) {
                    vo.setReplyToUserName(
                            parent.getIsDeleted() != null && parent.getIsDeleted() == 1
                                    ? "已删除用户"
                                    : parent.getUserName()
                    );
                    parent.getChildren().add(vo);
                } else {
                    // 父评论被删除或孤立，降级为顶级评论
                    roots.add(vo);
                }
            } else {
                roots.add(vo);
            }
        }
        return roots;
    }

    private ArticleCommentVO toVO(ArticleComment comment) {
        return ArticleCommentVO.builder()
                .id(comment.getId())
                .articleId(comment.getArticleId())
                .userId(comment.getUserId())
                .userName(comment.getUserName())
                .content(comment.getContent())
                .parentId(comment.getParentId())
                .isDeleted(comment.getIsDeleted())
                .createTime(comment.getCreateTime())
                .build();
        // replyToUserName 和 children 在 buildTree() 中设置
    }
}

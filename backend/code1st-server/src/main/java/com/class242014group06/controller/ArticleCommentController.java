package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ArticleCommentDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.ArticleCommentService;
import com.class242014group06.vo.ArticleCommentVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章评论管理
 */
@RestController
@RequestMapping("/admin/article-comment")
@Slf4j
@Api(tags = "文章评论接口")
public class ArticleCommentController {

    @Autowired
    private ArticleCommentService commentService;

    /**
     * 用户提交文章评论（需登录）
     */
    @PostMapping("/{articleId}")
    @ApiOperation("提交文章评论")
    public Result<String> addComment(@PathVariable Long articleId, @RequestBody ArticleCommentDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("用户提交文章评论，userId：{}，articleId：{}", userId, articleId);
        commentService.addComment(userId, articleId, dto);
        return Result.success("评论提交成功");
    }

    /**
     * 查询某文章的所有评论（公开）
     */
    @GetMapping("/public/{articleId}")
    @ApiOperation("查询文章评论列表（公开）")
    public Result<List<ArticleCommentVO>> listComments(@PathVariable Long articleId) {
        log.info("查询文章评论（公开），articleId：{}", articleId);
        List<ArticleCommentVO> comments = commentService.listByArticleId(articleId);
        return Result.success(comments);
    }

    /**
     * 获取文章评论数（公开）
     */
    @GetMapping("/public/{articleId}/count")
    @ApiOperation("获取文章评论数（公开）")
    public Result<Long> getCommentCount(@PathVariable Long articleId) {
        log.info("获取文章评论数（公开），articleId：{}", articleId);
        return Result.success(commentService.countByArticleId(articleId));
    }
}
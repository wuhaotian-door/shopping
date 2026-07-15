package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.KnowledgeArticleDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.KnowledgeArticleService;
import com.class242014group06.vo.KnowledgeArticleVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 非遗知识库文章管理
 */
@RestController
@RequestMapping("/admin/knowledge")
@Slf4j
@Api(tags = "非遗知识库接口")
public class KnowledgeArticleController {

    @Autowired
    private KnowledgeArticleService articleService;

    /**
     * 传承人创建文章
     */
    @PostMapping
    @ApiOperation("创建文章")
    public Result<String> create(@RequestBody KnowledgeArticleDTO dto) {
        Long authorId = BaseContext.getCurrentId();
        log.info("创建文章，authorId：{}", authorId);
        articleService.create(authorId, dto);
        return Result.success("文章创建成功");
    }

    /**
     * 传承人更新文章
     */
    @PutMapping("/{id}")
    @ApiOperation("更新文章")
    public Result<String> update(@PathVariable Long id, @RequestBody KnowledgeArticleDTO dto) {
        Long authorId = BaseContext.getCurrentId();
        log.info("更新文章，id：{}，authorId：{}", id, authorId);
        articleService.update(id, authorId, dto);
        return Result.success("文章更新成功");
    }

    /**
     * 传承人删除文章
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除文章")
    public Result<String> delete(@PathVariable Long id) {
        Long authorId = BaseContext.getCurrentId();
        log.info("删除文章，id：{}，authorId：{}", id, authorId);
        articleService.delete(id, authorId);
        return Result.success("文章已删除");
    }

    /**
     * 传承人查看自己的文章列表
     */
    @GetMapping("/my")
    @ApiOperation("查看自己的文章列表")
    public Result<List<KnowledgeArticleVO>> listMyArticles() {
        Long authorId = BaseContext.getCurrentId();
        log.info("查看自己的文章列表，authorId：{}", authorId);
        List<KnowledgeArticleVO> list = articleService.listByAuthor(authorId);
        return Result.success(list);
    }

    /**
     * 公开接口：查询所有已发布文章（首页展示）
     */
    @GetMapping("/public")
    @ApiOperation("查询所有已发布文章（公开）")
    public Result<List<KnowledgeArticleVO>> listPublished() {
        log.info("公开查询所有已发布文章");
        List<KnowledgeArticleVO> list = articleService.listPublished();
        return Result.success(list);
    }
}

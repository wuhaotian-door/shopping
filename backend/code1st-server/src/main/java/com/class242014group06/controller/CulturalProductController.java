package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.CulturalProductDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.CulturalProductService;
import com.class242014group06.vo.CulturalProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文创产品管理
 */
@RestController
@RequestMapping("/admin/product")
@Slf4j
@Api(tags = "文创产品接口")
public class CulturalProductController {

    @Autowired
    private CulturalProductService productService;

    /**
     * 传承人创建产品
     */
    @PostMapping
    @ApiOperation("创建文创产品")
    public Result<String> create(@RequestBody CulturalProductDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("创建文创产品，inheritorId：{}", inheritorId);
        productService.create(inheritorId, dto);
        return Result.success("产品创建成功");
    }

    /**
     * 传承人更新产品
     */
    @PutMapping("/{id}")
    @ApiOperation("更新文创产品")
    public Result<String> update(@PathVariable Long id, @RequestBody CulturalProductDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("更新文创产品，id：{}，inheritorId：{}", id, inheritorId);
        productService.update(id, inheritorId, dto);
        return Result.success("产品更新成功");
    }

    /**
     * 传承人删除产品
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除文创产品")
    public Result<String> delete(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("删除文创产品，id：{}，inheritorId：{}", id, inheritorId);
        productService.delete(id, inheritorId);
        return Result.success("产品已删除");
    }

    /**
     * 切换产品上下架
     */
    @PutMapping("/{id}/status")
    @ApiOperation("切换产品上下架")
    public Result<String> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("切换产品状态，id：{}，status：{}", id, status);
        productService.toggleStatus(id, inheritorId, status);
        return Result.success(status == 1 ? "产品已上架" : "产品已下架");
    }

    /**
     * 传承人查看自己的产品列表
     */
    @GetMapping("/my")
    @ApiOperation("查看自己的产品列表")
    public Result<List<CulturalProductVO>> listMyProducts() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("查看自己的产品列表，inheritorId：{}", inheritorId);
        List<CulturalProductVO> list = productService.listByInheritor(inheritorId);
        return Result.success(list);
    }

    /**
     * 公开接口：查询所有已上架产品
     */
    @GetMapping("/public")
    @ApiOperation("查询所有已上架产品（公开）")
    public Result<List<CulturalProductVO>> listPublished() {
        log.info("公开查询所有已上架产品");
        List<CulturalProductVO> list = productService.listPublished();
        return Result.success(list);
    }
}

package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ProductSubmitDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.ProductService;
import com.class242014group06.vo.ProductVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文创商品管理 — 商城浏览 & 传承人商品管理
 */
@RestController
@RequestMapping("/admin/product")
@Slf4j
@Api(tags = "文创商品相关接口")
public class ProductController {

    @Autowired
    private ProductService productService;

    /**
     * 查询所有上架商品（公开访问）
     */
    @GetMapping
    @ApiOperation("查询所有上架商品")
    public Result<List<ProductVO>> listAll() {
        log.info("查询所有上架商品");
        List<ProductVO> products = productService.listAll();
        return Result.success(products);
    }

    /**
     * 按分类查询商品（公开访问）
     */
    @GetMapping("/category/{category}")
    @ApiOperation("按分类查询商品")
    public Result<List<ProductVO>> listByCategory(@PathVariable String category) {
        log.info("按分类查询商品：{}", category);
        List<ProductVO> products = productService.listByCategory(category);
        return Result.success(products);
    }

    /**
     * 查询商品详情
     */
    @GetMapping("/{id}")
    @ApiOperation("查询商品详情")
    public Result<ProductVO> getById(@PathVariable Long id) {
        log.info("查询商品详情：{}", id);
        ProductVO product = productService.getById(id);
        return Result.success(product);
    }

    /**
     * 传承人上架商品
     */
    @PostMapping
    @ApiOperation("传承人上架商品")
    public Result<String> add(@RequestBody ProductSubmitDTO productSubmitDTO) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人上架商品，inheritorId：{}，商品：{}", inheritorId, productSubmitDTO);
        productService.add(inheritorId, productSubmitDTO);
        return Result.success("商品上架成功");
    }

    /**
     * 传承人修改商品信息
     */
    @PutMapping("/{id}")
    @ApiOperation("传承人修改商品")
    public Result<String> update(@PathVariable Long id, @RequestBody ProductSubmitDTO productSubmitDTO) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人修改商品，productId：{}，inheritorId：{}", id, inheritorId);
        productService.update(id, inheritorId, productSubmitDTO);
        return Result.success("商品修改成功");
    }

    /**
     * 传承人下架商品
     */
    @PutMapping("/{id}/off")
    @ApiOperation("传承人下架商品")
    public Result<String> off(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人下架商品，productId：{}，inheritorId：{}", id, inheritorId);
        productService.toggleStatus(id, inheritorId, 0);
        return Result.success("商品已下架");
    }

    /**
     * 传承人重新上架商品
     */
    @PutMapping("/{id}/on")
    @ApiOperation("传承人重新上架商品")
    public Result<String> on(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人重新上架商品，productId：{}，inheritorId：{}", id, inheritorId);
        productService.toggleStatus(id, inheritorId, 1);
        return Result.success("商品已上架");
    }

    /**
     * 管理员删除任意商品
     */
    @DeleteMapping("/{id}/admin")
    @ApiOperation("管理员删除商品")
    public Result<String> deleteByAdmin(@PathVariable Long id) {
        log.info("管理员删除商品，id：{}", id);
        productService.deleteByAdmin(id);
        return Result.success("商品已删除");
    }

    /**
     * 管理员切换商品上下架
     */
    @PutMapping("/{id}/admin/toggle")
    @ApiOperation("管理员切换商品上下架")
    public Result<String> toggleByAdmin(@PathVariable Long id, @RequestParam Integer status) {
        log.info("管理员切换商品状态，id：{}，status：{}", id, status);
        productService.toggleByAdmin(id, status);
        return Result.success(status == 1 ? "商品已上架" : "商品已下架");
    }

    /**
     * 传承人查看自己的商品列表
     */
    @GetMapping("/my")
    @ApiOperation("传承人查看自己的商品")
    public Result<List<ProductVO>> listMyProducts() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("传承人查看自己的商品，inheritorId：{}", inheritorId);
        List<ProductVO> products = productService.listByInheritor(inheritorId);
        return Result.success(products);
    }
}

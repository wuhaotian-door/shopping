package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.HeritageProjectDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.HeritageProjectService;
import com.class242014group06.vo.HeritageProjectVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 非遗项目管理
 */
@RestController
@RequestMapping("/admin/project")
@Slf4j
@Api(tags = "非遗项目接口")
public class HeritageProjectController {

    @Autowired
    private HeritageProjectService projectService;

    /**
     * 传承人创建项目
     */
    @PostMapping
    @ApiOperation("创建非遗项目")
    public Result<String> create(@RequestBody HeritageProjectDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("创建非遗项目，inheritorId：{}", inheritorId);
        String warning = projectService.create(inheritorId, dto);
        if (warning != null) {
            return Result.success(warning);
        }
        return Result.success("项目创建成功");
    }

    /**
     * 传承人更新项目
     */
    @PutMapping("/{id}")
    @ApiOperation("更新非遗项目")
    public Result<String> update(@PathVariable Long id, @RequestBody HeritageProjectDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("更新非遗项目，id：{}，inheritorId：{}", id, inheritorId);
        String warning = projectService.update(id, inheritorId, dto);
        if (warning != null) {
            return Result.success(warning);
        }
        return Result.success("项目更新成功");
    }

    /**
     * 传承人删除项目
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除非遗项目")
    public Result<String> delete(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("删除非遗项目，id：{}，inheritorId：{}", id, inheritorId);
        projectService.delete(id, inheritorId);
        return Result.success("项目已删除");
    }

    /**
     * 切换项目上下架
     */
    @PutMapping("/{id}/status")
    @ApiOperation("切换项目上下架")
    public Result<String> toggleStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("切换项目状态，id：{}，status：{}", id, status);
        projectService.toggleStatus(id, inheritorId, status);
        return Result.success(status == 1 ? "项目已上架" : "项目已下架");
    }

    /**
     * 传承人查看自己的项目列表，可按类型筛选
     */
    @GetMapping("/my")
    @ApiOperation("查看自己的项目列表，可按类型筛选")
    public Result<List<HeritageProjectVO>> listMyProjects(@RequestParam(required = false) Integer projectType) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("查看自己的项目列表，inheritorId：{}，projectType：{}", inheritorId, projectType);
        List<HeritageProjectVO> list;
        if (projectType != null) {
            list = projectService.listByInheritor(inheritorId, projectType);
        } else {
            list = projectService.listByInheritor(inheritorId);
        }
        return Result.success(list);
    }

    /**
     * 管理员查询所有项目（含未上架）
     */
    @GetMapping("/all")
    @ApiOperation("管理员查询所有项目")
    public Result<List<HeritageProjectVO>> listAll() {
        log.info("管理员查询所有项目");
        return Result.success(projectService.listAll());
    }

    /**
     * 管理员删除任意项目
     */
    @DeleteMapping("/{id}/admin")
    @ApiOperation("管理员删除项目")
    public Result<String> deleteByAdmin(@PathVariable Long id) {
        log.info("管理员删除项目，id：{}", id);
        projectService.deleteByAdmin(id);
        return Result.success("项目已删除");
    }

    /**
     * 公开接口：查询所有已上架项目
     */
    @GetMapping("/public")
    @ApiOperation("查询所有已上架项目（公开）")
    public Result<List<HeritageProjectVO>> listPublished() {
        log.info("公开查询所有已上架项目");
        List<HeritageProjectVO> list = projectService.listPublished();
        return Result.success(list);
    }

    /**
     * 公开接口：查询指定传承人的已上架项目
     */
    @GetMapping("/public/inheritor/{inheritorId}")
    @ApiOperation("查询指定传承人的已上架项目（公开）")
    public Result<List<HeritageProjectVO>> listPublishedByInheritor(@PathVariable Long inheritorId) {
        log.info("公开查询传承人的已上架项目，inheritorId：{}", inheritorId);
        List<HeritageProjectVO> list = projectService.listPublishedByInheritor(inheritorId);
        return Result.success(list);
    }

    /**
     * 校验项目日期是否在时段范围内
     */
    @GetMapping("/validate-dates")
    @ApiOperation("校验项目日期是否在时段范围内")
    public Result<String> validateDates(@RequestParam String startDate, @RequestParam String endDate) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("校验项目日期，inheritorId：{}，startDate：{}，endDate：{}", inheritorId, startDate, endDate);
        // 这里返回简单的提示，前端可根据 /slot/range 自行校验
        return Result.success(null);
    }
}

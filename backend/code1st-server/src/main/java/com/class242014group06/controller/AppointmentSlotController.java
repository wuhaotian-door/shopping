package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.AppointmentSlotDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.AppointmentSlotService;
import com.class242014group06.vo.AppointmentSlotVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 可预约时段管理
 */
@RestController
@RequestMapping("/admin/slot")
@Slf4j
@Api(tags = "预约时段接口")
public class AppointmentSlotController {

    @Autowired
    private AppointmentSlotService slotService;

    /**
     * 传承人新增可预约时段
     */
    @PostMapping
    @ApiOperation("新增可预约时段")
    public Result<String> create(@RequestBody AppointmentSlotDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("新增可预约时段，inheritorId：{}", inheritorId);
        slotService.create(inheritorId, dto);
        return Result.success("时段添加成功");
    }

    /**
     * 传承人更新时段
     */
    @PutMapping("/{id}")
    @ApiOperation("更新时段")
    public Result<String> update(@PathVariable Long id, @RequestBody AppointmentSlotDTO dto) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("更新时段，id：{}，inheritorId：{}", id, inheritorId);
        slotService.update(id, inheritorId, dto);
        return Result.success("时段更新成功");
    }

    /**
     * 传承人删除时段
     */
    @DeleteMapping("/{id}")
    @ApiOperation("删除时段")
    public Result<String> delete(@PathVariable Long id) {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("删除时段，id：{}，inheritorId：{}", id, inheritorId);
        slotService.delete(id, inheritorId);
        return Result.success("时段已删除");
    }

    /**
     * 传承人查看自己的时段列表
     */
    @GetMapping("/my")
    @ApiOperation("查看自己的时段列表")
    public Result<List<AppointmentSlotVO>> listMySlots() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("查看自己的时段列表，inheritorId：{}", inheritorId);
        List<AppointmentSlotVO> list = slotService.listByInheritor(inheritorId);
        return Result.success(list);
    }

    /**
     * 公开接口：查询传承人的可用时段（供用户预约时选择）
     */
    @GetMapping("/available")
    @ApiOperation("查询可用时段（公开）")
    public Result<List<AppointmentSlotVO>> getAvailableSlots(@RequestParam Long inheritorId, @RequestParam String date) {
        log.info("查询可用时段，inheritorId：{}，date：{}", inheritorId, date);
        List<AppointmentSlotVO> list = slotService.getAvailableSlots(inheritorId, date);
        return Result.success(list);
    }

    /**
     * 获取自己的时段覆盖的日期范围（用于项目校验）
     */
    @GetMapping("/range")
    @ApiOperation("获取时段日期范围")
    public Result<Map<String, Object>> getSlotDateRange() {
        Long inheritorId = BaseContext.getCurrentId();
        log.info("获取时段日期范围，inheritorId：{}", inheritorId);
        Map<String, Object> range = slotService.getSlotDateRange(inheritorId);
        return Result.success(range);
    }
}

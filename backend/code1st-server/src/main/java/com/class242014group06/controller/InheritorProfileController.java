package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.InheritorProfileDTO;
import com.class242014group06.result.Result;
import com.class242014group06.service.InheritorProfileService;
import com.class242014group06.vo.InheritorProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 传承人个人资料管理
 */
@RestController
@RequestMapping("/admin/inheritor/profile")
@Slf4j
@Api(tags = "传承人个人资料接口")
public class InheritorProfileController {

    @Autowired
    private InheritorProfileService profileService;

    /**
     * 获取当前登录传承人的个人资料
     */
    @GetMapping
    @ApiOperation("获取当前传承人个人资料")
    public Result<InheritorProfileVO> getMyProfile() {
        Long employeeId = BaseContext.getCurrentId();
        log.info("获取传承人个人资料，employeeId：{}", employeeId);
        InheritorProfileVO vo = profileService.getByEmployeeId(employeeId);
        return Result.success(vo);
    }

    /**
     * 保存或更新个人资料
     */
    @PutMapping
    @ApiOperation("保存或更新个人资料")
    public Result<String> saveOrUpdate(@RequestBody InheritorProfileDTO dto) {
        Long employeeId = BaseContext.getCurrentId();
        log.info("保存传承人个人资料，employeeId：{}", employeeId);
        profileService.saveOrUpdate(employeeId, dto);
        return Result.success("保存成功");
    }

    /**
     * 查询所有传承人资料（公开接口，首页展示）
     */
    @GetMapping("/public")
    @ApiOperation("查询所有传承人资料（公开）")
    public Result<List<InheritorProfileVO>> listPublic() {
        log.info("公开查询所有传承人资料");
        List<InheritorProfileVO> list = profileService.listAllWithEmployee();
        return Result.success(list);
    }
}

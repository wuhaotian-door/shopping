package com.class242014group06.controller;

import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.ChangePasswordDTO;
import com.class242014group06.dto.UserProfileDTO;
import com.class242014group06.entity.User;
import com.class242014group06.result.Result;
import com.class242014group06.service.UserService;
import com.class242014group06.vo.UserProfileVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 普通用户个人资料管理
 */
@RestController
@RequestMapping("/admin/user")
@Slf4j
@Api(tags = "普通用户个人资料接口")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的个人资料
     */
    @GetMapping("/profile")
    @ApiOperation("获取当前用户个人资料")
    public Result<UserProfileVO> getMyProfile() {
        Long userId = BaseContext.getCurrentId();
        log.info("获取用户个人资料，userId：{}", userId);
        User user = userService.getById(userId);
        UserProfileVO vo = UserProfileVO.builder()
                .id(user.getId())
                .name(user.getName())
                .username(user.getUsername())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .sex(user.getSex())
                .build();
        return Result.success(vo);
    }

    /**
     * 更新当前登录用户的个人资料
     */
    @PutMapping("/profile")
    @ApiOperation("更新当前用户个人资料")
    public Result<String> updateProfile(@RequestBody UserProfileDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("更新用户个人资料，userId：{}", userId);
        userService.updateProfile(userId, dto);
        return Result.success("保存成功");
    }

    /**
     * 发送修改密码的邮箱验证码
     */
    @PostMapping("/send-change-password-code")
    @ApiOperation("发送修改密码验证码")
    public Result<String> sendChangePasswordCode() {
        Long userId = BaseContext.getCurrentId();
        log.info("发送修改密码验证码，userId：{}", userId);
        userService.sendChangePasswordCode(userId);
        return Result.success("验证码已发送至注册邮箱");
    }

    /**
     * 验证码校验通过后修改密码
     */
    @PutMapping("/change-password")
    @ApiOperation("验证码修改密码")
    public Result<String> changePassword(@RequestBody ChangePasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        log.info("修改密码，userId：{}", userId);
        userService.changePassword(userId, dto.getCode(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }
}

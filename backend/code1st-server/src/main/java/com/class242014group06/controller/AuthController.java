package com.class242014group06.controller;

import com.class242014group06.constant.JwtClaimsConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.ChangePasswordDTO;
import com.class242014group06.dto.ForgotPasswordDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.dto.ResetPasswordDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.properties.JwtProperties;
import com.class242014group06.result.Result;
import com.class242014group06.service.AdminService;
import com.class242014group06.service.AuthService;
import com.class242014group06.service.InheritorService;
import com.class242014group06.utils.JwtUtil;
import com.class242014group06.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 统一认证控制器 — 用户 / 传承人 / 管理员三端登录、注册
 */
@RestController
@RequestMapping("/admin/auth")
@Slf4j
@Api(tags = "认证相关接口")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private InheritorService inheritorService;
    @Autowired
    private AdminService adminService;
    @Autowired
    private JwtProperties jwtProperties;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private UserMapper userMapper;

    /**
     * 统一登录（自动判断管理员 → 传承人 → 普通用户）
     */
    @PostMapping("/login")
    @ApiOperation("统一登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO dto) {
        log.info("统一登录：{}", dto);
        EmployeeLoginVO vo = authService.login(dto);

        // 生成 JWT，携带用户ID和角色
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, vo.getId());
        claims.put(JwtClaimsConstant.ROLE, vo.getRole());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);
        vo.setToken(token);
        return Result.success(vo);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    @ApiOperation("登出")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 校验当前 token 是否有效（用于前端启动时验证登录状态）
     */
    @GetMapping("/me")
    @ApiOperation("获取当前登录用户信息")
    public Result<EmployeeLoginVO> me() {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        // 根据角色查找用户名
        String name = "";
        String username = "";
        if (RoleConstant.ADMIN.equals(role)) {
            Admin admin = adminMapper.getById(userId);
            if (admin != null) { name = admin.getName(); username = admin.getUsername(); }
        } else if (RoleConstant.INHERITOR.equals(role)) {
            Inheritor inheritor = inheritorMapper.getById(userId);
            if (inheritor != null) { name = inheritor.getName(); username = inheritor.getUsername(); }
        } else {
            User user = userMapper.getById(userId);
            if (user != null) { name = user.getName(); username = user.getUsername(); }
        }
        log.info("token校验通过：userId={}, role={}, name={}", userId, role, name);
        EmployeeLoginVO vo = EmployeeLoginVO.builder()
                .id(userId).userName(username).name(name).role(role).build();
        return Result.success(vo);
    }

    /**
     * 普通用户注册（公开接口）
     */
    @PostMapping("/register")
    @ApiOperation("普通用户注册")
    public Result<String> register(@RequestBody EmployeeRegisterDTO dto) {
        log.info("普通用户注册：{}", dto);
        authService.registerUser(dto);
        return Result.success("注册成功");
    }

    /**
     * 非遗传承人注册（公开接口，含地区信息）
     */
    @PostMapping("/register/inheritor")
    @ApiOperation("非遗传承人注册")
    public Result<String> registerInheritor(@RequestBody InheritorRegisterDTO dto) {
        log.info("非遗传承人注册：{}", dto);
        authService.registerInheritor(dto);
        return Result.success("申请已提交，请等待管理员审核通过");
    }

    /**
     * 获取待审核传承人列表（管理员专用）
     */
    @GetMapping("/pending")
    @ApiOperation("获取待审核传承人列表")
    public Result<List<Inheritor>> getPendingList() {
        log.info("获取待审核传承人列表");
        List<Inheritor> list = inheritorService.getPendingList();
        return Result.success(list);
    }

    /**
     * 审核通过传承人（管理员专用）
     */
    @PutMapping("/{id}/approve")
    @ApiOperation("审核通过传承人")
    public Result<String> approve(@PathVariable Long id) {
        log.info("审核通过传承人：{}", id);
        inheritorService.approve(id);
        return Result.success("审核通过");
    }

    /**
     * 驳回传承人申请（管理员专用）
     */
    @PutMapping("/{id}/reject")
    @ApiOperation("驳回传承人申请")
    public Result<String> reject(@PathVariable Long id) {
        log.info("驳回传承人申请：{}", id);
        inheritorService.reject(id);
        return Result.success("已驳回");
    }

    /**
     * 修改密码（登录后调用）
     */
    @PutMapping("/change-password")
    @ApiOperation("修改密码")
    public Result<String> changePassword(@RequestBody ChangePasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        String role = BaseContext.getCurrentRole();
        log.info("修改密码：userId={}, role={}", userId, role);
        if (!RoleConstant.ADMIN.equals(role)) {
            return Result.error("仅管理员支持此操作");
        }
        adminService.changePassword(userId, dto.getOldPassword(), dto.getNewPassword());
        return Result.success("密码修改成功");
    }

    /**
     * 忘记密码 — 发送邮箱验证码（公开接口）
     */
    @PostMapping("/forgot-password/send-code")
    @ApiOperation("发送密码重置验证码")
    public Result<String> sendResetCode(@RequestBody ForgotPasswordDTO dto) {
        log.info("发送密码重置验证码：{}", dto.getUsername());
        authService.sendResetCode(dto);
        return Result.success("验证码已发送至注册邮箱");
    }

    /**
     * 忘记密码 — 验证码校验并重置密码（公开接口）
     */
    @PostMapping("/forgot-password/reset")
    @ApiOperation("验证码重置密码")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO dto) {
        log.info("重置密码：{}", dto.getUsername());
        authService.resetPassword(dto);
        return Result.success("密码重置成功");
    }
}

package com.class242014group06.controller;

import com.class242014group06.constant.JwtClaimsConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.dto.BatchStatusDTO;
import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Employee;
import com.class242014group06.properties.JwtProperties;
import com.class242014group06.result.Result;
import com.class242014group06.service.EmployeeService;
import com.class242014group06.service.OperationLogService;
import com.class242014group06.utils.JwtUtil;
import com.class242014group06.vo.EmployeeLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工管理
 */
@RestController
@RequestMapping("/admin/employee")
@Slf4j
@Api(tags = "员工相关接口")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private OperationLogService operationLogService;
    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("员工登录")
    public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO employeeLoginDTO) {
        log.info("员工登录：{}", employeeLoginDTO);

        Employee employee = employeeService.login(employeeLoginDTO);

        //登录成功后，生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.EMP_ID, employee.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims);

        EmployeeLoginVO employeeLoginVO = EmployeeLoginVO.builder()
                .id(employee.getId())
                .userName(employee.getUsername())
                .name(employee.getName())
                .token(token)
                .role(employee.getRole())
                .build();

        return Result.success(employeeLoginVO);
    }

    /**
     * 退出
     *
     * @return
     */
    @PostMapping("/logout")
    @ApiOperation("员工登出")
    public Result<String> logout() {
        return Result.success();
    }

    /**
     * 新增员工
     * @param employeeDTO
     * @return
     */
    @PostMapping
    @ApiOperation("新增员工")
    public Result save(@RequestBody EmployeeDTO employeeDTO) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("新增员工{}", employeeDTO);
        employeeService.save(employeeDTO);
        // 记录操作日志
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "CREATE_ADMIN", "admin", null,
                "创建管理员「" + employeeDTO.getName() + "」");
        return Result.success();
    }

    /**
     * 普通用户自行注册（无需登录）
     * @param employeeRegisterDTO
     * @return
     */
    @PostMapping("/register")
    @ApiOperation("普通用户注册")
    public Result<String> register(@RequestBody EmployeeRegisterDTO employeeRegisterDTO) {
        log.info("普通用户注册：{}", employeeRegisterDTO);
        employeeService.register(employeeRegisterDTO);
        return Result.success("注册成功");
    }

    /**
     * 非遗传承人注册（无需登录，信息更详细）
     * @param inheritorRegisterDTO
     * @return
     */
    @PostMapping("/register/inheritor")
    @ApiOperation("非遗传承人注册")
    public Result<String> registerInheritor(@RequestBody InheritorRegisterDTO inheritorRegisterDTO) {
        log.info("非遗传承人注册：{}", inheritorRegisterDTO);
        employeeService.registerInheritor(inheritorRegisterDTO);
        return Result.success("申请已提交，请等待管理员审核通过");
    }

    /**
     * 获取待审核的传承人列表（管理员专用）
     * @return
     */
    @GetMapping("/pending")
    @ApiOperation("获取待审核的传承人列表")
    public Result<List<Employee>> getPendingList() {
        log.info("获取待审核的传承人列表");
        List<Employee> pendingList = employeeService.getPendingList();
        return Result.success(pendingList);
    }

    /**
     * 审核通过传承人（管理员专用）
     * @param id
     * @return
     */
    @PutMapping("/{id}/approve")
    @ApiOperation("审核通过传承人")
    public Result<String> approveInheritor(@PathVariable Long id) {
        log.info("审核通过传承人：{}", id);
        employeeService.approveInheritor(id);
        return Result.success("审核通过");
    }

    /**
     * 驳回传承人申请（管理员专用）
     * @param id
     * @return
     */
    @PutMapping("/{id}/reject")
    @ApiOperation("驳回传承人申请")
    public Result<String> rejectInheritor(@PathVariable Long id) {
        log.info("驳回传承人申请：{}", id);
        employeeService.rejectInheritor(id);
        return Result.success("已驳回");
    }

    /**
     * 获取所有用户列表（管理员专用）
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("获取所有用户列表")
    public Result<List<Employee>> listAll() {
        log.info("获取所有用户列表");
        return Result.success(employeeService.listAll());
    }

    /**
     * 搜索用户（管理员专用）
     * @param keyword
     * @return
     */
    @GetMapping("/search")
    @ApiOperation("搜索用户")
    public Result<List<Employee>> search(@RequestParam String keyword) {
        log.info("搜索用户：{}", keyword);
        return Result.success(employeeService.search(keyword));
    }

    /**
     * 修改用户状态（管理员专用）
     * @param id
     * @param status 状态值
     * @return
     */
    @PutMapping("/{id}/status")
    @ApiOperation("修改用户状态")
    public Result<String> updateStatus(@PathVariable Long id, @RequestParam Integer status,
            @RequestParam(required = false) String role) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("修改用户状态：id={}, status={}, role={}", id, status, role);
        employeeService.updateStatus(id, status, role);
        // 记录操作日志
        String statusText = status == 1 ? "启用" : "禁用";
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "TOGGLE_USER_STATUS", "user", id,
                "将用户ID=" + id + " 状态改为" + statusText);
        return Result.success("状态已更新");
    }

    @PostMapping("/batch-status")
    @ApiOperation("批量修改用户状态")
    public Result<String> batchUpdateStatus(@RequestBody BatchStatusDTO dto) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        log.info("批量修改用户状态：ids={}, status={}, role={}", dto.getIds(), dto.getStatus(), dto.getRole());
        employeeService.batchUpdateStatus(dto.getIds(), dto.getStatus(), dto.getRole());
        String statusText = dto.getStatus() == 1 ? "启用" : "禁用";
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "TOGGLE_USER_STATUS", "user", null,
                "批量" + statusText + dto.getIds().size() + "个用户");
        return Result.success("已" + statusText + dto.getIds().size() + "个用户");
    }

    /**
     * 获取仪表盘统计数据（管理员专用）
     * @return
     */
    @GetMapping("/statistics")
    @ApiOperation("获取仪表盘统计数据")
    public Result<Map<String, Long>> getStatistics() {
        log.info("获取仪表盘统计数据");
        return Result.success(employeeService.getStatistics());
    }

}

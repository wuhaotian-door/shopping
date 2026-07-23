package com.class242014group06.controller;

import com.class242014group06.constant.RoleConstant;
import com.class242014group06.context.BaseContext;
import com.class242014group06.entity.UserReport;
import com.class242014group06.result.Result;
import com.class242014group06.service.OperationLogService;
import com.class242014group06.service.UserReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/report")
@Slf4j
@Api(tags = "用户举报接口")
public class ReportController {

    @Autowired
    private UserReportService reportService;

    @Autowired
    private OperationLogService operationLogService;

    /** 提交举报（聊天中调用） */
    @PostMapping
    @ApiOperation("提交举报")
    public Result<String> submit(@RequestBody UserReport report) {
        Long reporterId = BaseContext.getCurrentId();
        String reporterRole = BaseContext.getCurrentRole();
        if (reporterRole == null) reporterRole = "USER";
        report.setReporterId(reporterId);
        report.setReporterRole(reporterRole);
        if (report.getReportedUserRole() == null) report.setReportedUserRole("USER");
        log.info("用户举报：reporter={}/{}, reported={}/{}", reporterId, reporterRole,
                report.getReportedUserId(), report.getReportedUserRole());
        reportService.submit(report);
        return Result.success("举报已提交，管理员将尽快处理");
    }

    /** 管理员查看举报列表 */
    @GetMapping
    @ApiOperation("查看举报列表")
    public Result<List<UserReport>> listAll() {
        BaseContext.requireAdmin();
        log.info("管理员查看举报列表");
        return Result.success(reportService.listAll());
    }

    /** 管理员处理举报 */
    @PutMapping("/{id}/handle")
    @ApiOperation("处理举报")
    public Result<String> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        BaseContext.requireAdmin();
        Long operatorId = BaseContext.getCurrentId();
        String handleResult = (String) body.getOrDefault("handleResult", "");
        Integer banDays = body.get("banDays") != null ? ((Number) body.get("banDays")).intValue() : 0;
        log.info("处理举报：id={}, result={}, banDays={}", id, handleResult, banDays);
        reportService.handle(id, handleResult, banDays);
        String banInfo = banDays > 0 ? "，封号" + banDays + "天" : "";
        operationLogService.log(operatorId, "管理员", RoleConstant.ADMIN,
                "HANDLE_REPORT", "report", id,
                "处理举报ID=" + id + "：" + handleResult + banInfo);
        return Result.success("处理完成");
    }
}

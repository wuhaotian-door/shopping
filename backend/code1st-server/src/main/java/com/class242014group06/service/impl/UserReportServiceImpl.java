package com.class242014group06.service.impl;

import com.class242014group06.constant.RoleConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.entity.UserReport;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.mapper.UserReportMapper;
import com.class242014group06.service.UserReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserReportServiceImpl implements UserReportService {

    @Autowired
    private UserReportMapper reportMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private InheritorMapper inheritorMapper;

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public void submit(UserReport report) {
        report.setCreateTime(LocalDateTime.now());
        report.setStatus(0);
        reportMapper.insert(report);
    }

    @Override
    public List<UserReport> listAll() {
        return reportMapper.listAll();
    }

    @Override
    public void handle(Long reportId, String handleResult, Integer banDays) {
        UserReport report = reportMapper.getById(reportId);
        if (report == null) return;

        // 更新举报状态
        reportMapper.handle(reportId, handleResult, banDays);

        // 封号处理
        if (banDays != null && banDays > 0) {
            LocalDateTime banUntil = LocalDateTime.now().plusDays(banDays);
            banUser(report.getReportedUserId(), report.getReportedUserRole(), banUntil);
        }
    }

    /**
     * 封禁用户：设置status=0并设置ban_until
     */
    private void banUser(Long userId, String role, LocalDateTime banUntil) {
        if (RoleConstant.ADMIN.equals(role)) {
            adminMapper.update(Admin.builder().id(userId).status(StatusConstant.DISABLE).banUntil(banUntil).build());
        } else if (RoleConstant.INHERITOR.equals(role)) {
            inheritorMapper.update(Inheritor.builder().id(userId).status(StatusConstant.DISABLE).banUntil(banUntil).build());
        } else {
            userMapper.update(User.builder().id(userId).status(StatusConstant.DISABLE).banUntil(banUntil).build());
        }
    }
}

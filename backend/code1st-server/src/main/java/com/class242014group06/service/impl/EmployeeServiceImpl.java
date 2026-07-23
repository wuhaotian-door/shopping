package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.PasswordConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Employee;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.exception.RegistrationFailedException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.EmployeeMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.EmployeeService;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private InheritorMapper inheritorMapper;

    /**
     * 员工登录（保留兼容，AuthService 是新的统一登录入口）
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        // 先查 admin 表
        Admin admin = adminMapper.getByUsername(username);
        if (admin != null) {
            password = DigestUtils.md5DigestAsHex(password.getBytes());
            if (!password.equals(admin.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (admin.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            Employee emp = new Employee();
            BeanUtils.copyProperties(admin, emp);
            emp.setRole(RoleConstant.ADMIN);
            return emp;
        }

        // 再查 inheritor 表
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor != null) {
            password = DigestUtils.md5DigestAsHex(password.getBytes());
            if (!password.equals(inheritor.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (inheritor.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            if (inheritor.getStatus() == StatusConstant.PENDING) {
                throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
            }
            Employee emp = new Employee();
            BeanUtils.copyProperties(inheritor, emp);
            emp.setRole(RoleConstant.INHERITOR);
            return emp;
        }

        // 最后查 user 表
        User user = userMapper.getByUsername(username);
        if (user != null) {
            password = DigestUtils.md5DigestAsHex(password.getBytes());
            if (!password.equals(user.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (user.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            Employee emp = new Employee();
            BeanUtils.copyProperties(user, emp);
            emp.setRole(RoleConstant.USER);
            return emp;
        }

        throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    /**
     * 新增管理员
     */
    public void save(EmployeeDTO employeeDTO) {
        Admin admin = new Admin();
        BeanUtils.copyProperties(employeeDTO, admin);
        admin.setStatus(StatusConstant.ENABLE);
        admin.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));
        adminMapper.insert(admin);
    }

    /**
     * 普通用户自行注册
     */
    @Override
    public void register(EmployeeRegisterDTO employeeRegisterDTO) {
        User user = new User();
        BeanUtils.copyProperties(employeeRegisterDTO, user);
        user.setPassword(DigestUtils.md5DigestAsHex(employeeRegisterDTO.getPassword().getBytes()));
        user.setStatus(StatusConstant.ENABLE);
        userMapper.insert(user);
    }

    /**
     * 非遗传承人注册
     */
    @Override
    public void registerInheritor(InheritorRegisterDTO inheritorRegisterDTO) {
        Inheritor inheritor = new Inheritor();
        BeanUtils.copyProperties(inheritorRegisterDTO, inheritor);
        inheritor.setPassword(DigestUtils.md5DigestAsHex(inheritorRegisterDTO.getPassword().getBytes()));
        inheritor.setStatus(StatusConstant.PENDING);
        inheritorMapper.insert(inheritor);
    }

    /**
     * 获取待审核的传承人列表
     */
    @Override
    public List<Employee> getPendingList() {
        List<Inheritor> list = inheritorMapper.getByStatus(StatusConstant.PENDING);
        List<Employee> result = new ArrayList<>();
        for (Inheritor i : list) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(i, emp);
            emp.setRole(RoleConstant.INHERITOR);
            result.add(emp);
        }
        return result;
    }

    /**
     * 审核通过传承人
     */
    @Override
    public void approveInheritor(Long id) {
        Inheritor inheritor = Inheritor.builder().id(id).status(StatusConstant.ENABLE).build();
        inheritorMapper.update(inheritor);
    }

    /**
     * 驳回传承人申请
     */
    @Override
    public void rejectInheritor(Long id) {
        Inheritor inheritor = Inheritor.builder().id(id).status(StatusConstant.DISABLE).build();
        inheritorMapper.update(inheritor);
    }

    /**
     * 查询所有用户（合并三张表）
     */
    @Override
    public List<Employee> listAll() {
        List<Employee> result = new ArrayList<>();

        for (User u : userMapper.listAll()) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(u, emp);
            emp.setRole(RoleConstant.USER);
            result.add(emp);
        }
        for (Inheritor i : inheritorMapper.listAll()) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(i, emp);
            emp.setRole(RoleConstant.INHERITOR);
            result.add(emp);
        }
        for (Admin a : adminMapper.listAll()) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(a, emp);
            emp.setRole(RoleConstant.ADMIN);
            result.add(emp);
        }
        return result;
    }

    /**
     * 搜索用户（跨三张表）
     */
    @Override
    public List<Employee> search(String keyword) {
        List<Employee> result = new ArrayList<>();

        for (User u : userMapper.searchByKeyword(keyword)) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(u, emp);
            emp.setRole(RoleConstant.USER);
            result.add(emp);
        }
        for (Inheritor i : inheritorMapper.searchByKeyword(keyword)) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(i, emp);
            emp.setRole(RoleConstant.INHERITOR);
            result.add(emp);
        }
        for (Admin a : adminMapper.searchByKeyword(keyword)) {
            Employee emp = new Employee();
            BeanUtils.copyProperties(a, emp);
            emp.setRole(RoleConstant.ADMIN);
            result.add(emp);
        }
        return result;
    }

    /**
     * 修改用户状态（根据ID范围判断表）
     * 简单策略：逐个表尝试更新
     */
    @Override
    public void updateStatus(Long id, Integer status, String role) {
        if (RoleConstant.ADMIN.equals(role)) {
            adminMapper.update(Admin.builder().id(id).status(status).build());
        } else if (RoleConstant.INHERITOR.equals(role)) {
            inheritorMapper.update(Inheritor.builder().id(id).status(status).build());
        } else {
            userMapper.update(User.builder().id(id).status(status).build());
        }
    }

    /**
     * 获取仪表盘统计数据（汇总三张表）
     */
    @Override
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new HashMap<>();
        long userCount = userMapper.countAll();
        long inheritorCount = inheritorMapper.countAll();
        long adminCount = adminMapper.countAll();
        stats.put("totalUsers", userCount + inheritorCount + adminCount);
        stats.put("userCount", userCount);
        stats.put("inheritorCount", inheritorCount);
        stats.put("adminCount", adminCount);
        stats.put("pendingCount", inheritorMapper.countByStatus(StatusConstant.PENDING));
        stats.put("enabledCount",
                userMapper.countByStatus(StatusConstant.ENABLE)
                + inheritorMapper.countByStatus(StatusConstant.ENABLE)
                + adminMapper.countByStatus(StatusConstant.ENABLE));
        stats.put("disabledCount",
                userMapper.countByStatus(StatusConstant.DISABLE)
                + inheritorMapper.countByStatus(StatusConstant.DISABLE)
                + adminMapper.countByStatus(StatusConstant.DISABLE));
        return stats;
    }

    @Override
    public void banUser(Long id, Integer status, String role, Integer banDays) {
        java.time.LocalDateTime banUntil = null;
        if (status == StatusConstant.DISABLE && banDays != null && banDays > 0) {
            banUntil = java.time.LocalDateTime.now().plusDays(banDays);
        }
        if (RoleConstant.ADMIN.equals(role)) {
            adminMapper.update(Admin.builder().id(id).status(status).banUntil(banUntil).build());
        } else if (RoleConstant.INHERITOR.equals(role)) {
            inheritorMapper.update(Inheritor.builder().id(id).status(status).banUntil(banUntil).build());
        } else {
            userMapper.update(User.builder().id(id).status(status).banUntil(banUntil).build());
        }
    }

    @Override
    public void batchUpdateStatus(java.util.List<Long> ids, Integer status, String role) {
        for (Long id : ids) {
            if (RoleConstant.ADMIN.equals(role)) {
                adminMapper.update(Admin.builder().id(id).status(status).build());
            } else if (RoleConstant.INHERITOR.equals(role)) {
                inheritorMapper.update(Inheritor.builder().id(id).status(status).build());
            } else {
                userMapper.update(User.builder().id(id).status(status).build());
            }
        }
    }
}

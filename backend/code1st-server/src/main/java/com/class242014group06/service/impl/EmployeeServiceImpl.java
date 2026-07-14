package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.PasswordConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.EmployeeDTO;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Employee;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.exception.RegistrationFailedException;
import com.class242014group06.mapper.EmployeeMapper;
import com.class242014group06.service.EmployeeService;




import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;


@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        //对前端传来的密码进行md5加密处理
        password =  DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        if (employee.getStatus() == StatusConstant.PENDING) {
            //账号待审核（非遗传承人注册后等待管理员审核）
            throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    public void save(EmployeeDTO employeeDTO){
        Employee employee = new Employee();

        //对象属性拷贝
        BeanUtils.copyProperties(employeeDTO,employee);

        //设置对象状态,默认正常
        employee.setStatus(StatusConstant.ENABLE);

        //设置密码,默认为123456
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));

//        //设置当前记录的创建时间和修改时间
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
//
//        //设置当前创建人id和修改人id
//        employee.setUpdateUser(BaseContext.getCurrentId());
//        employee.setCreateUser(BaseContext.getCurrentId());

        employeeMapper.insert(employee);
    }

    /**
     * 普通用户自行注册（无需登录）
     * @param employeeRegisterDTO
     */
    @Override
    public void register(EmployeeRegisterDTO employeeRegisterDTO) {
        // 强制设置为普通用户角色
        employeeRegisterDTO.setRole(RoleConstant.USER);

        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeRegisterDTO, employee);

        // 对用户输入的密码进行 MD5 加密
        employee.setPassword(DigestUtils.md5DigestAsHex(employeeRegisterDTO.getPassword().getBytes()));

        // 设置状态为启用
        employee.setStatus(StatusConstant.ENABLE);

        employeeMapper.insert(employee);
    }

    /**
     * 非遗传承人注册（无需登录，信息更详细）
     * @param inheritorRegisterDTO
     */
    @Override
    public void registerInheritor(InheritorRegisterDTO inheritorRegisterDTO) {
        // 强制设置为非遗传承人角色
        inheritorRegisterDTO.setRole(RoleConstant.INHERITOR);

        Employee employee = new Employee();
        BeanUtils.copyProperties(inheritorRegisterDTO, employee);

        // 对用户输入的密码进行 MD5 加密
        employee.setPassword(DigestUtils.md5DigestAsHex(inheritorRegisterDTO.getPassword().getBytes()));

        // 设置状态为待审核（需要管理员审核通过后才能登录）
        employee.setStatus(StatusConstant.PENDING);

        employeeMapper.insert(employee);
    }

    /**
     * 获取待审核的传承人列表
     * @return
     */
    @Override
    public List<Employee> getPendingList() {
        return employeeMapper.getByStatus(StatusConstant.PENDING);
    }

    /**
     * 审核通过传承人
     * @param id
     */
    @Override
    public void approveInheritor(Long id) {
        Employee employee = Employee.builder()
                .id(id)
                .status(StatusConstant.ENABLE)
                .build();
        employeeMapper.update(employee);
    }

    /**
     * 驳回传承人申请
     * @param id
     */
    @Override
    public void rejectInheritor(Long id) {
        Employee employee = Employee.builder()
                .id(id)
                .status(StatusConstant.DISABLE)
                .build();
        employeeMapper.update(employee);
    }

}

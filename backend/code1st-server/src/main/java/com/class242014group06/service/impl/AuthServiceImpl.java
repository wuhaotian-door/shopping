package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.PasswordConstant;
import com.class242014group06.constant.RoleConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.dto.EmployeeLoginDTO;
import com.class242014group06.dto.EmployeeRegisterDTO;
import com.class242014group06.dto.InheritorRegisterDTO;
import com.class242014group06.entity.Admin;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.entity.User;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.LoginFailedException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.exception.RegistrationFailedException;
import com.class242014group06.mapper.AdminMapper;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.mapper.UserMapper;
import com.class242014group06.service.AuthService;
import com.class242014group06.vo.EmployeeLoginVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private InheritorMapper inheritorMapper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public EmployeeLoginVO login(EmployeeLoginDTO dto) {
        String username = dto.getUsername();
        String password = DigestUtils.md5DigestAsHex(dto.getPassword().getBytes());

        // 1. 先查管理员表
        Admin admin = adminMapper.getByUsername(username);
        if (admin != null) {
            if (!password.equals(admin.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (admin.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            return EmployeeLoginVO.builder()
                    .id(admin.getId())
                    .userName(admin.getUsername())
                    .name(admin.getName())
                    .role(RoleConstant.ADMIN)
                    .build();
        }

        // 2. 再查传承人表
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor != null) {
            if (!password.equals(inheritor.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (inheritor.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            if (inheritor.getStatus() == StatusConstant.PENDING) {
                throw new AccountPendingApprovalException(MessageConstant.ACCOUNT_PENDING);
            }
            return EmployeeLoginVO.builder()
                    .id(inheritor.getId())
                    .userName(inheritor.getUsername())
                    .name(inheritor.getName())
                    .role(RoleConstant.INHERITOR)
                    .build();
        }

        // 3. 最后查用户表
        User user = userMapper.getByUsername(username);
        if (user != null) {
            if (!password.equals(user.getPassword())) {
                throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
            }
            if (user.getStatus() == StatusConstant.DISABLE) {
                throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
            }
            return EmployeeLoginVO.builder()
                    .id(user.getId())
                    .userName(user.getUsername())
                    .name(user.getName())
                    .role(RoleConstant.USER)
                    .build();
        }

        throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    @Override
    public void registerUser(EmployeeRegisterDTO dto) {
        // 检查用户名是否已存在（跨表检查）
        if (userMapper.getByUsername(dto.getUsername()) != null
                || inheritorMapper.getByUsername(dto.getUsername()) != null
                || adminMapper.getByUsername(dto.getUsername()) != null) {
            throw new RegistrationFailedException(MessageConstant.ALREADY_EXISTS);
        }
        User user = new User();
        BeanUtils.copyProperties(dto, user);
        user.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));
        user.setStatus(StatusConstant.ENABLE);
        userMapper.insert(user);
    }

    @Override
    public void registerInheritor(InheritorRegisterDTO dto) {
        // 检查用户名是否已存在（跨表检查）
        if (userMapper.getByUsername(dto.getUsername()) != null
                || inheritorMapper.getByUsername(dto.getUsername()) != null
                || adminMapper.getByUsername(dto.getUsername()) != null) {
            throw new RegistrationFailedException(MessageConstant.ALREADY_EXISTS);
        }
        Inheritor inheritor = new Inheritor();
        BeanUtils.copyProperties(dto, inheritor);
        inheritor.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));
        inheritor.setStatus(StatusConstant.PENDING); // 传承人注册后待审核
        inheritorMapper.insert(inheritor);
    }
}

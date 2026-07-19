package com.class242014group06.service.impl;

import com.class242014group06.constant.MessageConstant;
import com.class242014group06.constant.StatusConstant;
import com.class242014group06.entity.Inheritor;
import com.class242014group06.exception.AccountLockedException;
import com.class242014group06.exception.AccountNotFoundException;
import com.class242014group06.exception.AccountPendingApprovalException;
import com.class242014group06.exception.PasswordErrorException;
import com.class242014group06.mapper.InheritorMapper;
import com.class242014group06.service.InheritorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.List;

@Service
public class InheritorServiceImpl implements InheritorService {

    @Autowired
    private InheritorMapper inheritorMapper;

    @Override
    public Inheritor login(String username, String password) {
        Inheritor inheritor = inheritorMapper.getByUsername(username);
        if (inheritor == null) {
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
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
        return inheritor;
    }

    @Override
    public List<Inheritor> getPendingList() {
        return inheritorMapper.getByStatus(StatusConstant.PENDING);
    }

    @Override
    public void approve(Long id) {
        Inheritor inheritor = Inheritor.builder()
                .id(id)
                .status(StatusConstant.ENABLE)
                .build();
        inheritorMapper.update(inheritor);
    }

    @Override
    public void reject(Long id) {
        Inheritor inheritor = Inheritor.builder()
                .id(id)
                .status(StatusConstant.DISABLE)
                .build();
        inheritorMapper.update(inheritor);
    }
}

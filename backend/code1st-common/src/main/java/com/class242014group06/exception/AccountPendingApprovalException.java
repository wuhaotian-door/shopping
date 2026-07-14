package com.class242014group06.exception;

/**
 * 账号待审核异常（非遗传承人注册后等待管理员审核期间尝试登录时抛出）
 */
public class AccountPendingApprovalException extends BaseException {

    public AccountPendingApprovalException() {
    }

    public AccountPendingApprovalException(String msg) {
        super(msg);
    }
}

package com.class242014group06.exception;

/**
 * 未授权访问异常（非管理员访问管理员接口）
 */
public class UnauthorizedException extends BaseException {

    public UnauthorizedException() {
        super("无权访问，需要管理员权限");
    }

    public UnauthorizedException(String msg) {
        super(msg);
    }
}

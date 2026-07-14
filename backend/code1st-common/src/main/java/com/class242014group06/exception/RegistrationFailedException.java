package com.class242014group06.exception;

/**
 * 注册失败异常
 */
public class RegistrationFailedException extends BaseException {
    public RegistrationFailedException() {
    }

    public RegistrationFailedException(String msg) {
        super(msg);
    }
}
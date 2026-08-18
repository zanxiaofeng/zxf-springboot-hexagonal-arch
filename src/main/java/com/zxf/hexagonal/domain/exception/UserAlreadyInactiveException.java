package com.zxf.hexagonal.domain.exception;

/**
 * 非法状态转换：已停用用户再次停用。
 */
public class UserAlreadyInactiveException extends DomainException {

    public static final String CODE = "USER_ALREADY_INACTIVE";

    private final Long userId;

    public UserAlreadyInactiveException(Long userId) {
        super(CODE, "User %s is already inactive".formatted(userId));
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}

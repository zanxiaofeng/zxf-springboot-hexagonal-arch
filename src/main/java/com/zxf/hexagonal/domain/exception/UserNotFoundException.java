package com.zxf.hexagonal.domain.exception;

/**
 * 用户不存在（或已被软删除）。
 */
public class UserNotFoundException extends DomainException {

    public static final String CODE = "USER_NOT_FOUND";

    private final Long userId;

    public UserNotFoundException(Long userId) {
        super(CODE, "User not found: %s".formatted(userId));
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}

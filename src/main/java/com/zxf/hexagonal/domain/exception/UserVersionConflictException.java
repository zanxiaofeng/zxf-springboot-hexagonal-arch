package com.zxf.hexagonal.domain.exception;

/**
 * 乐观锁冲突：请求携带的 version 与当前数据不一致。
 */
public class UserVersionConflictException extends DomainException {

    public static final String CODE = "USER_VERSION_CONFLICT";

    private final Long userId;

    public UserVersionConflictException(Long userId) {
        super(CODE, "Version conflict on user: %s".formatted(userId));
        this.userId = userId;
    }

    /** 适配器翻译 OptimisticLockingFailureException 时使用（保留 cause）。 */
    public UserVersionConflictException(Long userId, Throwable cause) {
        super(CODE, "Version conflict on user: %s".formatted(userId), cause);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}

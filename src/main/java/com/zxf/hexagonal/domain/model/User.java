package com.zxf.hexagonal.domain.model;

import com.zxf.hexagonal.domain.exception.UserAlreadyInactiveException;

/**
 * 用户聚合根（纯领域模型，零框架依赖）。
 *
 * <p>持久化细节（@Version 注解、审计时间戳、软删除标记）位于 UserJpaEntity，
 * 本类仅携带 version 数值用于并发控制语义。</p>
 */
public class User {

    private static final int NAME_MIN = 2;
    private static final int NAME_MAX = 50;

    private final UserId id;             // null = 未持久化
    private String name;
    private Email email;
    private UserStatus status;
    private final Long version;          // null = 未持久化

    private User(UserId id, String name, Email email, UserStatus status, Long version) {
        this.id = id;
        this.name = validatedName(name);
        this.email = requireEmail(email);
        this.status = requireStatus(status);
        this.version = version;
    }

    /**
     * 创建新用户（未持久化，id/version 为空，状态 ACTIVE）。
     */
    public static User create(String name, Email email) {
        return new User(null, name, email, UserStatus.ACTIVE, null);
    }

    /**
     * 从持久化数据重建（PersistenceMapper 专用，id/version 必须存在）。
     */
    public static User restore(UserId id, String name, Email email, UserStatus status, Long version) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null on restore");
        }
        if (version == null) {
            throw new IllegalArgumentException("version must not be null on restore");
        }
        return new User(id, name, email, status, version);
    }

    /**
     * 变更昵称（2-50 字符，非空）。
     */
    public void changeName(String newName) {
        this.name = validatedName(newName);
    }

    /**
     * 变更邮箱（格式由 Email 值对象构造期校验）。
     */
    public void changeEmail(Email newEmail) {
        this.email = requireEmail(newEmail);
    }

    /**
     * 激活：幂等操作——已激活用户再次激活不产生副作用。
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 停用：严格校验——已停用用户再次停用视为非法状态转换。
     */
    public void deactivate() {
        if (this.status == UserStatus.INACTIVE) {
            throw new UserAlreadyInactiveException(id != null ? id.value() : null);
        }
        this.status = UserStatus.INACTIVE;
    }

    private static String validatedName(String name) {
        if (name == null || name.isBlank() || name.length() < NAME_MIN || name.length() > NAME_MAX) {
            throw new IllegalArgumentException(
                    "Name must be %d-%d characters and not blank".formatted(NAME_MIN, NAME_MAX));
        }
        return name;
    }

    private static Email requireEmail(Email email) {
        if (email == null) {
            throw new IllegalArgumentException("email must not be null");
        }
        return email;
    }

    private static UserStatus requireStatus(UserStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        return status;
    }

    public UserId getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }
}

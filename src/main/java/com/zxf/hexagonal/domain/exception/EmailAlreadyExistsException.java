package com.zxf.hexagonal.domain.exception;

/**
 * 邮箱已被其他用户占用。
 */
public class EmailAlreadyExistsException extends DomainException {

    public static final String CODE = "EMAIL_ALREADY_EXISTS";

    private final String email;

    public EmailAlreadyExistsException(String email) {
        super(CODE, "Email already exists: %s".formatted(email));
        this.email = email;
    }

    public String getEmail() {
        return email;
    }
}

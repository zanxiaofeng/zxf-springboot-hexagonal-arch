package com.zxf.hexagonal.domain.model;

/**
 * 用户标识值对象。
 */
public record UserId(Long value) {

    public UserId {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("UserId must be a positive number");
        }
    }
}

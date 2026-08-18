package com.zxf.hexagonal.domain.model;

import java.util.regex.Pattern;

/**
 * 邮箱值对象：格式校验内聚于构造期，非法值无法被创建。
 */
public record Email(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    public Email {
        if (value == null || !PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}

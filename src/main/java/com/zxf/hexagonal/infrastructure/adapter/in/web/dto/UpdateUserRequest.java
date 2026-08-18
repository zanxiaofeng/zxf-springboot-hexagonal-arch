package com.zxf.hexagonal.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 更新用户 HTTP 请求：name/email 为 null 表示不更新；version 必填（乐观锁）。 */
public record UpdateUserRequest(

        @NotNull(message = "Version is required")
        Long version,

        @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
        String name,

        @Email(message = "Must be a valid email")
        String email
) {
}

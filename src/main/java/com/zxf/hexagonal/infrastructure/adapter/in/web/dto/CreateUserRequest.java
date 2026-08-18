package com.zxf.hexagonal.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建用户 HTTP 请求。 */
public record CreateUserRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email
) {
}

package com.zxf.hexagonal.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 创建用户命令。 */
public record CreateUserCommand(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Must be a valid email")
        String email
) {
}

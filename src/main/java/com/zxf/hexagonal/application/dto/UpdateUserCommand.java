package com.zxf.hexagonal.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 更新用户命令：name/email 为 null 表示不更新；version 必填（乐观锁）。 */
public record UpdateUserCommand(

        @NotNull(message = "Id is required")
        Long id,

        @NotNull(message = "Version is required")
        Long version,

        @Size(min = 2, max = 50, message = "Name must be 2-50 characters")
        String name,

        @Email(message = "Must be a valid email")
        String email
) {
}

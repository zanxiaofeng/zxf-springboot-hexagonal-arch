package com.zxf.hexagonal.infrastructure.adapter.in.web.dto;

import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.domain.model.UserStatus;

/** 用户 HTTP 响应。 */
public record UserResponse(
        Long id,
        String name,
        String email,
        UserStatus status,
        Long version
) {

    public static UserResponse from(UserDto dto) {
        return new UserResponse(dto.id(), dto.name(), dto.email(), dto.status(), dto.version());
    }
}

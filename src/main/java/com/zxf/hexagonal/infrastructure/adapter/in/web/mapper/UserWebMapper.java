package com.zxf.hexagonal.infrastructure.adapter.in.web.mapper;

import com.zxf.hexagonal.application.dto.CreateUserCommand;
import com.zxf.hexagonal.application.dto.UpdateUserCommand;
import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.CreateUserRequest;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.zxf.hexagonal.infrastructure.adapter.in.web.dto.UserResponse;
import org.springframework.data.domain.Page;

/**
 * Web 层映射：HTTP Request/Response ↔ 应用层 Command/DTO 转换的唯一发生地。
 */
public final class UserWebMapper {

    private UserWebMapper() {
    }

    public static CreateUserCommand toCommand(CreateUserRequest request) {
        return new CreateUserCommand(request.name(), request.email());
    }

    public static UpdateUserCommand toCommand(Long id, UpdateUserRequest request) {
        return new UpdateUserCommand(id, request.version(), request.name(), request.email());
    }

    public static UserResponse toResponse(UserDto dto) {
        return UserResponse.from(dto);
    }

    public static Page<UserResponse> toResponsePage(Page<UserDto> page) {
        return page.map(UserResponse::from);
    }
}

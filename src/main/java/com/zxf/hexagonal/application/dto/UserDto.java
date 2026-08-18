package com.zxf.hexagonal.application.dto;

import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserStatus;

import java.util.Objects;

/** 用户出参 DTO：from() 静态工厂承载 领域对象 → DTO 转换。 */
public record UserDto(
        Long id,
        String name,
        String email,
        UserStatus status,
        Long version
) {

    public static UserDto from(User user) {
        Objects.requireNonNull(user.getId(), "id must not be null on a persisted user");
        return new UserDto(
                user.getId().value(),
                user.getName(),
                user.getEmail().value(),
                user.getStatus(),
                user.getVersion()
        );
    }
}

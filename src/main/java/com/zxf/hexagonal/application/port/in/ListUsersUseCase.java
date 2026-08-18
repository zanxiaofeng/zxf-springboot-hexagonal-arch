package com.zxf.hexagonal.application.port.in;

import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** 入端口：分页查询用户列表（name 模糊、status 精确，均可选）。 */
public interface ListUsersUseCase {

    Page<UserDto> list(String name, UserStatus status, Pageable pageable);
}

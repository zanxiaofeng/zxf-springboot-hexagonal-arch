package com.zxf.hexagonal.application.port.in;

import com.zxf.hexagonal.application.dto.CreateUserCommand;
import com.zxf.hexagonal.application.dto.UserDto;

/** 入端口：创建用户。 */
public interface CreateUserUseCase {

    UserDto create(CreateUserCommand command);
}

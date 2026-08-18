package com.zxf.hexagonal.application.port.in;

import com.zxf.hexagonal.application.dto.UpdateUserCommand;
import com.zxf.hexagonal.application.dto.UserDto;

/** 入端口：部分更新用户（null 字段 = 不更新，带乐观锁版本校验）。 */
public interface UpdateUserUseCase {

    UserDto update(UpdateUserCommand command);
}

package com.zxf.hexagonal.application.port.in;

import com.zxf.hexagonal.application.dto.UserDto;
import com.zxf.hexagonal.domain.model.UserStatus;

/** 入端口：用户状态流转（经领域方法校验非法转换）。 */
public interface ChangeUserStatusUseCase {

    UserDto changeStatus(Long id, UserStatus target);
}

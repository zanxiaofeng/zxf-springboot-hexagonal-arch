package com.zxf.hexagonal.application.port.in;

import com.zxf.hexagonal.application.dto.UserDto;

/** 入端口：按 ID 查询用户。 */
public interface GetUserUseCase {

    UserDto findById(Long id);
}

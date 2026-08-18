package com.zxf.hexagonal.application.port.in;

/** 入端口：软删除用户。 */
public interface DeleteUserUseCase {

    void delete(Long id);
}

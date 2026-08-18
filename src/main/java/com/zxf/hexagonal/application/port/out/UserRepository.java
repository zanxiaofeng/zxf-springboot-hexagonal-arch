package com.zxf.hexagonal.application.port.out;

import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * 出端口：用户仓库（接口唯一定义处，适配器在 infrastructure/adapter/out/persistence 实现）。
 *
 * <p>软删除已过滤：findById/findByEmail/findAll 均不返回已删除数据。</p>
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(Email email);

    /** 邮箱是否已被其他用户占用（排除指定 id）。 */
    boolean existsByEmailAndIdNot(Email email, UserId id);

    User save(User user);

    /** 软删除；不存在（或已删除）时抛 UserNotFoundException。 */
    void deleteById(UserId id);

    Page<User> findAll(String name, UserStatus status, Pageable pageable);
}

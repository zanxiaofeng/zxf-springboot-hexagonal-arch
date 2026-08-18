package com.zxf.hexagonal.infrastructure.adapter.out.persistence.adapter;

import com.zxf.hexagonal.application.port.out.UserRepository;
import com.zxf.hexagonal.domain.exception.UserNotFoundException;
import com.zxf.hexagonal.domain.exception.UserVersionConflictException;
import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import com.zxf.hexagonal.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import com.zxf.hexagonal.infrastructure.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.zxf.hexagonal.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * 仓库适配器：实现 application/port/out 的 UserRepository，桥接到 Spring Data JPA。
 * 技术异常在此翻译为领域异常，不泄露到应用层。
 */
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value())
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public boolean existsByEmailAndIdNot(Email email, UserId id) {
        return jpaRepository.existsByEmailAndIdNot(email.value(), id.value());
    }

    @Override
    public User save(User user) {
        try {
            UserJpaEntity saved = jpaRepository.save(UserPersistenceMapper.toEntity(user));
            return UserPersistenceMapper.toDomain(saved);
        } catch (OptimisticLockingFailureException ex) {
            // JPA flush 阶段的乐观锁冲突兜底（Service 层版本比对之外的二道防线）
            throw new UserVersionConflictException(
                    user.getId() != null ? user.getId().value() : null, ex);
        }
    }

    @Override
    public void deleteById(UserId id) {
        int updated = jpaRepository.softDelete(id.value(), OffsetDateTime.now());
        if (updated == 0) {
            throw new UserNotFoundException(id.value());
        }
    }

    @Override
    public Page<User> findAll(String name, UserStatus status, Pageable pageable) {
        UserJpaEntity.UserStatus entityStatus =
                status != null ? UserJpaEntity.UserStatus.valueOf(status.name()) : null;
        return jpaRepository.findByCriteria(name, entityStatus, pageable)
                .map(UserPersistenceMapper::toDomain);
    }
}

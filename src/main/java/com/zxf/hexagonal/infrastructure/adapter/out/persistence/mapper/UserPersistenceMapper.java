package com.zxf.hexagonal.infrastructure.adapter.out.persistence.mapper;

import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.infrastructure.adapter.out.persistence.entity.UserJpaEntity;

/**
 * 持久化映射：JpaEntity ↔ 领域模型双向转换的唯一发生地。
 */
public final class UserPersistenceMapper {

    private UserPersistenceMapper() {
    }

    public static User toDomain(UserJpaEntity entity) {
        return User.restore(
                new UserId(entity.getId()),
                entity.getName(),
                new Email(entity.getEmail()),
                domainStatus(entity.getStatus()),
                entity.getVersion()
        );
    }

    public static UserJpaEntity toEntity(User user) {
        return UserJpaEntity.builder()
                .id(user.getId() != null ? user.getId().value() : null)
                .name(user.getName())
                .email(user.getEmail().value())
                .status(entityStatus(user.getStatus()))
                .version(user.getVersion())
                .build();
    }

    private static com.zxf.hexagonal.domain.model.UserStatus domainStatus(UserJpaEntity.UserStatus status) {
        return com.zxf.hexagonal.domain.model.UserStatus.valueOf(status.name());
    }

    private static UserJpaEntity.UserStatus entityStatus(com.zxf.hexagonal.domain.model.UserStatus status) {
        return UserJpaEntity.UserStatus.valueOf(status.name());
    }
}

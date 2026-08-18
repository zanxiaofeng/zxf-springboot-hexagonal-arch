package com.zxf.hexagonal.infrastructure.adapter.out.persistence.repository;

import com.zxf.hexagonal.infrastructure.adapter.out.persistence.entity.UserJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    /**
     * 条件分页查询（name 模糊、status 精确，均可为 null；@SQLRestriction 自动过滤软删除）。
     */
    @Query("""
            SELECT u FROM UserJpaEntity u
            WHERE (:name IS NULL OR u.name LIKE CONCAT('%', :name, '%'))
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<UserJpaEntity> findByCriteria(@Param("name") String name,
                                       @Param("status") UserJpaEntity.UserStatus status,
                                       Pageable pageable);

    /**
     * 软删除：仅更新未删除行（@SQLRestriction 不影响 @Modifying 更新），返回影响行数。
     */
    @Modifying
    @Query("UPDATE UserJpaEntity u SET u.deletedAt = :deletedAt WHERE u.id = :id AND u.deletedAt IS NULL")
    int softDelete(@Param("id") Long id, @Param("deletedAt") OffsetDateTime deletedAt);
}

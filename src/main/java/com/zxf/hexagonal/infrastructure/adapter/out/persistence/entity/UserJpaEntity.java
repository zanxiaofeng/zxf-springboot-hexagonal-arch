package com.zxf.hexagonal.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.time.OffsetDateTime;

/**
 * 用户 JPA 实体（持久化技术对象，非领域模型）：乐观锁、审计时间戳、软删除均落在此处，
 * 领域模型经 UserPersistenceMapper 与本类双向隔离。
 */
@Entity
@Table(name = "users")
@SQLRestriction("deleted_at IS NULL")   // Hibernate 7：软删除过滤（替代已废弃的 @Where）
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)        // 禁止 ORDINAL：数据库可读 + 枚举重排序安全
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Version                            // 乐观锁：所有可变实体必须
    private Long version;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * 本地 UserStatus 枚举（持久化层私有的状态表示，与领域枚举一一映射）。
     */
    public enum UserStatus {
        ACTIVE,
        INACTIVE
    }
}

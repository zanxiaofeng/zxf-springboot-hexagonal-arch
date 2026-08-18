package com.zxf.hexagonal.integration;

import com.zxf.hexagonal.application.port.out.UserRepository;
import com.zxf.hexagonal.domain.exception.UserNotFoundException;
import com.zxf.hexagonal.domain.model.Email;
import com.zxf.hexagonal.domain.model.User;
import com.zxf.hexagonal.domain.model.UserId;
import com.zxf.hexagonal.domain.model.UserStatus;
import com.zxf.hexagonal.infrastructure.adapter.out.persistence.adapter.UserRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 仓库适配器集成测试：@DataJpaTest 持久化切片 + Testcontainers 真实 MySQL，
 * 验证 JpaEntity 映射、@SQLRestriction 软删除过滤与乐观锁等真实数据库行为。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)   // 不替换为内嵌库，使用容器 MySQL
@Import(UserRepositoryAdapter.class)
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryAdapterTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void flywayLocation(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }

    @Autowired  // 测试切片允许字段注入；生产代码必须构造器注入
    private UserRepository userRepository;          // application/port/out 接口

    @Test
    void save_andFindById_roundTrip() {
        User saved = userRepository.save(
                User.create("Alice", new Email("alice@example.com")));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();    // @Version 初始为 0
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);

        Optional<User> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
        assertThat(found.get().getEmail()).isEqualTo(new Email("alice@example.com"));
    }

    @Test
    void findByEmail_returnsMatchingUser() {
        userRepository.save(User.create("Bob", new Email("bob@example.com")));

        Optional<User> found = userRepository.findByEmail(new Email("bob@example.com"));

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Bob");
        assertThat(userRepository.findByEmail(new Email("nobody@example.com"))).isEmpty();
    }

    @Test
    void existsByEmailAndIdNot_distinguishesOwners() {
        User alice = userRepository.save(
                User.create("Alice", new Email("shared@example.com")));

        // 同邮箱被他人占用
        assertThat(userRepository.existsByEmailAndIdNot(
                new Email("shared@example.com"), new UserId(alice.getId().value() + 999)))
                .isTrue();
        // 邮箱属于自己（更新场景）
        assertThat(userRepository.existsByEmailAndIdNot(
                new Email("shared@example.com"), alice.getId()))
                .isFalse();
    }

    @Test
    void deleteById_softDeletes_andHidesFromQueries() {
        User saved = userRepository.save(
                User.create("Carol", new Email("carol@example.com")));

        userRepository.deleteById(saved.getId());

        // 软删除后 findById/findByEmail 均不可见（@SQLRestriction 过滤）
        assertThat(userRepository.findById(saved.getId())).isEmpty();
        assertThat(userRepository.findByEmail(new Email("carol@example.com"))).isEmpty();
        // 重复删除 → UserNotFoundException
        assertThatThrownBy(() -> userRepository.deleteById(saved.getId()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findAll_filtersByNameAndStatus() {
        userRepository.save(User.create("Alice", new Email("alice-filter@example.com")));
        userRepository.save(User.create("Bob", new Email("bob-filter@example.com")));

        Page<User> byName = userRepository.findAll("ali", null, PageRequest.of(0, 10));
        Page<User> byStatus = userRepository.findAll(null, UserStatus.INACTIVE, PageRequest.of(0, 10));

        assertThat(byName.getContent())
                .extracting(User::getName)
                .containsExactly("Alice");
        assertThat(byStatus.getContent()).isEmpty();    // 新建用户均为 ACTIVE
    }
}

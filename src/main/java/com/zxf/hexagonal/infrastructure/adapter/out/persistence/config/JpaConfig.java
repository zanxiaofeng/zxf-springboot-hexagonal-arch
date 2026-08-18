package com.zxf.hexagonal.infrastructure.adapter.out.persistence.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 配置（就近管理）：精确扫描持久化适配器包，避免误扫其他层。
 * 注意：SB4 中 @EntityScan 的包已迁移至 org.springframework.boot.persistence.autoconfigure。
 */
@Configuration
@EntityScan(basePackages = "com.zxf.hexagonal.infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.zxf.hexagonal.infrastructure.adapter.out.persistence.repository")
public class JpaConfig {
}

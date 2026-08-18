package com.zxf.hexagonal.support.sql;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * e2e 测试 DB 直验工具：绕过应用层直接查询数据库状态。
 */
@Component
@RequiredArgsConstructor
public class DatabaseVerifier {

    private final JdbcTemplate jdbcTemplate;

    public long countUsers() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
    }

    public boolean userExistsByEmail(String email) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Long.class, email) > 0;
    }

    public OffsetDateTime findDeletedAtById(Long id) {
        List<Timestamp> result = jdbcTemplate.queryForList(
                "SELECT deleted_at FROM users WHERE id = ?", Timestamp.class, id);
        return result.isEmpty() || result.get(0) == null
                ? null
                : result.get(0).toInstant().atOffset(OffsetDateTime.now().getOffset());
    }
}

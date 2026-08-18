package com.zxf.hexagonal.domain.event;

import java.time.OffsetDateTime;

/**
 * 用户创建事件：创建事务提交成功后触发下游副作用（欢迎通知）。
 */
public record UserCreatedEvent(
        Long userId,
        String email,
        OffsetDateTime occurredAt
) implements DomainEvent {
}

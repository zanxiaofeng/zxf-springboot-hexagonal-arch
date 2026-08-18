package com.zxf.hexagonal.infrastructure.adapter.out.messaging;

import com.zxf.hexagonal.application.port.out.NotificationGateway;
import com.zxf.hexagonal.domain.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 用户创建事件处理器：驱动通知服务（出站副作用）。
 *
 * <p>事务时机由 LocalEventPublisher 保证（afterCommit 后才发布 Spring 事件），
 * 此处用普通 @EventListener 消费——afterCommit 阶段发布时事务同步已收尾，
 * 再挂 @TransactionalEventListener(AFTER_COMMIT) 反而不会触发。
 * 处理失败仅记日志降级，不影响已提交事务。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventHandler {

    private final NotificationGateway notificationGateway;

    @EventListener
    public void onUserCreated(UserCreatedEvent event) {
        boolean delivered = notificationGateway.sendWelcome(event.userId(), event.email());
        if (!delivered) {
            log.warn("Welcome notification degraded, userId: {}, email: {}", event.userId(), event.email());
        }
    }
}

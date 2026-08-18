package com.zxf.hexagonal.infrastructure.adapter.out.messaging;

import com.zxf.hexagonal.application.port.out.NotificationGateway;
import com.zxf.hexagonal.domain.event.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 用户创建事件处理器：事务提交成功后驱动通知服务（出站副作用）。
 *
 * <p>afterCommit 阶段的异常不影响已提交事务，此处理失败仅记日志降级。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserCreatedEventHandler {

    private final NotificationGateway notificationGateway;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        boolean delivered = notificationGateway.sendWelcome(event.userId(), event.email());
        if (!delivered) {
            log.warn("Welcome notification degraded, userId: {}, email: {}", event.userId(), event.email());
        }
    }
}

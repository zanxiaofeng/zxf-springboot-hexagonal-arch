package com.zxf.hexagonal.infrastructure.adapter.out.messaging;

import com.zxf.hexagonal.application.port.out.EventPublisher;
import com.zxf.hexagonal.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 领域事件发布适配器（本地事件实现）。
 *
 * <p>事务一致性保证：事务内调用时注册 afterCommit 回调，事务提交成功后才经
 * Spring 事件分发（消费方用 @TransactionalEventListener(AFTER_COMMIT) 接收）；
 * 事务回滚则事件不外发。无事务时直接发布。</p>
 */
@Component
@RequiredArgsConstructor
public class LocalEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher springPublisher;

    @Override
    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    springPublisher.publishEvent(event);
                }
            });
        } else {
            springPublisher.publishEvent(event);
        }
    }
}

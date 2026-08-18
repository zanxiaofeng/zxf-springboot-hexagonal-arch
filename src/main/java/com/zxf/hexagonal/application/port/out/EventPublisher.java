package com.zxf.hexagonal.application.port.out;

import com.zxf.hexagonal.domain.event.DomainEvent;

/**
 * 出端口：领域事件发布。
 *
 * <p>实现必须保证：事务内调用时延迟到提交成功后（afterCommit）才真正外发，
 * 事务回滚则不外发。</p>
 */
public interface EventPublisher {

    void publish(DomainEvent event);
}

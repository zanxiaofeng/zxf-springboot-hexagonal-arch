package com.zxf.hexagonal.application.port.out;

/**
 * 出端口：通知服务（外部系统）。
 *
 * <p>失败策略：瞬态故障降级返回 false（记 WARN），不阻塞主流程。</p>
 */
public interface NotificationGateway {

    /**
     * 发送用户创建欢迎通知。
     *
     * @return true=通知成功；false=下游不可达已降级
     */
    boolean sendWelcome(Long userId, String email);
}

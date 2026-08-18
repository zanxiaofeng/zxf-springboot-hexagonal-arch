package com.zxf.hexagonal.infrastructure.adapter.out.external;

import com.zxf.hexagonal.application.port.out.NotificationGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * 通知服务适配器：RestClient 调用外部 HTTP 服务，瞬态故障降级返回 false。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationGatewayAdapter implements NotificationGateway {

    private final RestClient notificationRestClient;

    @Override
    public boolean sendWelcome(Long userId, String email) {
        try {
            notificationRestClient.post()
                    .uri("/api/v1/notifications")
                    .body(new WelcomeNotificationRequest(userId, email, "WELCOME"))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (ResourceAccessException ex) {
            // 连接失败/超时：瞬态错误，降级不阻塞主流程
            log.warn("Notification service unreachable, degrade. userId: {}", userId);
            return false;
        } catch (Exception ex) {
            log.error("Failed to send welcome notification, userId: {}", userId, ex);
            return false;
        }
    }

    private record WelcomeNotificationRequest(Long userId, String email, String type) {
    }
}

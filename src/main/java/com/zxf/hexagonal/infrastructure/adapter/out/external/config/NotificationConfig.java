package com.zxf.hexagonal.infrastructure.adapter.out.external.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 通知服务 RestClient 配置（就近管理于 external 适配器目录下）。
 */
@Configuration
public class NotificationConfig {

    @Bean
    public RestClient notificationRestClient(RestClient.Builder builder,
            @Value("${app.downstream.notification.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .build();
    }
}

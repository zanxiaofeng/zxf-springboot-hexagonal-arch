package com.zxf.hexagonal.support.mocks;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.experimental.UtilityClass;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

/**
 * 通知服务 WireMock 打桩/验证（mock{Service}{Scenario} / verify{Service}{Action} 模式）。
 * WireMock 3.x standalone：客户端类包名 com.github.tomakehurst.wiremock.client。
 */
@UtilityClass
public class NotificationMockFactory {

    public void mockNotificationSuccess(WireMockServer server) {
        server.stubFor(post(urlEqualTo("/api/v1/notifications"))
                .willReturn(okJson("{\"status\":\"SENT\"}")));
    }

    public void mockNotificationServerFailure(WireMockServer server) {
        server.stubFor(post(urlEqualTo("/api/v1/notifications"))
                .willReturn(aResponse().withStatus(500)));
    }

    public void verifyNotificationCalled(WireMockServer server, int count) {
        server.verify(count, postRequestedFor(urlEqualTo("/api/v1/notifications")));
    }
}

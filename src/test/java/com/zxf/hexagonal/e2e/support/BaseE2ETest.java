package com.zxf.hexagonal.e2e.support;

import com.zxf.hexagonal.support.sql.DatabaseVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.testcontainers.containers.MySQLContainer;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * e2e 测试基类：完整 Spring Boot 上下文 + MockMvc + Testcontainers MySQL + WireMock 下游。
 *
 * <p>容器为 JVM 级单例（静态手动启动），跨测试类共享，避免重复启动开销；
 * 每个测试方法前经 @Sql 重建种子数据（cleanup → init）。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc                 // SB4：@SpringBootTest 不再自动注入 MockMvc，必须显式声明
@ActiveProfiles("test")
@Sql(scripts = {"/sql-data/cleanup/cleanup-users.sql", "/sql-data/init/data.sql"},
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
public abstract class BaseE2ETest {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");
    protected static final WireMockServer WIRE_MOCK =
            new WireMockServer(new com.github.tomakehurst.wiremock.core.WireMockConfiguration()
                    .dynamicPort());

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        if (!MYSQL.isRunning()) {
            MYSQL.start();
        }
        if (!WIRE_MOCK.isRunning()) {
            WIRE_MOCK.start();
        }
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("app.downstream.notification.base-url",
                () -> "http://127.0.0.1:" + WIRE_MOCK.port());   // 用 127.0.0.1 避免 localhost 解析为 IPv6 导致连接被拒
    }

    @Autowired  // 测试基类允许字段注入；生产代码必须构造器注入
    protected MockMvc mockMvc;

    @Autowired
    protected DatabaseVerifier databaseVerifier;

    @BeforeEach
    void resetWireMockRequests() {
        // 仅清空请求计数（stub 由各测试 Given 段注册）
        WIRE_MOCK.resetRequests();
    }

    // ── HTTP 辅助方法：断言状态码并返回响应体 ──

    protected String httpAndAssert(HttpMethod method, String uri, String jsonBody,
            HttpStatus expected) throws Exception {
        RequestBuilder builder = jsonBody != null
                ? MockMvcRequestBuilders.request(method, uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                : MockMvcRequestBuilders.request(method, uri);
        return mockMvc.perform(builder)
                .andExpect(status().is(expected.value()))
                .andReturn().getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    protected String httpGetAndAssert(String uri, HttpStatus expected) throws Exception {
        return httpAndAssert(HttpMethod.GET, uri, null, expected);
    }

    protected String httpPostAndAssert(String uri, String jsonBody,
            HttpStatus expected) throws Exception {
        return httpAndAssert(HttpMethod.POST, uri, jsonBody, expected);
    }

    protected String httpPutAndAssert(String uri, String jsonBody,
            HttpStatus expected) throws Exception {
        return httpAndAssert(HttpMethod.PUT, uri, jsonBody, expected);
    }

    protected String httpPatchAndAssert(String uri, String jsonBody,
            HttpStatus expected) throws Exception {
        return httpAndAssert(HttpMethod.PATCH, uri, jsonBody, expected);
    }

    protected void httpDeleteAndAssert(String uri, HttpStatus expected) throws Exception {
        httpAndAssert(HttpMethod.DELETE, uri, null, expected);
    }
}

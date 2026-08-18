package com.zxf.hexagonal.e2e;

import com.zxf.hexagonal.e2e.support.BaseE2ETest;
import com.zxf.hexagonal.support.mocks.NotificationMockFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户管理端到端流程：完整请求链路（Controller → Service → Repository → MySQL + WireMock 下游），
 * 覆盖 REQ-001 全部用例的成功与错误场景。
 */
class UserFlowTest extends BaseE2ETest {

    // ── UC-001 创建用户 ──

    @Test
    void testCreateUser() throws Exception {
        // Given
        NotificationMockFactory.mockNotificationSuccess(WIRE_MOCK);
        long countBefore = databaseVerifier.countUsers();

        // When
        MvcResult result = mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"name\":\"Bob\",\"email\":\"bob@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/2"))
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.name").value("Bob"))
                .andExpect(jsonPath("$.data.email").value("bob@example.com"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn();

        // Then — DB 状态 + 下游调用（afterCommit 后同步触发）
        assertThat(databaseVerifier.countUsers()).isEqualTo(countBefore + 1);
        NotificationMockFactory.verifyNotificationCalled(WIRE_MOCK, 1);
    }

    @Test
    void testCreateUserWithValidationError() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"name\":\"A\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());

        // 校验失败不触发下游
        NotificationMockFactory.verifyNotificationCalled(WIRE_MOCK, 0);
    }

    @Test
    void testCreateUserWithDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"name\":\"Alice Clone\",\"email\":\"alice@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));

        NotificationMockFactory.verifyNotificationCalled(WIRE_MOCK, 0);
    }

    // ── UC-002 查询用户 ──

    @Test
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("000000"))
                .andExpect(jsonPath("$.data.name").value("Alice"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    // ── UC-003 用户列表 ──

    @Test
    void testListUsersWithNameFilter() throws Exception {
        mockMvc.perform(get("/api/v1/users").param("name", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].name").value("Alice"));
    }

    @Test
    void testListUsersWithStatusFilter() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .param("status", "INACTIVE")
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    // ── UC-004 更新用户（部分更新 + 乐观锁）──

    @Test
    void testUpdateUser() throws Exception {
        // name 更新、email 为 null 不更新
        String body = httpPutAndAssert("/api/v1/users/1",
                "{\"version\":0,\"name\":\"Alice Chen\",\"email\":null}", HttpStatus.OK);

        assertThat(body).contains("Alice Chen");
        assertThat(body).contains("alice@example.com");
        assertThat(body).contains("\"version\":1");   // 版本自增
    }

    @Test
    void testUpdateUserVersionConflict() throws Exception {
        httpPutAndAssert("/api/v1/users/1",
                "{\"version\":99,\"name\":\"Stale Update\"}", HttpStatus.CONFLICT);
    }

    // ── UC-005 状态流转 ──

    @Test
    void testChangeStatusDeactivateThenReactivate() throws Exception {
        // ACTIVE → INACTIVE
        httpPatchAndAssert("/api/v1/users/1/status",
                "{\"status\":\"INACTIVE\"}", HttpStatus.OK);

        // INACTIVE → INACTIVE：非法转换
        httpPatchAndAssert("/api/v1/users/1/status",
                "{\"status\":\"INACTIVE\"}", HttpStatus.CONFLICT);

        // INACTIVE → ACTIVE：恢复
        httpPatchAndAssert("/api/v1/users/1/status",
                "{\"status\":\"ACTIVE\"}", HttpStatus.OK);
    }

    // ── UC-006 软删除 ──

    @Test
    void testDeleteUser() throws Exception {
        // 新建再删除，验证完整生命周期
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"name\":\"Carol\",\"email\":\"carol@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        httpDeleteAndAssert("/api/v1/users/2", HttpStatus.NO_CONTENT);

        // 已删除不可见；软删除标记已写入 DB
        httpGetAndAssert("/api/v1/users/2", HttpStatus.NOT_FOUND);
        assertThat(databaseVerifier.findDeletedAtById(2L)).isNotNull();

        // 重复删除 → 404
        httpDeleteAndAssert("/api/v1/users/2", HttpStatus.NOT_FOUND);
    }

    @Test
    void testDeleteUserNotFound() throws Exception {
        httpDeleteAndAssert("/api/v1/users/99999", HttpStatus.NOT_FOUND);
    }
}

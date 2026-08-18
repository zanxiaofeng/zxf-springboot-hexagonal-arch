# REQ-001: 用户管理

## Status
- [x] Requirement Analysis
- [x] Technical Design
- [x] Implementation
- [x] Testing
- [x] Completed

## Background

系统需要基础的用户管理能力，作为后续所有业务模块（订单、通知等）的支撑。本需求同时是项目的**首个纵切面**：通过完整的创建 → 查询 → 列表 → 更新 → 状态流转 → 删除流程，验证六边形架构骨架（领域层零依赖、端口与适配器、类型化异常、领域事件 + afterCommit 下游通知、软删除、乐观锁）端到端跑通，为后续模块提供可复制的实现范式。

## Functional Requirements

### UC-001: 创建用户
**Given** 系统中不存在该邮箱的用户
**When** 客户端提交 `{name, email}` 创建用户
**Then** 返回 201 Created（含 `Location: /api/v1/users/{id}` 与 `ApiResponse<UserResponse>` 响应体），用户状态为 `ACTIVE`，事务提交成功后向通知服务发送欢迎通知

#### Acceptance Criteria
1. name 非空且 2–50 字符；email 格式合法 —— 违反时返回 400 + `VALIDATION_ERROR`，不触发下游调用
2. email 已被占用时返回 409 + `EMAIL_ALREADY_EXISTS`，不发布领域事件、不调用通知服务
3. 创建成功发布 `UserCreatedEvent`；通知服务调用发生在**事务提交后**（afterCommit）——事务回滚则不通知
4. 通知服务不可用时**降级**（记录 WARN，返回 `false`），创建流程不受影响（仍返回 201）

### UC-002: 查询用户
**Given** 用户 `{id}` 存在且未删除
**When** 客户端按 id 查询
**Then** 返回 200 + 用户详情（`ApiResponse<UserResponse>`）

#### Acceptance Criteria
1. 用户不存在或已软删除时返回 404 + `USER_NOT_FOUND`
2. id 非正数返回 400（`@Positive` 校验）

### UC-003: 用户列表（分页 + 过滤）
**Given** 系统中存在若干未删除用户
**When** 客户端请求 `GET /api/v1/users?page=0&size=20&sort=id,asc&name=...&status=ACTIVE`
**Then** 返回 200 + `ApiResponse<Page<UserResponse>>`

#### Acceptance Criteria
1. 分页参数由 Spring Data `Pageable` 绑定；页大小全局上限 100
2. `name` 模糊匹配、`status` 精确匹配，两者均可选
3. 软删除用户不出现在列表中

### UC-004: 更新用户（部分更新 + 乐观锁）
**Given** 用户 `{id}` 存在，客户端持有该用户的 `version`
**When** 客户端提交 `{version, name?, email?}`（null 字段 = 不更新）
**Then** 返回 200 + 更新后的用户详情，`version` 自增

#### Acceptance Criteria
1. 字段为 null 表示不更新，而非清空
2. `version` 与当前不一致时返回 409 + `USER_VERSION_CONFLICT`
3. 更新为已存在的他人邮箱时返回 409 + `EMAIL_ALREADY_EXISTS`

### UC-005: 用户状态流转（停用 / 激活）
**Given** 用户处于 `ACTIVE` 状态
**When** 客户端调用 `PATCH /api/v1/users/{id}/status` 提交 `INACTIVE`
**Then** 用户状态变更为 `INACTIVE`，返回 200

#### Acceptance Criteria
1. 状态非法转换（如已 `INACTIVE` 再停用）返回 409 + `USER_ALREADY_INACTIVE`
2. 状态转换必须经过领域方法（`deactivate()` / `activate()`），禁止 setter 直改
3. `INACTIVE` → `ACTIVE` 允许

### UC-006: 删除用户（软删除）
**Given** 用户 `{id}` 存在且未删除
**When** 客户端调用 `DELETE /api/v1/users/{id}`
**Then** 返回 204 No Content（无响应体）；用户被软删除（`deleted_at` 置值）

#### Acceptance Criteria
1. 后续按 id 查询返回 404；不出现在列表中
2. 删除不存在的用户返回 404 + `USER_NOT_FOUND`
3. 物理数据保留（审计需要），查询经 `@SQLRestriction` 自动过滤

## Non-Functional Requirements
- Response time < 200ms (P95)
- 并发更新安全：所有可变数据带乐观锁版本号
- 事务一致性：任何外部副作用（通知）只在事务提交成功后发生
- 错误响应统一信封 `ApiResponse<T>`，错误码为稳定契约（见 API Spec §错误码）

## Database Changes
- New table `users` (see `db/migration/V1__create_users_table.sql`)

## Downstream Integration
- **通知服务**：创建用户成功后发送欢迎通知（`POST {notification-base-url}/api/v1/notifications`）
- 端口：`NotificationGateway`（`application/port/out/`），实现 `NotificationGatewayAdapter`（RestClient）
- 事件链路：`UserService` 事务内发布 `UserCreatedEvent` → `LocalEventPublisher` 注册 afterCommit → `UserCreatedEventHandler`（`@TransactionalEventListener(AFTER_COMMIT)`）→ `NotificationGateway`
- 失败策略：连接失败/超时（`ResourceAccessException`）降级返回 false 并记 WARN，不重试、不影响主流程

## Related Docs
- API Spec: `docs/design/api-spec-v1.md#user-management`
- Domain Model: `docs/design/domain-model.md#user-aggregate`

---

## Backlog（后续典型需求，暂不实现）

### REQ-002: 订单管理（计划）
聚合根 + 值对象计算（`PricingPolicy` 阶梯折扣）+ 库存不足等业务规则异常 + 订单事件外发。覆盖：复杂领域模型、值对象内聚计算、跨实体编排（`PricingService`）。

### REQ-003: 认证授权（计划）
Spring Security + JWT 登录、角色权限（`@PreAuthorize`）、`AccessDeniedException` 403 处理链、Security 入口点与 `ApiResponse` 一致的错误体。

### REQ-004: API 契约测试接入（计划）
为现有端点补充 Spring Cloud Contract 契约定义与 Stub 生成（`contract/` 包）。

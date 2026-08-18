# 领域模型

> 对应需求：`docs/requirements/REQ-001-user-management.md`。分层与包结构遵循 `docs/SpringBoot六边形架构包结构设计指南.md`。

## User Aggregate

### 实体：User（`domain/model/User.java`）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | `UserId` | 标识 VO，数据库自增 `Long` |
| name | `String` | 2–50 字符，非空 |
| email | `Email` | 值对象（格式校验内聚） |
| status | `UserStatus` | 状态枚举，默认 `ACTIVE` |

**不变式：**
- `name` 构造与变更时必须非空且 2–50 字符
- `email` 经 `Email` VO 构造校验，非法值无法被创建
- 状态变更只能经领域方法，非法转换抛 `UserAlreadyInactiveException`

**领域方法：**

```java
User.changeName(String newName)      // 校验后变更
User.changeEmail(Email newEmail)     // VO 自校验
User.activate()                      // INACTIVE -> ACTIVE
User.deactivate()                    // ACTIVE -> INACTIVE，重复停用抛异常
```

**持久化注意：** `version`（乐观锁）、`createdAt/updatedAt`（审计）、`deletedAt`（软删除）均为持久化细节，落在 `UserJpaEntity`（infrastructure），领域模型不感知。

### 值对象

| VO | 类型 | 校验规则 |
|----|------|---------|
| `UserId` | `record(Long value)` | 非空、正数 |
| `Email` | `record(String value)` | 正则 `^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$`，非法抛 `IllegalArgumentException`（构造期快失败） |

### 状态机：UserStatus

```
ACTIVE ──deactivate()──→ INACTIVE
INACTIVE ──activate()──→ ACTIVE
ACTIVE ──deactivate()──→ ✗ UserAlreadyInactiveException（同态转换拒绝）
```

## 领域事件

| 事件 | 载荷 | 发布时机 |
|------|------|---------|
| `UserCreatedEvent` | `userId, email, occurredAt` | `UserService.create()` 事务内发布（`EventPublisher` 端口），afterCommit 后由 Handler 消费 |

## 类型化业务异常（`domain/exception/`）

| 异常 | CODE | HTTP | 场景 |
|------|------|------|------|
| `UserNotFoundException` | `USER_NOT_FOUND` | 404 | 按 id 查询/更新/删除不存在 |
| `EmailAlreadyExistsException` | `EMAIL_ALREADY_EXISTS` | 409 | 创建/更新时邮箱被占用 |
| `UserAlreadyInactiveException` | `USER_ALREADY_INACTIVE` | 409 | 已停用用户再次停用 |
| `UserVersionConflictException` | `USER_VERSION_CONFLICT` | 409 | 乐观锁版本不匹配 |

公共基类：`DomainException`（`errorCode` + message，业务上下文以类型化字段携带）。

## 端口清单

### 入端口（`application/port/in/`）

| UseCase | 方法 |
|---------|------|
| `CreateUserUseCase` | `UserDto create(CreateUserCommand)` |
| `GetUserUseCase` | `UserDto findById(Long id)` |
| `ListUsersUseCase` | `Page<UserDto> list(String name, UserStatus status, Pageable)` |
| `UpdateUserUseCase` | `UserDto update(UpdateUserCommand)` |
| `ChangeUserStatusUseCase` | `UserDto changeStatus(Long id, UserStatus target)` |
| `DeleteUserUseCase` | `void delete(Long id)` |

### 出端口（`application/port/out/`）

| Port | 方法 | 适配器实现 |
|------|------|-----------|
| `UserRepository` | `findById / findByEmail / existsByEmailAndIdNot / save / delete / findAll(criteria, Pageable)` | `UserRepositoryAdapter`（JPA） |
| `EventPublisher` | `publish(DomainEvent)` | `LocalEventPublisher`（afterCommit → Spring 事件） |
| `NotificationGateway` | `boolean sendWelcome(Long userId, String email)` | `NotificationGatewayAdapter`（RestClient） |

## 对象映射关系

```
Web 层                     应用层                      领域层                 持久化
CreateUserRequest  ─WebMapper→  CreateUserCommand  ─Service→  User        ─PersistenceMapper→  UserJpaEntity
UserResponse       ←WebMapper─  UserDto           ←─from()──  User        ←─toDomain()────────  UserJpaEntity
```

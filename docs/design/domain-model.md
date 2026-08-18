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

---

## Pricing（定价，REQ-002 领域先行）

> 领域层先行片段（2026-08-18）：为验证 `domain/service` 领域策略模式而落地，**尚无生产调用方**；REQ-002 订单管理落地时接入 Order 聚合与应用服务。

### 值对象：DiscountRate（`domain/model/DiscountRate.java`）

| 字段 | 类型 | 说明 |
|------|------|------|
| value | `BigDecimal` | 折扣率，[0, 1]；语义为「减免比例」（0.10 = 减 10%），非支付乘数 |

**不变式：** [0,1] 区间校验内聚构造期；`ZERO` 为零折扣常量。

**领域方法：**

```java
DiscountRate.applyTo(BigDecimal amount)   // 返回减免后金额（amount × (1 − rate)）
```

> 精度与舍入（scale / RoundingMode）未定，随 REQ-002 的 Money 决策一并确定。

### 值对象：PriceScheme（`domain/model/PriceScheme.java`）

| 字段 | 类型 | 说明 |
|------|------|------|
| basePrice | `BigDecimal` | 单价，非负 |
| discountRate | `DiscountRate` | 折扣率 VO |

**领域方法：**

```java
PriceScheme.calculateFinalPrice(int quantity)   // 单价 × 数量 → DiscountRate.applyTo
```

### 领域策略：VolumeDiscountPolicy（`domain/service/VolumeDiscountPolicy.java`）

按历史订单数（跨聚合事实，由应用层查好传入）确定折扣率：满 100 单 15%、满 10 单 10%、其余 `ZERO`。纯规则计算、零框架依赖、不访问端口（由 `HexagonalArchitectureTest.domainServicesArePurePolicies` 守护）。

**组合方式（REQ-002 应用层接入时）：** `count = orderRepository.countByUserId(userId)` → `rate = volumeDiscountPolicy.discountRateFor(count)` → `new PriceScheme(unitPrice, rate).calculateFinalPrice(qty)`。衔接经 `DiscountRate` 类型保证，组合验证见 `unit/domain/PricingCompositionTest`。

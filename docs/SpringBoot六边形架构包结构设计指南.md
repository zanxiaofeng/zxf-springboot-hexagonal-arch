# Spring Boot 六边形架构（Hexagonal Architecture）包结构设计指南

六边形架构（又称端口与适配器架构）是一种以业务领域为核心、将应用逻辑与外部基础设施彻底解耦的软件架构风格。在 Spring Boot 项目中采用六边形架构，可以显著提升代码的可测试性、可维护性和技术栈替换的灵活性。

> **本文档是本项目架构设计的唯一权威。** `.claude/rules/` 下的各规范文件均以本文档确立的分层与包结构为准。

**技术栈基线：** Java 21 · Spring Boot 4.1.x · Spring Framework 7 · MySQL 8.0（生产）· Testcontainers（测试）· Maven 3.9+

---

## 一、核心思想

六边形架构的精髓可以用一句话概括：**依赖方向永远向内指向领域层。**

- **领域层（Domain）** 位于最中心，只包含纯粹的业务逻辑，零框架依赖——不引入 Spring、JPA、Kafka 等任何框架，可以在纯 JVM 环境下毫秒级运行单元测试。
- **应用层（Application）** 包裹领域层，负责用例编排，定义系统与外部交互的端口（Port）。应用层允许使用 Spring 装配注解（`@Service`、`@Transactional`、`@RequiredArgsConstructor`），但不依赖任何具体基础设施技术。
- **基础设施层（Infrastructure）** 位于最外层，通过适配器（Adapter）实现端口，连接数据库、消息队列、HTTP 接口等外部系统。

**外层可以依赖内层，内层绝对不可依赖外层。这是六边形架构的铁律。**

---

## 二、包结构设计

以下是一个完整的 Spring Boot 六边形架构包结构（单模块）：

```text
com.example.myapp
├── domain                                  # 领域层（零框架依赖）
│   ├── model                               # 实体、值对象
│   │   ├── User.java
│   │   ├── Order.java
│   │   ├── UserId.java                     # 标识符值对象
│   │   ├── Email.java                      # 带校验规则的值对象
│   │   └── PricingPolicy.java              # 定价规则（值对象，内聚纯计算逻辑）
│   ├── event                               # 领域事件
│   │   ├── DomainEvent.java                # 事件标记接口
│   │   └── OrderCreatedEvent.java
│   └── exception                           # 类型化业务异常
│       ├── DomainException.java            # 公共基类（可选）
│       ├── UserNotFoundException.java
│       └── InsufficientStockException.java
├── application                             # 应用层
│   ├── port
│   │   ├── in                              # 入端口（Driving Port / UseCase）
│   │   │   ├── GetUserUseCase.java
│   │   │   └── CreateOrderUseCase.java
│   │   └── out                             # 出端口（Driven Port）
│   │       ├── UserRepository.java         # 仓库接口唯一定义处
│   │       ├── OrderRepository.java
│   │       ├── EventPublisher.java         # 领域事件发布端口
│   │       └── PaymentGateway.java         # 外部系统端口
│   ├── service                             # 应用服务（用例编排）
│   │   ├── UserService.java
│   │   ├── OrderService.java
│   │   └── PricingService.java             # 跨实体编排逻辑
│   └── dto                                 # 应用层 DTO / Command / Query
│       ├── CreateOrderCommand.java
│       ├── OrderItemCommand.java
│       └── UserDto.java
├── infrastructure                          # 基础设施层（最外层）
│   ├── adapter
│   │   ├── in                              # 入站适配器（Upstream / 驱动适配器）
│   │   │   ├── web
│   │   │   │   ├── controller
│   │   │   │   │   └── OrderController.java
│   │   │   │   ├── dto                     # 请求/响应 DTO
│   │   │   │   │   └── OrderRequest.java
│   │   │   │   ├── common                  # ApiResponse 等传输层公共对象
│   │   │   │   │   └── ApiResponse.java
│   │   │   │   ├── mapper                  # Web 层对象映射
│   │   │   │   │   └── OrderWebMapper.java
│   │   │   │   └── exception               # 异常到 HTTP 响应的映射
│   │   │   │       └── GlobalExceptionHandler.java
│   │   │   ├── messaging                   # 消息消费者
│   │   │   │   └── OrderEventListener.java
│   │   │   └── scheduler                   # 定时任务
│   │   │       └── DailyReportJob.java
│   │   └── out                             # 出站适配器（Downstream / 被驱动适配器）
│   │       ├── persistence
│   │       │   ├── entity                  # JPA 实体
│   │       │   │   └── UserJpaEntity.java
│   │       │   ├── repository              # Spring Data JPA
│   │       │   │   └── UserJpaRepository.java
│   │       │   ├── adapter                 # 仓库适配器实现
│   │       │   │   └── UserRepositoryAdapter.java
│   │       │   ├── mapper                  # 持久化层对象映射
│   │       │   │   └── UserPersistenceMapper.java
│   │       │   └── config                  # JPA 配置（就近管理）
│   │       │       └── JpaConfig.java
│   │       ├── messaging                   # 消息生产者
│   │       │   ├── KafkaEventPublisher.java
│   │       │   └── config
│   │       │       └── KafkaConfig.java
│   │       └── external                    # 外部系统调用
│   │           ├── PaymentGatewayAdapter.java
│   │           └── config
│   │               └── PaymentConfig.java
│   └── config                              # 仅保留跨适配器共享的全局配置
│       └── SecurityConfig.java
└── MyAppApplication.java                   # Spring Boot 启动类
```

**测试代码位于 `src/test/java` 下，按相同基包组织：**

```text
src/test/java/com.example.myapp
├── unit                                    # 纯逻辑测试，零容器
│   ├── domain
│   │   └── PricingPolicyTest.java
│   └── application
│       └── OrderServiceTest.java
├── integration                             # 适配器集成测试
│   └── UserRepositoryAdapterTest.java
└── e2e                                     # 端到端测试
    └── OrderFlowTest.java
```

**命名约定：**

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 入端口 | `{Action}{Entity}UseCase` | `CreateOrderUseCase` |
| 应用服务 | `{Entity}Service` | `OrderService` |
| 出端口（仓库） | `{Entity}Repository` | `UserRepository` |
| 出端口（外部系统） | `{Service}Gateway` | `PaymentGateway` |
| JPA 实体 | `{Entity}JpaEntity` | `UserJpaEntity` |
| Spring Data 接口 | `{Entity}JpaRepository` | `UserJpaRepository` |
| 仓库适配器 | `{Entity}RepositoryAdapter` | `UserRepositoryAdapter` |
| 持久化映射 | `{Entity}PersistenceMapper` | `UserPersistenceMapper` |
| Web 映射 | `{Entity}WebMapper` | `OrderWebMapper` |
| 应用层 DTO | `{Action}{Entity}Command` / `{Entity}Dto` | `CreateOrderCommand` / `UserDto` |
| Web 层 DTO | `{Action}{Entity}Request` / `{Entity}Response` | `CreateOrderRequest` / `UserResponse` |
| 领域异常 | `{BusinessCondition}Exception` | `InsufficientStockException` |
| 单元测试 | `{ClassUnderTest}Test` | `PricingPolicyTest` |
| 集成测试 | `{AdapterUnderTest}Test` | `UserRepositoryAdapterTest` |
| 端到端测试 | `{BusinessFlow}Test` | `OrderFlowTest` |

---

## 三、Upstream 与 Downstream 的定位

在六边形架构中，外部系统与应用的交互方向决定了上下游关系：

| 方向 | 六边形术语 | 包位置 | 说明 | 典型组件 |
|------|-----------|--------|------|---------|
| Upstream | Driving Adapter（驱动适配器） | `infrastructure/adapter/in/` | 外部系统驱动你的应用 | REST Controller、消息消费者、定时任务、CLI |
| Downstream | Driven Adapter（被驱动适配器） | `infrastructure/adapter/out/` | 你的应用驱动外部系统 | 数据库、消息生产者、外部 API、文件存储 |

依赖流向如下：

```text
Upstream（外部系统） ─调用──→ Application ─调用──→ Downstream（外部系统）
  Controller/Consumer              Service             DB/MQ/外部API
```

> 注意与 DDD 战略设计中的 Upstream/Downstream Service（团队间协作关系）区分：本文按「交互方向」定义，即 Driving / Driven Adapter。

---

## 四、各层代码示例

### 4.1 领域层（零框架依赖）

领域层只包含纯 Java 代码，不引入 Spring 或其他框架依赖。

#### 实体与值对象

```java
// domain/model/UserId.java
public record UserId(String value) {
    public UserId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("UserId must not be blank");
        }
    }
}

// domain/model/Email.java
public record Email(String value) {
    public Email {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
    }
}

// domain/model/User.java
public class User {
    private final UserId id;
    private String name;
    private Email email;

    public User(UserId id, String name, Email email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public void changeEmail(Email newEmail) {
        this.email = newEmail;
    }

    public void changeName(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        this.name = newName;
    }

    public UserId getId() { return id; }
    public String getName() { return name; }
    public Email getEmail() { return email; }
}
```

#### 值对象承载纯计算逻辑

原可能放在 domain.service 中的纯计算逻辑，内聚到值对象中，领域层不包含「服务」：

```java
// domain/model/PricingPolicy.java
public class PricingPolicy {
    private final BigDecimal basePrice;
    private final BigDecimal discountRate;

    public PricingPolicy(BigDecimal basePrice, BigDecimal discountRate) {
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Base price must not be negative");
        }
        if (discountRate.compareTo(BigDecimal.ZERO) < 0
                || discountRate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Discount rate must be between 0 and 1");
        }
        this.basePrice = basePrice;
        this.discountRate = discountRate;
    }

    /** 纯计算逻辑，无需外部依赖 */
    public BigDecimal calculateFinalPrice(int quantity) {
        BigDecimal subtotal = basePrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal discount = subtotal.multiply(discountRate);
        return subtotal.subtract(discount);
    }
}
```

#### 领域事件与类型化业务异常

```java
// domain/event/DomainEvent.java —— 事件标记接口，为出端口提供类型安全
public interface DomainEvent {
}

// domain/event/OrderCreatedEvent.java
public record OrderCreatedEvent(
    String orderId,
    String userId,
    BigDecimal totalAmount,
    OffsetDateTime occurredAt
) implements DomainEvent {}
```

每个业务条件对应一个异常类型，携带业务上下文，定义稳定的错误码常量供传输层映射：

```java
// domain/exception/DomainException.java —— 公共基类（可选，减少样板）
public abstract class DomainException extends RuntimeException {
    private final String errorCode;          // 稳定的错误码，客户端契约

    protected DomainException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}

// domain/exception/UserNotFoundException.java
public class UserNotFoundException extends DomainException {
    public static final String CODE = "USER_NOT_FOUND";

    private final String userId;

    public UserNotFoundException(String userId) {
        super(CODE, "User not found: %s".formatted(userId));
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}

// domain/exception/InsufficientStockException.java
public class InsufficientStockException extends DomainException {
    public static final String CODE = "INSUFFICIENT_STOCK";

    private final String productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(String productId, int requested, int available) {
        super(CODE, "Insufficient stock for product %s: requested %d, available %d"
                .formatted(productId, requested, available));
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public String getProductId() { return productId; }
    public int getRequested() { return requested; }
    public int getAvailable() { return available; }
}
```

### 4.2 应用层（端口定义与用例编排）

应用层通过端口声明系统需要什么能力，但不关心具体实现。应用服务是 Spring Bean，可以使用 `@Service`、`@Transactional` 等装配注解——这不破坏六边形：应用层的纯净指**不依赖具体基础设施技术**（JPA、Kafka、HTTP 客户端），而非禁止 Spring 容器装配。

#### 入端口（UseCase 接口）

```java
// application/port/in/GetUserUseCase.java
public interface GetUserUseCase {
    UserDto getUserById(String userId);
}

// application/port/in/CreateOrderUseCase.java
public interface CreateOrderUseCase {
    OrderDto createOrder(CreateOrderCommand command);
}
```

#### 出端口（Driven Port）—— 仓库接口唯一定义处

```java
// application/port/out/UserRepository.java
public interface UserRepository {
    Optional<User> findById(UserId id);
    User save(User user);
}

// application/port/out/OrderRepository.java
public interface OrderRepository {
    Order save(Order order);
    Optional<Order> findById(OrderId id);
    long countByUserId(String userId);       // 计数用 count，禁止加载全量列表后 .size()
}

// application/port/out/EventPublisher.java —— 泛型约束在 DomainEvent，类型安全
public interface EventPublisher {
    void publish(DomainEvent event);
}

// application/port/out/PaymentGateway.java —— 外部系统端口
public interface PaymentGateway {
    PaymentResult charge(String orderId, BigDecimal amount);
}
```

**关键设计决策：仓库接口只在此处定义一份，领域层不再重复定义。** 适配器实现此接口即可，消除了两份签名几乎一致的接口带来的维护隐患。

#### 应用层 DTO / Command

```java
// application/dto/CreateOrderCommand.java
public record CreateOrderCommand(
    String userId,
    List<OrderItemCommand> items
) {}

// application/dto/OrderItemCommand.java
public record OrderItemCommand(
    String productId,
    int quantity
) {}

// application/dto/UserDto.java —— from() 静态工厂承载领域对象 → DTO 转换
public record UserDto(
    String id,
    String name,
    String email
) {
    public static UserDto from(User user) {
        return new UserDto(
            user.getId().value(),
            user.getName(),
            user.getEmail().value()
        );
    }
}
```

#### 应用服务（用例编排）

跨实体编排逻辑放在应用层服务中，而非领域层：

```java
// application/service/UserService.java
@Service
@RequiredArgsConstructor
public class UserService implements GetUserUseCase {
    private final UserRepository userRepository;   // 注入出端口接口

    @Override
    public UserDto getUserById(String userId) {
        User user = userRepository.findById(new UserId(userId))
            .orElseThrow(() -> new UserNotFoundException(userId));
        return UserDto.from(user);
    }
}

// application/service/PricingService.java
@Service
@RequiredArgsConstructor
public class PricingService {
    private final OrderRepository orderRepository;

    /** 跨实体编排：需要查询订单历史来计算阶梯折扣 */
    public BigDecimal calculateOrderDiscount(String userId, List<OrderItem> items) {
        long previousOrderCount = orderRepository.countByUserId(userId);
        return determineVolumeDiscount(previousOrderCount);
    }

    private BigDecimal determineVolumeDiscount(long orderCount) {
        // 阶梯折扣规则 ...
        return BigDecimal.ZERO;
    }
}

// application/service/OrderService.java
@Service
@RequiredArgsConstructor
public class OrderService implements CreateOrderUseCase {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
    private final PricingService pricingService;

    @Override
    @Transactional
    public OrderDto createOrder(CreateOrderCommand command) {
        User user = userRepository.findById(new UserId(command.userId()))
            .orElseThrow(() -> new UserNotFoundException(command.userId()));

        // 编排领域对象完成业务
        Order order = Order.create(user, command.items());
        // 跨实体编排：定价
        BigDecimal discount = pricingService.calculateOrderDiscount(
            command.userId(), order.getItems());
        order.applyDiscount(discount);

        Order saved = orderRepository.save(order);

        // 发布领域事件（适配器保证事务提交后才真正外发，见 4.3）
        eventPublisher.publish(new OrderCreatedEvent(
            saved.getId().value(),
            user.getId().value(),
            saved.getTotalAmount(),
            OffsetDateTime.now()
        ));

        return OrderDto.from(saved);
    }
}
```

### 4.3 基础设施层（适配器实现）

基础设施层实现应用层定义的端口，将具体技术细节隔离在外。

#### 出站适配器 —— 持久化

JPA 实体是持久化技术细节，与领域模型分离：乐观锁 `@Version`、审计时间戳 `@PrePersist`/`@PreUpdate` 都落在 JpaEntity 上，领域模型保持纯净。

```java
// infrastructure/adapter/out/persistence/entity/UserJpaEntity.java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserJpaEntity {

    @Id
    private String id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String email;

    @Version
    private Long version;                     // 乐观锁：所有可变实体必须

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = OffsetDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = OffsetDateTime.now(); }
}

// infrastructure/adapter/out/persistence/repository/UserJpaRepository.java
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, String> {
}

// infrastructure/adapter/out/persistence/mapper/UserPersistenceMapper.java
public final class UserPersistenceMapper {
    private UserPersistenceMapper() {}

    public static User toDomain(UserJpaEntity entity) {
        return new User(
            new UserId(entity.getId()),
            entity.getName(),
            new Email(entity.getEmail())
        );
    }

    public static UserJpaEntity toEntity(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setId(user.getId().value());
        entity.setName(user.getName());
        entity.setEmail(user.getEmail().value());
        return entity;
    }
}

// infrastructure/adapter/out/persistence/adapter/UserRepositoryAdapter.java
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {  // 实现 application.port.out 的接口
    private final UserJpaRepository jpaRepository;

    @Override
    public Optional<User> findById(UserId id) {
        return jpaRepository.findById(id.value())
            .map(UserPersistenceMapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpaEntity saved = jpaRepository.save(UserPersistenceMapper.toEntity(user));
        return UserPersistenceMapper.toDomain(saved);
    }
}

// infrastructure/adapter/out/persistence/config/JpaConfig.java
// 注意：Spring Boot 4 中 @EntityScan 的包名已迁移
@Configuration
@EntityScan(basePackages = "com.example.myapp.infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.example.myapp.infrastructure.adapter.out.persistence.repository")
public class JpaConfig {
}
```

> 乐观锁冲突（`OptimisticLockingFailureException`）由适配器翻译为领域异常（如 `OrderVersionConflictException`），不把 Spring Data 的异常类型泄露到应用层。

#### 出站适配器 —— 消息（事务一致性）

**直接在事务内发送外部消息会造成「事务回滚但消息已发出」的不一致。** `EventPublisher` 的 Kafka 实现必须注册事务同步回调，确保只在事务成功提交后才发送：

```java
// infrastructure/adapter/out/messaging/KafkaEventPublisher.java
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher implements EventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publish(DomainEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // 事务内：延迟到提交成功后再发送，避免「回滚但消息已发出」
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSend(event);
                }
            });
        } else {
            doSend(event);
        }
    }

    private void doSend(DomainEvent event) {
        String topic = "app." + event.getClass().getSimpleName().toLowerCase();
        kafkaTemplate.send(topic, event);
    }
}
```

```java
// infrastructure/adapter/out/messaging/config/KafkaConfig.java
@Configuration
public class KafkaConfig {
    // Spring Boot 自动配置 KafkaTemplate 时通常无需此配置类；
    // 仅在需要定制序列化器、topic 命名等时按需编写（就近管理）
}
```

> Kafka 依赖坐标是 `org.springframework.kafka:spring-kafka`（Spring Boot 没有名为 `spring-boot-starter-kafka` 的 starter），由 Boot 自动配置。

#### 出站适配器 —— 外部系统

```java
// infrastructure/adapter/out/external/PaymentGatewayAdapter.java
@Component
@RequiredArgsConstructor
public class PaymentGatewayAdapter implements PaymentGateway {
    private final RestClient paymentRestClient;   // 就近 config 中定义的 Bean

    @Override
    public PaymentResult charge(String orderId, BigDecimal amount) {
        try {
            PaymentResponse response = paymentRestClient.post()
                .uri("/api/v1/payments")
                .body(new ChargeRequest(orderId, amount))
                .retrieve()
                .body(PaymentResponse.class);
            return PaymentResult.success(response.transactionId());
        } catch (ResourceAccessException ex) {
            // 连接失败/超时：瞬态错误，按业务决策降级
            return PaymentResult.retryable();
        }
    }
}

// infrastructure/adapter/out/external/config/PaymentConfig.java
@Configuration
public class PaymentConfig {
    @Bean
    public RestClient paymentRestClient(RestClient.Builder builder,
            @Value("${app.downstream.payment.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
```

#### 入站适配器 —— Web

```java
// infrastructure/adapter/in/web/dto/UserResponse.java
public record UserResponse(
    String id,
    String name,
    String email
) {
    public static UserResponse from(UserDto dto) {
        return new UserResponse(dto.id(), dto.name(), dto.email());
    }
}

// infrastructure/adapter/in/web/mapper/UserWebMapper.java
public final class UserWebMapper {
    private UserWebMapper() {}

    public static UserResponse toResponse(UserDto dto) {
        return UserResponse.from(dto);
    }
}

// infrastructure/adapter/in/web/common/ApiResponse.java —— 统一响应信封
public record ApiResponse<T>(
    String code,
    T data,
    String message,
    OffsetDateTime timestamp,
    String traceId
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("000000", data, null, OffsetDateTime.now(), null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(code, null, message, OffsetDateTime.now(), null);
    }
}

// infrastructure/adapter/in/web/controller/UserController.java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final GetUserUseCase getUserUseCase;   // 依赖入端口接口，不依赖实现

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUser(@PathVariable String id) {
        UserDto dto = getUserUseCase.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(UserWebMapper.toResponse(dto)));
    }
}
```

```java
// infrastructure/adapter/in/web/exception/GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(InsufficientStockException.class)   // 领域异常在此映射为 HTTP
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)   // @Valid 校验失败
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", "Request validation failed"));
    }

    @ExceptionHandler(Exception.class)   // 兜底：固定文案，绝不回显 ex.getMessage()
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR", "Internal server error"));
    }
}
```

> 完整的异常映射矩阵（含 `AccessDeniedException`、`HttpMessageNotReadableException` 等 Spring 内置异常）见 `.claude/rules/exception-handling.md`。

---

## 五、与 DDD 分层架构的关系

六边形架构与领域驱动设计（DDD）的分层架构高度契合：

| DDD 分层 | 六边形架构对应 | 职责 |
|---------|--------------|------|
| 用户接口层 | `adapter.in`（Upstream） | 接收外部请求，转换为应用层指令 |
| 应用层 | `application` | 编排用例，协调领域对象完成业务目标 |
| 领域层 | `domain` | 封装核心业务规则与领域知识 |
| 基础设施层 | `adapter.out`（Downstream） | 提供技术能力支撑，如持久化、消息、外部调用 |

---

## 六、异常处理分层策略

异常在不同层级有不同的职责：

- **领域层**：定义类型化业务异常（如 `InsufficientStockException`），表达业务规则的违反；每个异常携带稳定错误码常量（客户端契约）与业务上下文。
- **应用层**：捕获领域异常，必要时包装为应用级异常，但不应吞掉原始异常信息（必须保留 cause）。
- **基础设施层入站适配器**：负责将领域异常映射为具体的传输协议响应（如 HTTP 状态码），通过 `GlobalExceptionHandler` 统一管理；兜底异常使用固定文案，不回显内部消息。

```text
领域异常 ─抛出──→ 应用服务 ─传播──→ Controller ─映射──→ HTTP 响应
（DomainException + 错误码）                    （GlobalExceptionHandler）
```

---

## 七、测试分层策略

| 测试层级 | 位置 | 依赖范围 | 工具 | 说明 |
|---------|------|---------|------|------|
| 领域单元测试 | `unit/domain/` | 零依赖 | JUnit 5 + AssertJ | 测试实体行为、值对象计算逻辑，毫秒级执行 |
| 应用服务测试 | `unit/application/` | domain + mock | JUnit 5 + Mockito | Mock 出端口，验证用例编排逻辑 |
| 适配器集成测试 | `integration/` | 全栈 | `@SpringBootTest` / `@DataJpaTest` + Testcontainers（MySQL） | 验证适配器与真实基础设施的交互 |
| 端到端测试 | `e2e/` | 全栈 | `@SpringBootTest` + `@AutoConfigureMockMvc` + WireMock | 验证完整请求链路，下游用 WireMock 打桩 |

> Spring Boot 4 注意：`@MockBean`/`@SpyBean` 已移除，改用 `@MockitoBean`/`@MockitoSpyBean`；`@SpringBootTest` 不再自动注入 MockMvc，必须加 `@AutoConfigureMockMvc`。

### 领域层单元测试（零依赖）

```java
// unit/domain/PricingPolicyTest.java
class PricingPolicyTest {

    @Test
    void calculateFinalPrice_withQuantityAndDiscount() {
        PricingPolicy policy = new PricingPolicy(
            new BigDecimal("100.00"),
            new BigDecimal("0.10")
        );

        BigDecimal result = policy.calculateFinalPrice(5);

        // 100 * 5 = 500, 500 * 0.10 = 50, 500 - 50 = 450
        assertThat(result).isEqualByComparingTo(new BigDecimal("450.00"));
    }

    @Test
    void constructor_rejectsNegativeBasePrice() {
        assertThatThrownBy(() -> new PricingPolicy(
            new BigDecimal("-1"), new BigDecimal("0.1")
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
```

### 应用服务测试（Mock 出端口）

```java
// unit/application/OrderServiceTest.java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock UserRepository userRepository;
    @Mock OrderRepository orderRepository;
    @Mock EventPublisher eventPublisher;
    @Mock PricingService pricingService;

    @InjectMocks
    OrderService orderService;

    @Test
    void createOrder_success() {
        // Given
        User user = new User(new UserId("u1"), "Alice", new Email("alice@example.com"));
        when(userRepository.findById(any())).thenReturn(Optional.of(user));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(pricingService.calculateOrderDiscount(anyString(), anyList()))
            .thenReturn(new BigDecimal("10.00"));

        CreateOrderCommand command = new CreateOrderCommand("u1", List.of());

        // When
        OrderDto result = orderService.createOrder(command);

        // Then
        assertThat(result).isNotNull();
        verify(eventPublisher).publish(any(OrderCreatedEvent.class));
    }

    @Test
    void createOrder_userNotFound_throwsException() {
        when(userRepository.findById(any())).thenReturn(Optional.empty());

        CreateOrderCommand command = new CreateOrderCommand("unknown", List.of());

        assertThatThrownBy(() -> orderService.createOrder(command))
            .isInstanceOf(UserNotFoundException.class);
    }
}
```

### 适配器集成测试（Testcontainers，真实 MySQL）

```java
// integration/UserRepositoryAdapterTest.java
@SpringBootTest
@Testcontainers
class UserRepositoryAdapterTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    UserRepository userRepository;   // 注入的是适配器实现，但面向接口编程

    @Test
    void save_and_findById_roundTrip() {
        User user = new User(new UserId("u1"), "Alice", new Email("alice@example.com"));

        userRepository.save(user);

        Optional<User> found = userRepository.findById(new UserId("u1"));
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }
}
```

> 测试数据库与生产同为 MySQL（Testcontainers），不再依赖 H2 模拟，消除方言差异带来的假阳性。

### 端到端测试（完整请求链路 + WireMock 下游打桩）

```java
// e2e/OrderFlowTest.java
@SpringBootTest
@AutoConfigureMockMvc          // Spring Boot 4 必须显式声明，MockMvc 不再自动注入
class OrderFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createOrder_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userId": "u1", "items": [{"productId": "p1", "quantity": 2}]}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.code").value("000000"));
    }
}
```

---

## 八、依赖方向速查

```text
adapter.in ─调用──→ application.port.in ─调用──→ application.port.out ←──实现── adapter.out
(Controller)          (UseCase 接口)             (Repository/Gateway 接口)   (Persistence/Messaging/External Adapter)
                          │                            │
                          └──────调用──────→ domain.model ←──────引用────────┘

所有箭头最终指向 domain，domain 不指向任何外层。
```

---

## 九、常见误区与最佳实践

**误区一：将 Spring Data JPA 接口放在领域层**
`JpaRepository` 属于框架特定接口，领域层应只包含纯 Java 代码。仓库接口统一放在 `application/port/out` 中，具体实现放在 `infrastructure/adapter/out/persistence/adapter/` 中。

**误区二：Controller 直接调用领域逻辑**
Controller 必须通过应用服务（UseCase）进行编排，保持领域层的独立性和完整性。

**误区三：DTO 混用不分层**
- `application/dto`：应用层内部传递的 Command、Query、DTO
- `infrastructure/adapter/in/web/dto`：HTTP 请求和响应对象

两者职责不同，应分开管理。

**误区四：端口命名不清晰**
推荐使用 `port.in` 和 `port.out` 明确表达交互方向，比 `api` / `spi` 更加直观易懂。

**误区五：领域层包含「服务」**
领域层应只包含实体自身行为和值对象。跨实体的编排逻辑属于应用层的职责，应放在 `application/service` 中。纯计算逻辑可以内聚到值对象中。

**误区六：映射逻辑散落各处**
`toDomain()` / `from()` 等跨技术栈转换方法应集中在各适配器下的 `mapper/` 目录中；应用层 DTO 与领域对象的转换用 DTO 上的静态工厂（`from()`）承载。

**误区七：配置类集中堆放**
各适配器的私有配置（如 `JpaConfig`、`KafkaConfig`、`PaymentConfig`）应放在对应适配器目录下就近管理，顶层 `config` 仅保留跨适配器共享的全局配置（如 `SecurityConfig`）。

**误区八：事务内外发消息**
在 `@Transactional` 方法内直接调用外部消息系统，事务回滚后消息已发出，造成数据与消息不一致。事件发布适配器必须注册 `TransactionSynchronization.afterCommit`，确保只在事务提交成功后发送（见 4.3）。

**误区九：为拿计数加载全量数据**
`repository.findByUserId(userId).size()` 会把全部实体加载进内存。出端口应提供 `countByUserId` 这类计数方法。

---

## 十、多模块项目结构（进阶）

对于大型项目，可将六边形架构进一步拆分为 Maven 多模块：

```text
myapp/
├── domain/          # 纯 Java 模块，零框架依赖
├── application/     # 依赖 domain，定义入/出端口
├── infrastructure/  # 依赖 application + domain，引入 Spring Boot
└── bootstrap/       # 启动模块，聚合所有依赖
```

这种结构的优势在于：单元测试可以仅依赖 domain 和 application 模块，完全脱离数据库和 Web 容器运行，测试速度极快，且不受基础设施变更的影响。

> **本项目当前采用单模块结构**（见第二章）。多模块是团队与代码规模增长后的演进方向，拆分时机：模块间出现编译瓶颈、或需要限制领域层依赖时。

### 10.1 父 POM（多模块聚合）

```xml
<!-- myapp/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>myapp</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>domain</module>
        <module>application</module>
        <module>infrastructure</module>
        <module>bootstrap</module>
    </modules>

    <properties>
        <java.version>21</java.version>
        <!-- 示例值，以项目实际采用的 Spring Boot 4.1.x 补丁版本为准 -->
        <spring-boot.version>4.1.5</spring-boot.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>

    <!-- 统一依赖版本管理，子模块不写版本号 -->
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>${spring-boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <!-- 关键：BOM 只管理依赖版本，不管理插件版本；
             自建 parent 必须在 pluginManagement 中显式声明 spring-boot-maven-plugin 版本，
             否则 repackage 会使用 Maven 超级 POM 的旧版本插件而失败 -->
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <version>${spring-boot.version}</version>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.13.0</version>
                    <configuration>
                        <release>${java.version}</release>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 10.2 Domain 模块（零框架依赖）

```xml
<!-- myapp/domain/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>myapp</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>domain</artifactId>
    <description>领域层：纯 Java，零框架依赖</description>

    <dependencies>
        <!-- 单元测试（领域层测试零依赖） -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

### 10.3 Application 模块（依赖 domain，定义端口）

```xml
<!-- myapp/application/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>myapp</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>application</artifactId>
    <description>应用层：定义入/出端口，编排用例</description>

    <dependencies>
        <!-- 内部依赖：领域层 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>domain</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- 装配注解与事务边界：@Service / @Transactional（不引入 starter，仅注解依赖） -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-context</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-tx</artifactId>
        </dependency>

        <!-- Lombok：@RequiredArgsConstructor 等样板代码消除 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 单元测试 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
```

> 与单模块保持同一规则：应用服务使用 Spring 装配注解（`@Service`、`@Transactional`），application 模块因此声明 `spring-context` / `spring-tx`（仅注解所需，不含 Web、JPA 等基础设施）。**不引入任何 starter。**

### 10.4 Infrastructure 模块（依赖 application + domain，引入 Spring Boot）

```xml
<!-- myapp/infrastructure/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>myapp</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>infrastructure</artifactId>
    <description>基础设施层：适配器实现，引入 Spring Boot 及中间件</description>

    <dependencies>
        <!-- 内部依赖：应用层 + 领域层 -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>application</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>domain</artifactId>
            <version>${project.version}</version>
        </dependency>

        <!-- 入站适配器：Web（Spring Boot 4 起 starter 更名为 webmvc） -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webmvc</artifactId>
        </dependency>

        <!-- 出站适配器：持久化 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- 出站适配器：消息（无 starter，直接依赖 spring-kafka，按需引入） -->
        <!--
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        -->

        <!-- 出站适配器：外部 HTTP 调用 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-restclient</artifactId>
        </dependency>

        <!-- 数据库迁移 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-flyway</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>

        <!-- 测试：Spring Boot Test + Testcontainers -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mysql</artifactId>
            <scope>test</scope>
        </dependency>
        <!-- Testcontainers 版本由 spring-boot-dependencies BOM 管理 -->
    </dependencies>
</project>
```

### 10.5 Bootstrap 模块（启动模块，聚合所有依赖）

```xml
<!-- myapp/bootstrap/pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.example</groupId>
        <artifactId>myapp</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>bootstrap</artifactId>
    <description>启动模块：聚合所有依赖，包含 Spring Boot 启动类</description>

    <dependencies>
        <!-- 内部依赖：基础设施层（它已传递依赖 application + domain） -->
        <dependency>
            <groupId>com.example</groupId>
            <artifactId>infrastructure</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>

    <!-- Spring Boot 打包插件（版本由父 POM pluginManagement 管理） -->
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <mainClass>com.example.myapp.MyAppApplication</mainClass>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>repackage</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

### 10.6 模块间依赖关系总览

```text
bootstrap ─依赖──→ infrastructure ─依赖──→ application ─依赖──→ domain
   │                      │                       │
   ── 启动类              ├── Spring Boot         ├── spring-context/tx（注解）
                         ├── JPA / Kafka          └── 零框架依赖
                         └── Testcontainers
```

关键设计决策：

- **bootstrap 只依赖 infrastructure**：infrastructure 已通过内部依赖传递引入 application 和 domain，无需重复声明。
- **domain 模块零框架依赖**：不引入 Spring、Hibernate、Kafka、Lombok 等任何依赖，仅保留 JUnit/AssertJ 用于单元测试。领域层可在纯 JVM 环境下毫秒级运行测试。
- **application 模块只引注解依赖**：`spring-context` / `spring-tx` / Lombok（provided），支撑 `@Service` 与 `@Transactional`，不引入任何 starter 与基础设施。
- **测试依赖隔离**：infrastructure 模块引入 Testcontainers；domain 和 application 模块的测试不依赖容器，实现分层测试速度的最大化。

### 10.7 多模块下的包扫描配置

启动类在 bootstrap 模块，组件分布在 infrastructure 模块中，需确保包扫描覆盖正确：

```java
// bootstrap/src/main/java/com/example/myapp/MyAppApplication.java
@SpringBootApplication
public class MyAppApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyAppApplication.class, args);
    }
}
```

启动类位于 `com.example.myapp` 包时默认扫描该包及所有子包（各模块共享同一包前缀），无需显式 `scanBasePackages`。同时，infrastructure 模块中的 JPA 配置类使用精确扫描，避免误扫其他层的类：

```java
// infrastructure/adapter/out/persistence/config/JpaConfig.java
@Configuration
@EntityScan(basePackages = "com.example.myapp.infrastructure.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.example.myapp.infrastructure.adapter.out.persistence.repository")
public class JpaConfig {
}
```

### 10.8 多模块测试策略

| 测试层级 | 所在模块 | 依赖范围 | 工具 | 说明 |
|---------|---------|---------|------|------|
| 领域单元测试 | `domain/src/test/` | 零依赖 | JUnit 5 + AssertJ | 测试实体行为、值对象计算逻辑，毫秒级执行 |
| 应用服务测试 | `application/src/test/` | domain + mock | JUnit 5 + Mockito | Mock 出端口，验证用例编排逻辑 |
| 适配器集成测试 | `infrastructure/src/test/` | 全栈 | `@SpringBootTest` / `@DataJpaTest` + Testcontainers | 验证适配器与真实基础设施的交互 |
| 端到端测试 | `bootstrap/src/test/` | 全栈 | `@SpringBootTest` + `@AutoConfigureMockMvc` + WireMock | 验证完整请求链路 |

注意：application 模块的测试不应启动 Spring 容器。如果需要 `@SpringBootTest`，应放在 infrastructure 或 bootstrap 模块中。application 模块的测试使用 `@ExtendWith(MockitoExtension.class)` 纯 JUnit 测试。

---

## 十一、总结

六边形架构通过端口与适配器的模式，将业务逻辑牢牢锁定在领域层中心，使外部技术细节成为可插拔的组件。在 Spring Boot 项目中，遵循以下原则：

- **依赖向内**：外层依赖内层，领域层零框架依赖。
- **端口隔离**：用 `port.in` 声明系统提供的能力（UseCase），用 `port.out` 声明系统需要的能力（Repository / Gateway / EventPublisher）。仓库接口统一定义在 `application.port.out` 中，不重复定义。
- **适配器实现**：Upstream 放在 `adapter.in/`，Downstream 放在 `adapter.out/`。
- **用例驱动**：以应用服务为核心编排业务流程，Controller 和 Repository 都只是适配器。
- **领域纯粹**：领域层只包含实体、值对象、领域事件和类型化业务异常，不包含服务和仓库接口。
- **映射集中**：跨技术栈的对象转换集中在各适配器的 `mapper/` 目录中；应用层 DTO 转换用静态工厂承载。
- **配置就近**：适配器私有配置放在适配器目录下，全局配置才放顶层。
- **异常分层**：类型化领域异常（稳定错误码 + 业务上下文）由入站适配器统一映射为传输协议响应。
- **事务一致**：事件外发必须延迟到事务提交成功后（`afterCommit`）。
- **测试分层**：`unit`（零容器）→ `integration`（Testcontainers 真实基础设施）→ `e2e`（完整链路 + WireMock 下游打桩）。
- **多模块分层**：domain 零框架依赖，application 仅注解依赖，infrastructure 引入 Spring Boot，bootstrap 聚合启动。

掌握这套包结构设计，你的 Spring Boot 项目将拥有清晰的边界、高度的可测试性，以及从容应对技术栈演进的能力。

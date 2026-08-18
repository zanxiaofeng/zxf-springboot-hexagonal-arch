# springboot-hexagonal-arch

Spring Boot 六边形架构（Ports & Adapters）参考实现。

## 架构权威

- **完整设计指南（唯一权威）**：`docs/SpringBoot六边形架构包结构设计指南.md`
- 分层规范：`.claude/rules/architecture.md`（及各专题文件）
- 依赖方向：`infrastructure → application → domain`，domain 零框架依赖
- 端口唯一定义处：`application/port/out`（Repository / Gateway / EventPublisher）
- 错误体系：类型化领域异常（`domain/exception/` + `CODE` 常量），HTTP 映射仅在 `GlobalExceptionHandler`

## 技术栈

Java 21 · Spring Boot 4.1.x · MySQL 8.0 · Flyway · Testcontainers · WireMock 3.x · Maven 3.9+

## 构建与测试

```bash
mvn test          # 全量测试（需要 Docker：Testcontainers MySQL + WireMock）
mvn spring-boot:run   # 本地运行（需 localhost:3306 MySQL，库名 hexagonal，Flyway 自动建表）
```

测试分层（`src/test/java/com/zxf/hexagonal/`）：
`unit/`（零容器，毫秒级）→ `integration/`（适配器 @DataJpaTest + Testcontainers）→ `e2e/`（MockMvc + WireMock 全链路，继承 `BaseE2ETest`）

## Sprint Status

| 需求 | 状态 |
|------|------|
| REQ-001 用户管理（walking skeleton） | ✅ 完成（37 测试全绿） |
| REQ-002 订单管理 | 计划 |
| REQ-003 认证授权 | 计划 |
| REQ-004 API 契约测试接入 | 计划 |

## 文档导航

- 需求：`docs/requirements/`（模板：`docs/templates/requirement-template.md`）
- 设计：`docs/design/api-spec-v1.md`、`docs/design/domain-model.md`
- 新增业务模块 Checklist：`.claude/rules/architecture.md` §10（Domain → Application → Infrastructure → Web → Test 五阶段）

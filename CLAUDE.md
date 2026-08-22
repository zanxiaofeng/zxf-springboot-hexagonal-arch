# springboot-hexagonal-arch

Spring Boot 六边形架构（Ports & Adapters）参考实现。

## 架构权威

- **完整设计指南（唯一权威）**：`docs/SpringBoot六边形架构包结构设计指南.md`
- 分层规范：`.claude/rules/architecture.md`（及各专题文件）
- 依赖方向：`infrastructure → application → domain`，domain 零框架依赖
- 端口唯一定义处：`application/port/out`（Repository / Gateway / EventPublisher）
- 错误体系：类型化领域异常（`domain/exception/` + `CODE` 常量），HTTP 映射仅在 `GlobalExceptionHandler`

## 规范适配（项目选型记录）

`.claude/rules/` 为服务所有同类项目的中立通用规范，涉及项目间可变选型处均以判据形式给出；本项目的实际选型如下，与规范正文冲突时以本段为准：

| 选型项 | 本项目状态 | 规范出处 |
|---|---|---|
| 业务异常表达模式 | **模式 A（类型化领域异常）**：`domain/exception/` + `CODE` 常量 | `exception-handling.md` §2.1 |
| ApiResponse 信封 | 含 `errors[]` 标准结构（与代码一致） | `api-conventions.md` |
| NullAway + Error Prone | **未接入**；接入时按 `java-coding-standard.md` §4.2 步骤（先 WARN 后 ERROR） | `java-coding-standard.md` §4.2 |
| lombok.config | 未创建；启用 `@Nullable` 构造器复制路径时按 §5.2 先创建（`copyableAnnotations`）；`lombok.addNullAnnotations = jspecify` 未启用 | `java-coding-standard.md` §5.2 |
| 错误消息外化（i18n） | **强制策略**：注解只写消息键（`<域>.<字段>.<约束>` 三段式），存量中文字面量收敛到资源文件 | `validation.md` §2.10 |
| 模块结构 | 当前单模块；按 `architecture.md` §11 时机演进 | `architecture.md` §11 |

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
| REQ-002 订单管理 | 计划（定价领域策略已先行，见 `docs/design/domain-model.md`） |
| REQ-003 认证授权 | 计划 |
| REQ-004 API 契约测试接入 | 计划 |

## 文档导航

- 需求：`docs/requirements/`（模板：`docs/templates/requirement-template.md`）
- 设计：`docs/design/api-spec-v1.md`、`docs/design/domain-model.md`
- 新增业务模块 Checklist：`.claude/rules/architecture.md` §10（Domain → Application → Infrastructure → Web → Test 五阶段）

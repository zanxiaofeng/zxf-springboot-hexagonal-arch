# API Specification v1

> Base URL: `/api/v1`。规范依据 `.claude/rules/api-conventions.md`。

## 通用约定

### 响应信封 `ApiResponse<T>`

```json
{
  "code": "000000",
  "data": { },
  "message": null,
  "timestamp": "2026-08-18T10:00:00+08:00",
  "traceId": null
}
```

### 错误码

| code | HTTP | 场景 |
|------|------|------|
| `000000` | 200/201 | 成功 |
| `VALIDATION_ERROR` | 400 | Bean Validation 失败（含 `errors[]` 明细） |
| `BAD_REQUEST` | 400/405/415 | 请求不可读 / 方法不支持 / 媒体类型不支持 |
| `NOT_FOUND` | 404 | 无匹配路由 |
| `USER_NOT_FOUND` | 404 | 用户不存在或已删除 |
| `EMAIL_ALREADY_EXISTS` | 409 | 邮箱被占用 |
| `USER_ALREADY_INACTIVE` | 409 | 非法状态转换 |
| `USER_VERSION_CONFLICT` | 409 | 乐观锁冲突 |
| `INTERNAL_ERROR` | 500 | 未预期异常（固定文案，不回显内部信息） |

### 分页

请求：`?page=0&size=20&sort=id,asc`（上限 100，全局配置）；响应 `data` 为 Spring Data `Page` 结构（`content/totalElements/totalPages/number/size`）。

---

## User Management

### 创建用户 — `POST /api/v1/users`

请求：
```json
{ "name": "Alice", "email": "alice@example.com" }
```

响应 `201`（头 `Location: /api/v1/users/{id}`）：
```json
{
  "code": "000000",
  "data": { "id": 1, "name": "Alice", "email": "alice@example.com", "status": "ACTIVE", "version": 0 },
  "message": null,
  "timestamp": "2026-08-18T10:00:00+08:00",
  "traceId": null
}
```

错误：400 `VALIDATION_ERROR`（name 为空/长度非法、email 格式非法）；409 `EMAIL_ALREADY_EXISTS`。

> 下游副作用：事务提交后 `POST {notification}/api/v1/notifications`（欢迎通知）；下游失败降级，不影响响应。

### 查询用户 — `GET /api/v1/users/{id}`

响应 `200`：`data` 同上。错误：400（id 非正数）；404 `USER_NOT_FOUND`。

### 用户列表 — `GET /api/v1/users`

参数：`page`、`size`、`sort`、可选 `name`（模糊）、可选 `status`（`ACTIVE`/`INACTIVE`）。

响应 `200`：
```json
{
  "code": "000000",
  "data": {
    "content": [ { "id": 1, "name": "Alice", "email": "alice@example.com", "status": "ACTIVE", "version": 0 } ],
    "totalElements": 1, "totalPages": 1, "number": 0, "size": 20
  },
  "timestamp": "2026-08-18T10:00:00+08:00",
  "traceId": null
}
```

### 更新用户（部分更新） — `PUT /api/v1/users/{id}`

请求（`null` 字段 = 不更新；`version` 必填）：
```json
{ "version": 0, "name": "Alice Chen", "email": null }
```

响应 `200`：更新后的 `UserResponse`（`version` 自增）。

错误：400 校验失败；404 `USER_NOT_FOUND`；409 `EMAIL_ALREADY_EXISTS` / `USER_VERSION_CONFLICT`。

### 状态流转 — `PATCH /api/v1/users/{id}/status`

请求：
```json
{ "status": "INACTIVE" }
```

响应 `200`：更新后的 `UserResponse`。错误：404 / 409 `USER_ALREADY_INACTIVE`。

### 删除用户（软删除） — `DELETE /api/v1/users/{id}`

响应 `204`（无响应体）。错误：404 `USER_NOT_FOUND`。

-- 种子数据（确定性 ID 1-99；每个测试方法执行前由 cleanup + init 重建）
INSERT INTO users (id, name, email, status, version, created_at)
VALUES (1, 'Alice', 'alice@example.com', 'ACTIVE', 0, '2026-01-01 00:00:00');

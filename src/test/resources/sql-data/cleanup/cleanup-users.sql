-- 清理脚本（幂等）：按外键反序 DELETE，不用 TRUNCATE
DELETE FROM users;

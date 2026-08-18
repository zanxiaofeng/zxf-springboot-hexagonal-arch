CREATE TABLE users (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(50)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NULL,
    updated_at TIMESTAMP(6) NULL,
    deleted_at TIMESTAMP(6) NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    KEY idx_users_status (status),
    KEY idx_users_deleted_at (deleted_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

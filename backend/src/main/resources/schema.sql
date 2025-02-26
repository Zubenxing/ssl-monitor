-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    full_name VARCHAR(100),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    last_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 初始用户数据
INSERT INTO users (username, password, email, role) 
VALUES ('admin', 'admin123', 'admin@example.com', 'ADMIN')
ON DUPLICATE KEY UPDATE username=username;

INSERT INTO users (username, password, email, role) 
VALUES ('user', 'user123', 'user@example.com', 'USER')
ON DUPLICATE KEY UPDATE username=username; 
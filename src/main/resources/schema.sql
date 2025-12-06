-- Settings
CREATE TABLE IF NOT EXISTS settings (
    tag_id VARCHAR(100) NOT NULL PRIMARY KEY,
    value_json JSON NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Languages and messages
CREATE TABLE IF NOT EXISTS languages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(32) UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS messages (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    tag_id VARCHAR(255) NOT NULL,
    language_id INT NOT NULL,
    UNIQUE (tag_id, language_id),
    message TEXT NOT NULL,
    FOREIGN KEY (language_id) REFERENCES languages(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Users and session/login data
CREATE TABLE IF NOT EXISTS users (
    id INT PRIMARY KEY AUTO_INCREMENT,
    mojang_id VARCHAR(36) NOT NULL UNIQUE,
    username VARCHAR(16) NOT NULL,
    first_login DATETIME NULL,
    system_user BOOLEAN NOT NULL DEFAULT FALSE,
    debug_user BOOLEAN NOT NULL DEFAULT FALSE,
    debug_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    language_id INT NOT NULL DEFAULT 1,
    FOREIGN KEY (language_id) REFERENCES languages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

CREATE TABLE IF NOT EXISTS user_playtime (
    id INT PRIMARY KEY,
    play_time INT NOT NULL DEFAULT 0,
    login_count INT NOT NULL DEFAULT 0,
    FOREIGN KEY (id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_login (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL,
    login_time DATETIME NOT NULL,
    logout_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    ip_address VARCHAR(45) NOT NULL,
    client_brand VARCHAR(50) NULL,
    protocol_version INT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_user_id ON user_login (user_id);
CREATE INDEX IF NOT EXISTS idx_ip_address ON user_login (ip_address);

CREATE TABLE IF NOT EXISTS user_session (
    id VARCHAR(36) PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    ip_address VARCHAR(45) NOT NULL,
    client_brand VARCHAR(50) NULL,
    protocol_version INT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Groups and colors
CREATE TABLE IF NOT EXISTS groups (
    id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(100) NOT NULL UNIQUE,
    priority INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_color_type (
    id INT PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_color (
    group_id INT,
    type_id INT,
    PRIMARY KEY (group_id, type_id),
    value TEXT NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (type_id) REFERENCES group_color_type(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS group_permission (
    group_id INT,
    permission VARCHAR(200),
    PRIMARY KEY (group_id, permission),
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_group_perm_groupId_exp ON group_permission (group_id, expires_at);

CREATE TABLE IF NOT EXISTS group_parent (
    group_id INT,
    parent_id INT,
    PRIMARY KEY (group_id, parent_id),
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (group_id) REFERENCES groups(id),
    FOREIGN KEY (parent_id) REFERENCES groups(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_group_parent_groupId_exp ON group_parent (group_id, expires_at);

-- User inheritance and permissions
CREATE TABLE IF NOT EXISTS user_parent (
    user_id INT,
    parent_id INT,
    PRIMARY KEY (user_id, parent_id),
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (parent_id) REFERENCES groups(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_user_parent_userId_exp ON user_parent (user_id, expires_at);

CREATE TABLE IF NOT EXISTS user_permission (
    user_id INT,
    permission VARCHAR(200),
    PRIMARY KEY (user_id, permission),
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_user_perm_userId_exp ON user_permission (expires_at, user_id);

-- Punishment types, reasons, logs, and current states
CREATE TABLE IF NOT EXISTS punishment_types (
    id INT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    ip_type BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS punishment_reasons (
    id INT PRIMARY KEY,
    type_id INT NOT NULL,
    reason_text TEXT NOT NULL,
    duration_secs BIGINT NULL,
    auto_flag_ip BOOLEAN NOT NULL DEFAULT FALSE,
    auto_punish BOOLEAN NOT NULL DEFAULT FALSE,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_reason_type ON punishment_reasons (type_id);

CREATE TABLE IF NOT EXISTS punishment_logs (
    id VARCHAR(36) PRIMARY KEY,
    action ENUM('CREATED', 'MODIFIED', 'REVOKED') NOT NULL,
    user_id INT NULL,
    ip_address VARCHAR(45) NULL,
    CONSTRAINT chk_user_or_ip_not_both_null CHECK (user_id IS NOT NULL OR ip_address IS NOT NULL),
    reason_id INT NULL,
    reason_type_id INT NOT NULL,
    reason_text TEXT NOT NULL,
    reason_duration BIGINT NULL,
    reason_auto_flag_ip BOOLEAN NOT NULL,
    reason_auto_punish BOOLEAN NOT NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (reason_id) REFERENCES punishment_reasons(id) ON DELETE SET NULL ON UPDATE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_audit_user ON punishment_logs (user_id);
CREATE INDEX IF NOT EXISTS idx_audit_ip ON punishment_logs (ip_address);
CREATE INDEX IF NOT EXISTS idx_audit_reason ON punishment_logs (reason_id);

CREATE TABLE IF NOT EXISTS punishment_current_ip (
    ip_address VARCHAR(45) NOT NULL,
    type_id INT NOT NULL,
    log_id VARCHAR(36) NOT NULL UNIQUE,
    PRIMARY KEY (ip_address, type_id),
    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (log_id) REFERENCES punishment_logs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS punishment_current_user (
    user_id INT NOT NULL,
    type_id INT NOT NULL,
    log_id VARCHAR(36) NOT NULL UNIQUE,
    PRIMARY KEY (user_id, type_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (log_id) REFERENCES punishment_logs(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

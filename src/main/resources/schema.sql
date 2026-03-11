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
    mojang_id UUID NOT NULL,
    username VARCHAR(16) NOT NULL,
    first_login DATETIME NULL,
    system_user BOOLEAN NOT NULL DEFAULT FALSE,
    debug_user BOOLEAN NOT NULL DEFAULT FALSE,
    debug_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    language_id INT NOT NULL DEFAULT 1,
    FOREIGN KEY (language_id) REFERENCES languages(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_users_mojang_id ON users (mojang_id);
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);

CREATE TABLE IF NOT EXISTS user_stats (
    id INT PRIMARY KEY,

    play_time INT NOT NULL DEFAULT 0,
    daily_streak INT NOT NULL DEFAULT 0,
    daily_streak_last_day DATE NULL,
    last_seen_at DATETIME NULL,

    FOREIGN KEY (id) REFERENCES users(id),

    INDEX idx_user_stats_last_seen_at (last_seen_at),
    INDEX idx_user_stats_streak_day (daily_streak_last_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_login (
    id UUID NOT NULL PRIMARY KEY,
    user_id INT NOT NULL,
    login_time DATETIME NOT NULL,
    logout_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    ip_address INET6 NOT NULL,
    client_brand VARCHAR(50) NULL,
    protocol_version INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX IF NOT EXISTS idx_user_id ON user_login (user_id);
CREATE INDEX IF NOT EXISTS idx_ip_address ON user_login (ip_address);

CREATE TABLE IF NOT EXISTS user_session (
    id UUID PRIMARY KEY,
    user_id INT NOT NULL UNIQUE,
    login_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    ip_address INET6 NOT NULL,
    client_brand VARCHAR(50) NULL,
    protocol_version INT NOT NULL,
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

CREATE TABLE IF NOT EXISTS permissions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    group_id INT NULL,
    permission VARCHAR(200) NOT NULL,
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT chk_permissions_xor CHECK (
        (user_id IS NOT NULL AND group_id IS NULL)
        OR
        (group_id IS NOT NULL AND user_id IS NULL)
    ),
    UNIQUE KEY uk_user_permission (user_id, permission),
    UNIQUE KEY uk_group_permission (group_id, permission),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_permissions_exp (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS parents (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    group_id INT NULL,
    parent_id INT NOT NULL,
    expires_at DATETIME NULL,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),

    CONSTRAINT chk_parents_xor CHECK (
        (user_id IS NOT NULL AND group_id IS NULL)
        OR
        (group_id IS NOT NULL AND user_id IS NULL)
    ),
    UNIQUE KEY uk_user_parent (user_id, parent_id),
    UNIQUE KEY uk_group_parent (group_id, parent_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_parents_exp (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),

    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_reason_type (type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS punishment_audit (
    id UUID PRIMARY KEY,
    action ENUM('CREATED', 'MODIFIED', 'REVOKED') NOT NULL,

    mojang_id UUID NULL,
    ip_address INET6 NULL,

    reason_id INT NULL,
    reason_type_id INT NULL,
    reason_text TEXT NOT NULL,
    reason_duration BIGINT NULL,
    reason_auto_flag_ip BOOLEAN NOT NULL,

    created_by INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),

    CONSTRAINT chk_user_or_ip_not_both_null CHECK (
        mojang_id IS NOT NULL OR ip_address IS NOT NULL
    ),
    FOREIGN KEY (reason_id) REFERENCES punishment_reasons(id) ON DELETE SET NULL,
    FOREIGN KEY (reason_type_id) REFERENCES punishment_types(id) ON DELETE SET NULL,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_user (mojang_id),
    INDEX idx_audit_ip (ip_address),
    INDEX idx_audit_reason (reason_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS punishment_ip_address (
    ip_address INET6 NOT NULL,
    type_id INT NOT NULL,
    audit_id UUID NOT NULL UNIQUE,

    expires_at DATETIME NULL,

    PRIMARY KEY (ip_address, type_id),
    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (audit_id) REFERENCES punishment_audit(id) ON DELETE CASCADE,
    INDEX idx_punish_ip_exp (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS punishment_user (
    mojang_id UUID NOT NULL,
    type_id INT NOT NULL,
    audit_id UUID NOT NULL UNIQUE,

    expires_at DATETIME NULL,

    PRIMARY KEY (mojang_id, type_id),
    FOREIGN KEY (type_id) REFERENCES punishment_types(id),
    FOREIGN KEY (audit_id) REFERENCES punishment_audit(id) ON DELETE CASCADE,
    INDEX idx_punish_user_exp (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Clan data
CREATE TABLE IF NOT EXISTS clans (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    tag VARCHAR(25) NULL UNIQUE,
    sign VARCHAR(25) NULL UNIQUE,
    description TEXT NULL,
    owner_id INT NOT NULL UNIQUE,
    created_by INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    FOREIGN KEY (owner_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clan_groups (
    group_id UUID NOT NULL,
    clan_id UUID NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    PRIMARY KEY (group_id),
    UNIQUE KEY (clan_id, group_id),
    UNIQUE KEY (clan_id, group_name),
    KEY (clan_id),
    FOREIGN KEY (clan_id) REFERENCES clans(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clan_member (
    clan_id UUID NOT NULL,
    user_id INT NOT NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    group_id UUID NOT NULL,
    PRIMARY KEY (clan_id, user_id),
    KEY (user_id),
    KEY (clan_id, group_id),
    FOREIGN KEY (clan_id) REFERENCES clans(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (clan_id, group_id) REFERENCES clan_groups(clan_id, group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clan_parent (
    clan_id UUID NOT NULL,
    group_id UUID NOT NULL,
    parent_id UUID NOT NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    PRIMARY KEY (clan_id, group_id, parent_id),
    KEY (expires_at),
    KEY (clan_id, group_id),
    FOREIGN KEY (clan_id) REFERENCES clans(id),
    FOREIGN KEY (clan_id, group_id) REFERENCES clan_groups(clan_id, group_id),
    FOREIGN KEY (clan_id, parent_id) REFERENCES clan_groups(clan_id, group_id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS clan_permission (
    clan_id UUID NOT NULL,
    group_id UUID NOT NULL,
    permission VARCHAR(200) NOT NULL,
    expires_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    PRIMARY KEY (clan_id, group_id, permission),
    KEY (expires_at),
    KEY (clan_id, group_id),
    FOREIGN KEY (clan_id) REFERENCES clans(id),
    FOREIGN KEY (clan_id, group_id) REFERENCES clan_groups(clan_id, group_id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS prefix_colors (
    id VARCHAR(100) NOT NULL PRIMARY KEY,
    color TEXT NOT NULL,
    animated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_prefix_colors (
    user_id INT NOT NULL,
    color_id VARCHAR(100) NOT NULL,
    PRIMARY KEY (user_id, color_id),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (color_id) REFERENCES prefix_colors(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS inventory_type (
    id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    inventory_name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_inventory (
    user_id INT NOT NULL,
    inventory_id INT NOT NULL,
    PRIMARY KEY (user_id, inventory_id),
    inventory_value LONGTEXT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (inventory_id) REFERENCES inventory_type(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenances (
    id INT NOT NULL AUTO_INCREMENT,
    title VARCHAR(64) NULL,
    reason VARCHAR(255) NULL,
    status ENUM('PLANNED','ACTIVE','ENDED','CANCELED') NOT NULL DEFAULT 'PLANNED',
    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,

    PRIMARY KEY (id),
    CONSTRAINT chk_maintenance_window_range CHECK (end_at > start_at),

    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_mw_status_time (status, start_at, end_at),
    INDEX idx_mw_time (start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS maintenance_whitelist (
    id INT NOT NULL AUTO_INCREMENT,
    maintenance_id INT NOT NULL,
    user_id INT NOT NULL,

    start_at DATETIME NULL,
    end_at DATETIME NULL,

    note VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_mwh_maintenance
        FOREIGN KEY (maintenance_id) REFERENCES maintenances(id)
            ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_mwh_range CHECK (
        (start_at IS NULL AND end_at IS NULL) OR
        (start_at IS NOT NULL AND (end_at IS NULL OR end_at > start_at))
    ),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_mwh_maintenance_user (maintenance_id, user_id),
    INDEX idx_mwh_user_time (user_id, start_at, end_at),
    INDEX idx_mwh_maintenance_time (maintenance_id, start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_excuses (
    id INT NOT NULL AUTO_INCREMENT,
    user_id INT NOT NULL,

    start_at DATETIME NOT NULL,
    end_at DATETIME NOT NULL,

    reason VARCHAR(255) NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(),
    created_by INT NOT NULL,
    changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(),
    changed_by INT NULL,

    PRIMARY KEY (id),
    CONSTRAINT chk_uew_range CHECK (end_at > start_at),

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (changed_by) REFERENCES users(id),
    INDEX idx_uew_user_time (user_id, start_at, end_at),
    INDEX idx_uew_time (start_at, end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

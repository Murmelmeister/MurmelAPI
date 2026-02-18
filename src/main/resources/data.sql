-- Default languages
INSERT INTO languages (id, code) VALUES
    (1, 'en-US'),
    (2, 'de-DE')
ON DUPLICATE KEY UPDATE code = VALUES(code);

-- Console user (system)
INSERT IGNORE INTO users (id, mojang_id, username, first_login, system_user, debug_user, debug_enabled, language_id) VALUES
    (-1, 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'Console', NULL, TRUE, TRUE, TRUE, 1);

-- Default group
INSERT IGNORE INTO groups (id, group_name, priority, is_default, created_by) VALUES
    (1, 'default', 1, TRUE, -1);

-- Group color types
INSERT IGNORE INTO group_color_type (id, name) VALUES
    (1, 'chat_prefix'),
    (2, 'chat_suffix'),
    (3, 'chat_color'),
    (4, 'chat_message'),
    (5, 'tab_prefix'),
    (6, 'tab_suffix'),
    (7, 'tab_color'),
    (8, 'team_prefix'),
    (9, 'team_suffix'),
    (10, 'team_color');

-- Punishment types
INSERT IGNORE INTO punishment_types (id, name, ip_type) VALUES
    (1, 'Ban', FALSE),
    (2, 'Mute', FALSE),
    (3, 'Kick', FALSE),
    (4, 'Clan', FALSE),
    (5, 'IP-Ban', TRUE),
    (6, 'IP-Mute', TRUE),
    (7, 'IP-Kick', TRUE),
    (8, 'IP-Clan', TRUE);

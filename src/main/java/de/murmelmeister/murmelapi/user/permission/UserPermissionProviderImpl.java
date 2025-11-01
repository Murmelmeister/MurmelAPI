package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

/**
 * UserPermissionProvider class to manage user permissions in the database.
 * This class implements the UserPermission interface and provides methods to interact with user permission data.
 */
public final class UserPermissionProviderImpl implements UserPermissionProvider {
    private static final String TABLE_NAME = "user_permission";

    private final Database database;
    private final UserPermissionCache cache;
    private final RefreshType all = RefreshType.USER_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_USER_PERMISSION;

    public UserPermissionProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new UserPermissionCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "user_id INT, permission VARCHAR(200), " +
                "PRIMARY KEY (user_id, permission), " +
                "expires_at DATETIME NULL, " +
                "created_by INT NOT NULL, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                "changed_by INT NULL, " +
                "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (created_by) REFERENCES users(id), " +
                "FOREIGN KEY (changed_by) REFERENCES users(id)"
        );
        database.update("CREATE INDEX IF NOT EXISTS idx_user_perm_userId_exp ON " + TABLE_NAME + " (user_id, expires_at)");
        // TODO: Get all user permissions + parent permissions of the user from db -> cache
    }

    @Override
    public void closeCache() {
        cache.close();
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public UserPermission getPermission(int userId, String permission) {
        return cache.get(userId, permission);
    }

    @Override
    public List<UserPermission> getPermissions(int userId) {
        return cache.getPermissions(userId);
    }

    @Override
    public UserPermission add(int userId, String permission, long duration, int createdBy) {
        if (userId < 1 || permission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        permission = permission.strip();
        if (permission.isEmpty()) return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (user_id, permission, expires_at, created_by) VALUES (?, ?, ?, ?)";
        String finalPermission = permission;
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, finalPermission);
            stmt.setTimestamp(3, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE user_id = ? AND permission = ?";
        LocalDateTime createdAt = database.query(selectSql, null, resultSet ->
                        resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, finalPermission);
                });
        if (createdAt == null) return null;

        UserPermission userPermission = new UserPermission(userId, permission, expiresAt, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, new UserPermissionCache.PermissionKey(userId, permission));
        cache.put(userPermission);
        return userPermission;
    }

    @Override
    public int remove(int userId, String permission) {
        if (userId < 1 || permission == null) return 0;

        permission = permission.strip();
        if (permission.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND permission = ?";
        String finalPermission = permission;
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, finalPermission);
        });
        if (row < 1) return 0;

        cache.remove(userId, permission);
        RefreshUtil.fireSingle(single, new UserPermissionCache.PermissionKey(userId, permission));
        return row;
    }

    @Override
    public int clear(int userId) {
        if (userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ?";
        int rows = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (rows < 1) return 0;

        cache.remove(userId);
        RefreshUtil.fireSingle(single, userId);
        return rows;
    }

    @Override
    public UserPermission update(int userId, String permission, long duration, int changedBy) {
        if (userId < 1 || permission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        permission = permission.strip();
        if (permission.isEmpty()) return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        UserPermission existing = cache.get(userId, permission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE user_id = ? AND permission = ?";
        String finalPermission = permission;
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, userId);
            stmt.setString(4, finalPermission);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE user_id = ? AND permission = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, finalPermission);
                });
        if (changedAt == null) return null;

        UserPermission userPermission = existing.withUpdateMeta(expiresAt, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new UserPermissionCache.PermissionKey(userId, permission));
        cache.put(userPermission);
        return userPermission;
    }

    @Override
    public int loadExpired() {
        // Get all expired permissions from the cache
        Set<UserPermissionCache.PermissionKey> expiredPermissions = cache.getCachedPermissions().stream()
                .filter(UserPermission::isExpired)
                .map(permission -> new UserPermissionCache.PermissionKey(permission.userId(), permission.permission()))
                .collect(Collectors.toSet());
        if (expiredPermissions.isEmpty()) return 0;

        // Delete expired permissions from the database
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()";
        int removed = database.update(sql);

        // Refresh the cache
        expiredPermissions.forEach(key -> RefreshUtil.fireSingle(single, key));
        return removed;
    }
}

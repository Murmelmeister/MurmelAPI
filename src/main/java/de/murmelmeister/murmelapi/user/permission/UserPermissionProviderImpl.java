package de.murmelmeister.murmelapi.user.permission;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * UserPermissionProvider class to manage user permissions in the database.
 * This class implements the UserPermission interface and provides methods to interact with user permission data.
 */
public final class UserPermissionProviderImpl implements UserPermissionProvider {
    private static final String TABLE_NAME = "user_permission";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final UserPermissionCache cache;
    private final RefreshType all = RefreshType.USER_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_USER_PERMISSION;

    public UserPermissionProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new UserPermissionCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable UserPermission getPermission(int userId, @NotNull String permission) {
        return cache.get(userId, permission);
    }

    @Override
    public @NotNull @Unmodifiable List<UserPermission> getPermissions(int userId) {
        return cache.getPermissions(userId);
    }

    @Override
    public @Nullable UserPermission add(int userId, @NotNull String permission, long duration, int createdBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (userId < 1 || normalizedPermission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (user_id, permission, expires_at, created_by)
                VALUES (?, ?, ?, ?)
                RETURNING user_id, permission, expires_at, created_by, created_at, changed_by, changed_at
                """.formatted(TABLE_NAME);
        UserPermission userPermission = database.query(sql, null, ResultSetUtil.userPermission(), stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, normalizedPermission);
            stmt.setObject(3, expiresAt, Types.TIMESTAMP);
            stmt.setInt(4, createdBy);
        });

        if (userPermission == null) return null;
        refreshProvider.fireSingle(single, new UserPermissionCache.PermissionKey(userId, normalizedPermission));
        return userPermission;
    }

    @Override
    public int remove(int userId, @NotNull String permission) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (userId < 1 || normalizedPermission == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setString(2, normalizedPermission);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new UserPermissionCache.PermissionKey(userId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(int userId) {
        if (userId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE user_id = ?".formatted(TABLE_NAME);
        int rows = database.update(sql,
                stmt -> stmt.setInt(1, userId));
        if (rows < 1) return 0;

        refreshProvider.fireSingle(single, new UserPermissionCache.PermissionKey(userId, null));
        return rows;
    }

    @Override
    public @Nullable UserPermission update(int userId, @NotNull String permission, long duration, int changedBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (userId < 1 || normalizedPermission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        UserPermission existing = cache.get(userId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET expires_at = ?, changed_by = ? WHERE user_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setInt(3, userId);
            stmt.setString(4, normalizedPermission);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE user_id = ? AND permission = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, userId);
                    stmt.setString(2, normalizedPermission);
                });
        if (changedAt == null) return null;

        UserPermission userPermission = UserPermission.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new UserPermissionCache.PermissionKey(userId, normalizedPermission));
        return userPermission;
    }

    @Override
    public int loadExpired() {
        // Get all expired permissions from the cache
        Set<UserPermissionCache.PermissionKey> expiredPermissions = cache.getAll().stream()
                .filter(UserPermission::isExpired)
                .map(permission -> new UserPermissionCache.PermissionKey(permission.userId(), permission.permission()))
                .collect(Collectors.toSet());
        if (expiredPermissions.isEmpty()) return 0;

        // Delete expired permissions from the database
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);
        int removed = database.update(sql);

        // Refresh the cache
        expiredPermissions.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}

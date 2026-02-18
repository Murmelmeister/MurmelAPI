package de.murmelmeister.murmelapi.group.permission;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * GroupPermissionProvider is a class that provides methods to manage group permissions in the database.
 * It implements the GroupPermission interface and uses the Database class to interact with the database.
 */
public final class GroupPermissionProviderImpl implements GroupPermissionProvider {
    private static final String TABLE_NAME = "group_permission";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final GroupPermissionCache cache;
    private final RefreshType all = RefreshType.GROUP_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_PERMISSION;

    public GroupPermissionProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new GroupPermissionCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable GroupPermission getPermission(int groupId, @NotNull String permission) {
        return cache.get(groupId, permission);
    }

    @Override
    public @Nullable List<GroupPermission> getPermissions(int groupId) {
        return cache.getPermissions(groupId);
    }

    @Override
    public @Nullable GroupPermission add(int groupId, @NotNull String permission, long duration, int createdBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (group_id, permission, expires_at, created_by)
                VALUES (?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setString(2, normalizedPermission);
            stmt.setTimestamp(3, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE group_id = ? AND permission = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setString(2, normalizedPermission);
                });
        if (createdAt == null) return null;

        GroupPermission groupPermission = new GroupPermission(groupId, normalizedPermission, expiresAt, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
        return groupPermission;
    }

    @Override
    public int remove(int groupId, @NotNull String permission) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE group_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setString(2, normalizedPermission);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE group_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, null));
        return row;
    }

    @Override
    public @Nullable GroupPermission update(int groupId, @NotNull String permission, long duration, int changedBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        GroupPermission existing = cache.get(groupId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET expires_at = ?, changed_by = ? WHERE group_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt).toString());
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setString(4, normalizedPermission);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE group_id = ? AND permission = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setString(2, normalizedPermission);
                });
        if (changedAt == null) return null;

        GroupPermission groupPermission = GroupPermission.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
        return groupPermission;
    }

    @Override
    public int loadExpired() {
        // Get all expired permissions from the cache
        List<GroupPermissionCache.PermissionKey> expiredPermissions = cache.getCachedPermissions().stream()
                .filter(GroupPermission::isExpired)
                .map(permission -> new GroupPermissionCache.PermissionKey(permission.groupId(), permission.permission()))
                .toList();
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

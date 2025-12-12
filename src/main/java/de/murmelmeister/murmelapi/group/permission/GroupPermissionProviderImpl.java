package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

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
    private final GroupPermissionCache cache;
    private final RefreshType all = RefreshType.GROUP_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_GROUP_PERMISSION;

    public GroupPermissionProviderImpl(Database database, Long fetchLimit, long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.cache = new GroupPermissionCache(database, TABLE_NAME, fetchLimit, cacheCapcity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public GroupPermission getPermission(int groupId, String permission) {
        return cache.get(groupId, permission);
    }

    @Override
    public List<GroupPermission> getPermissions(int groupId) {
        return cache.getPermissions(groupId);
    }

    @Override
    public GroupPermission add(int groupId, String permission, long duration, int createdBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (group_id, permission, expires_at, created_by) VALUES (?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setString(2, normalizedPermission);
            stmt.setTimestamp(3, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(4, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE group_id = ? AND permission = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setString(2, normalizedPermission);
                });
        if (createdAt == null) return null;

        GroupPermission groupPermission = new GroupPermission(groupId, normalizedPermission, expiresAt, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
        return groupPermission;
    }

    @Override
    public int remove(int groupId, String permission) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ? AND permission = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, groupId);
            stmt.setString(2, normalizedPermission);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(int groupId) {
        if (groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE group_id = ?";
        int row = database.update(sql,
                stmt -> stmt.setInt(1, groupId));
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public GroupPermission update(int groupId, String permission, long duration, int changedBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (groupId < 1 || normalizedPermission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        GroupPermission existing = cache.get(groupId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing; // No changes, return existing

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE group_id = ? AND permission = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt).toString());
            stmt.setInt(2, changedBy);
            stmt.setInt(3, groupId);
            stmt.setString(4, normalizedPermission);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE group_id = ? AND permission = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setInt(1, groupId);
                    stmt.setString(2, normalizedPermission);
                });
        if (changedAt == null) return null;

        GroupPermission groupPermission = existing.withUpdateMeta(expiresAt, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new GroupPermissionCache.PermissionKey(groupId, normalizedPermission));
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
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()";
        int removed = database.update(sql);

        // Refresh the cache
        expiredPermissions.forEach(key -> RefreshUtil.fireSingle(single, key));
        return removed;
    }
}

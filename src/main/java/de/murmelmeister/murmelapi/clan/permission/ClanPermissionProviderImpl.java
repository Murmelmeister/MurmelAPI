package de.murmelmeister.murmelapi.clan.permission;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanPermissionProviderImpl implements ClanPermissionProvider {
    private static final String TABLE_NAME = "clan_permission";

    private final Database database;
    private final ClanPermissionCache cache;
    private final RefreshType all = RefreshType.CLAN_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_PERMISSION;

    public ClanPermissionProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanPermissionCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public ClanPermission findPermission(UUID clanId, UUID groupId, String permission) {
        return cache.get(clanId, groupId, permission);
    }

    @Override
    public List<ClanPermission> findPermissions(UUID clanId, UUID groupId) {
        return cache.getByPermissions(clanId, groupId);
    }

    @Override
    public ClanPermission add(UUID clanId, UUID groupId, String permission, long duration, int createdBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (clanId == null || groupId == null || normalizedPermission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (clan_id, group_id, permission, expires_at, created_by) VALUES (?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, normalizedPermission);
            stmt.setTimestamp(4, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(5, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND permission = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setString(3, normalizedPermission);
                });
        if (createdAt == null) return null;

        ClanPermission clanPermission = new ClanPermission(clanId, groupId, normalizedPermission, expiresAt, createdAt, createdBy, null, null);
        RefreshUtil.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return clanPermission;
    }

    @Override
    public int remove(UUID clanId, UUID groupId, String permission) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (clanId == null || groupId == null || normalizedPermission == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND permission = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, normalizedPermission);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(UUID clanId, UUID groupId) {
        if (clanId == null || groupId == null) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new ClanPermissionCache.GroupKey(clanId, groupId));
        return row;
    }

    @Override
    public ClanPermission update(UUID clanId, UUID groupId, String permission, long duration, int changedBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (clanId == null || groupId == null || normalizedPermission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ClanPermission existing = cache.get(clanId, groupId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE clan_id = ? AND group_id = ? AND permission = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt).toString());
            stmt.setInt(2, changedBy);
            stmt.setString(3, clanId.toString());
            stmt.setString(4, groupId.toString());
            stmt.setString(5, normalizedPermission);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND permission = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setString(3, normalizedPermission);
                });
        if (changedAt == null) return null;

        ClanPermission clanPermission = existing.withUpdateMeta(expiresAt, changedAt, changedBy);
        RefreshUtil.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return clanPermission;
    }

    @Override
    public int loadExpired() {
        List<ClanPermissionCache.PermissionKey> expiredPermissions = cache.getAll().stream()
                .filter(ClanPermission::isExpired)
                .map(permission -> new ClanPermissionCache.PermissionKey(permission.clanId(), permission.groupId(), permission.permission()))
                .toList();

        if (expiredPermissions.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()";
        int removed = database.update(sql);

        expiredPermissions.forEach(key -> RefreshUtil.fireSingle(single, key));
        return removed;
    }
}

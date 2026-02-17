package de.murmelmeister.murmelapi.clan.permission;

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
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanPermissionProviderImpl implements ClanPermissionProvider {
    private static final String TABLE_NAME = "clan_permission";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanPermissionCache cache;
    private final RefreshType all = RefreshType.CLAN_PERMISSIONS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_PERMISSION;

    public ClanPermissionProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanPermissionCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ClanPermission findPermission(@Nullable UUID clanId, @Nullable UUID groupId, @Nullable String permission) {
        return cache.get(clanId, groupId, permission);
    }

    @Override
    public @Nullable List<ClanPermission> findPermissions(@Nullable UUID clanId, @Nullable UUID groupId) {
        return cache.getByPermissions(clanId, groupId);
    }

    @Override
    public @Nullable ClanPermission add(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission, long duration, int createdBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (clan_id, group_id, permission, expires_at, created_by)
                VALUES (?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, normalizedPermission);
            stmt.setTimestamp(4, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(5, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE clan_id = ? AND group_id = ? AND permission = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setString(3, normalizedPermission);
                });
        if (createdAt == null) return null;

        ClanPermission clanPermission = new ClanPermission(clanId, groupId, normalizedPermission, expiresAt, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return clanPermission;
    }

    @Override
    public int remove(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE clan_id = ? AND group_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setString(3, normalizedPermission);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return row;
    }

    @Override
    public int clear(@NotNull UUID clanId, @NotNull UUID groupId) {
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, null));
        return row;
    }

    @Override
    public @Nullable ClanPermission update(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String permission, long duration, int changedBy) {
        String normalizedPermission = StringUtil.normalize(permission);
        if (normalizedPermission == null || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ClanPermission existing = cache.get(clanId, groupId, normalizedPermission);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        @Language("MariaDB")
        String updateSql = "UPDATE %s SET expires_at = ?, changed_by = ? WHERE clan_id = ? AND group_id = ? AND permission = ?".formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setString(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt).toString());
            stmt.setInt(2, changedBy);
            stmt.setString(3, clanId.toString());
            stmt.setString(4, groupId.toString());
            stmt.setString(5, normalizedPermission);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE clan_id = ? AND group_id = ? AND permission = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setString(3, normalizedPermission);
                });
        if (changedAt == null) return null;

        ClanPermission clanPermission = ClanPermission.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new ClanPermissionCache.PermissionKey(clanId, groupId, normalizedPermission));
        return clanPermission;
    }

    @Override
    public int loadExpired() {
        List<ClanPermissionCache.PermissionKey> expiredPermissions = cache.getAll().stream()
                .filter(ClanPermission::isExpired)
                .map(permission -> new ClanPermissionCache.PermissionKey(permission.clanId(), permission.groupId(), permission.permission()))
                .toList();

        if (expiredPermissions.isEmpty()) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);
        int removed = database.update(sql);

        expiredPermissions.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}

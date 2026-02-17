package de.murmelmeister.murmelapi.clan.parent;

import de.murmelmeister.library.database.Database;
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

public final class ClanParentProviderImpl implements ClanParentProvider {
    private static final String TABLE_NAME = "clan_parent";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanParentCache cache;
    private final RefreshType all = RefreshType.CLAN_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_PARENT;

    public ClanParentProviderImpl(Database database, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanParentCache(database, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ClanParent findParent(@Nullable UUID clanId, @Nullable UUID groupId, int parentId) {
        return cache.get(clanId, groupId, parentId);
    }

    @Override
    public @Nullable List<ClanParent> findParents(@Nullable UUID clanId, @Nullable UUID groupId) {
        return cache.getByGroup(clanId, groupId);
    }

    @Override
    public @Nullable ClanParent add(@NotNull UUID clanId, @NotNull UUID groupId, int parentId, long duration, int createdBy) {
        if (parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (clan_id, group_id, parent_id, expires_at, created_by)
                VALUES (?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setInt(3, parentId);
            stmt.setTimestamp(4, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(5, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE clan_id = ? AND group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setInt(3, parentId);
                });
        if (createdAt == null) return null;

        ClanParent clanParent = new ClanParent(clanId, groupId, parentId, expiresAt, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
        return clanParent;
    }

    @Override
    public int remove(@NotNull UUID clanId, @NotNull UUID groupId, int parentId) {
        if (parentId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE clan_id = ? AND group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
            stmt.setInt(3, parentId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
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

        refreshProvider.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, null));
        return row;
    }

    @Override
    public @Nullable ClanParent update(@NotNull UUID clanId, @NotNull UUID groupId, int parentId, long duration, int changedBy) {
        if (parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ClanParent existing = cache.get(clanId, groupId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        @Language("MariaDB")
        String updateSql = """
                UPDATE %s SET
                    expires_at = ?,
                    changed_by = ?
                WHERE clan_id = ? AND group_id = ? AND parent_id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setString(3, clanId.toString());
            stmt.setString(4, groupId.toString());
            stmt.setInt(5, parentId);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE clan_id = ? AND group_id = ? AND parent_id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                    stmt.setInt(3, parentId);
                });
        if (changedAt == null) return null;

        ClanParent clanParent = ClanParent.builder(existing)
                .expiresAt(expiresAt)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
        return clanParent;
    }

    @Override
    public int loadExpired() {
        List<ClanParentCache.ParentKey> expiredParents = cache.getAll().stream()
                .filter(ClanParent::isExpired)
                .map(parent -> new ClanParentCache.ParentKey(parent.clanId(), parent.groupId(), parent.parentId()))
                .toList();

        if (expiredParents.isEmpty()) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()".formatted(TABLE_NAME);
        int removed = database.update(sql);

        expiredParents.forEach(key -> refreshProvider.fireSingle(single, key));
        return removed;
    }
}

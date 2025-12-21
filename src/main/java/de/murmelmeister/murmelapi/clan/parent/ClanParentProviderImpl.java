package de.murmelmeister.murmelapi.clan.parent;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

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
    private final ClanParentCache cache;
    private final RefreshType all = RefreshType.CLAN_PARENTS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_PARENT;

    public ClanParentProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanParentCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public ClanParent findParent(UUID clanId, int groupId, int parentId) {
        return cache.get(clanId, groupId, parentId);
    }

    @Override
    public List<ClanParent> findParents(UUID clanId, int groupId) {
        return cache.getByGroup(clanId, groupId);
    }

    @Override
    public ClanParent add(UUID clanId, int groupId, int parentId, long duration, int createdBy) {
        if (clanId == null || groupId < 1 || parentId < 1 || duration < -1 || createdBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        String insertSql = "INSERT INTO " + TABLE_NAME + " (clan_id, group_id, parent_id, expires_at, created_by) VALUES (?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, groupId);
            stmt.setInt(3, parentId);
            stmt.setTimestamp(4, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(5, createdBy);
        });
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND parent_id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, groupId);
                    stmt.setInt(3, parentId);
                });
        if (createdAt == null) return null;

        ClanParent clanParent = new ClanParent(clanId, groupId, parentId, expiresAt, createdAt, createdBy, null, null);
        RefreshUtil.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
        return clanParent;
    }

    @Override
    public int remove(UUID clanId, int groupId, int parentId) {
        if (clanId == null || groupId < 1 || parentId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND parent_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, groupId);
            stmt.setInt(3, parentId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
        return row;
    }

    @Override
    public int clear(UUID clanId, int groupId) {
        if (clanId == null || groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, groupId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, groupId);
        return row;
    }

    @Override
    public ClanParent update(UUID clanId, int groupId, int parentId, long duration, int changedBy) {
        if (clanId == null || groupId < 1 || parentId < 1 || duration < -1 || changedBy < CONSOLE_USER_ID)
            return null;

        LocalDateTime expiresAt = duration == -1 ? null : LocalDateTime.now().plusSeconds(duration);
        ClanParent existing = cache.get(clanId, groupId, parentId);
        if (existing == null) return null;

        if (Objects.equals(expiresAt, existing.expiresAt()))
            return existing;

        String updateSql = "UPDATE " + TABLE_NAME + " SET expires_at = ?, changed_by = ? WHERE clan_id = ? AND group_id = ? AND parent_id = ?";
        int row = database.update(updateSql, stmt -> {
            stmt.setTimestamp(1, expiresAt == null ? null : Timestamp.valueOf(expiresAt));
            stmt.setInt(2, changedBy);
            stmt.setString(3, clanId.toString());
            stmt.setInt(4, groupId);
            stmt.setInt(5, parentId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ? AND parent_id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, groupId);
                    stmt.setInt(3, parentId);
                });
        if (changedAt == null) return null;

        ClanParent clanParent = existing.withUpdateMeta(expiresAt, changedAt, changedBy);
        RefreshUtil.fireSingle(single, new ClanParentCache.ParentKey(clanId, groupId, parentId));
        return clanParent;
    }

    @Override
    public int loadExpired() {
        List<ClanParentCache.ParentKey> expiredParents = cache.getAll().stream()
                .filter(ClanParent::isExpired)
                .map(parent -> new ClanParentCache.ParentKey(parent.clanId(), parent.groupId(), parent.parentId()))
                .toList();

        if (expiredParents.isEmpty()) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()";
        int removed = database.update(sql);

        expiredParents.forEach(key -> RefreshUtil.fireSingle(single, key));
        return removed;
    }
}

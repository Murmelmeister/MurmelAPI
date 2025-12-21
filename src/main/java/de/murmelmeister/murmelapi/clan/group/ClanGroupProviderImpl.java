package de.murmelmeister.murmelapi.clan.group;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanGroupProviderImpl implements ClanGroupProvider {
    private static final String TABLE_NAME = "clan_groups";

    private final Database database;
    private final ClanGroupCache cache;
    private final RefreshType all = RefreshType.CLAN_GROUPS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_GROUP;

    public ClanGroupProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanGroupCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public ClanGroup findById(UUID clanId, int groupId) {
        return cache.getByKey(clanId, groupId);
    }

    @Override
    public List<ClanGroup> findByClanId(UUID clanId) {
        return cache.getByClanId(clanId);
    }

    @Override
    public List<ClanGroup> findAll() {
        return cache.getAll();
    }

    @Override
    public ClanGroup create(UUID clanId, String groupName, int priority, boolean defaultGroup, int createdBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (clanId == null || normalizedGroupName == null || priority < 0 || createdBy < CONSOLE_USER_ID)
            return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (clan_id, group_name, priority, created_by) VALUES (?, ?, ?, ?)";
        int id = (int) database.updateAndGetGeneratedKeys(insertSql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, normalizedGroupName);
            stmt.setInt(3, priority);
            stmt.setInt(4, createdBy);
        });
        if (id < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_name = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, normalizedGroupName);
                });
        if (createdAt == null) return null;

        ClanGroup clanGroup = new ClanGroup(clanId, id, normalizedGroupName, priority, defaultGroup, createdAt, createdBy, null, null);
        RefreshUtil.fireSingle(single, new ClanGroupCache.GroupKey(clanId, id));
        return clanGroup;
    }

    @Override
    public int delete(UUID clanId, int groupId) {
        if (clanId == null || groupId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, groupId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return row;
    }

    @Override
    public ClanGroup update(UUID clanId, int groupId, String groupName, int priority, boolean defaultGroup, int changedBy) {
        if (clanId == null || groupId < 1 || groupName == null || priority < 0 || changedBy < CONSOLE_USER_ID)
            return null;

        ClanGroup existing = cache.getByKey(clanId, groupId);
        if (existing == null) return null;

        if (Objects.equals(groupName, existing.groupName())
                && priority == existing.priority()
                && defaultGroup == existing.defaultGroup())
            return existing;

        String normalizedGroupName = StringUtil.normalize(groupName);
        String sql = "UPDATE " + TABLE_NAME + " SET group_name = ?, priority = ?, default_group = ?, changed_by = ? WHERE clan_id = ? AND group_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setBoolean(3, defaultGroup);
            stmt.setInt(4, changedBy);
            stmt.setString(5, clanId.toString());
            stmt.setInt(6, groupId);
        });
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND group_id = ?";
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, groupId);
                });
        if (changedAt == null) return null;

        ClanGroup updated = existing.withUpdateMeta(normalizedGroupName, priority, defaultGroup, changedBy, changedAt);
        RefreshUtil.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return updated;
    }
}

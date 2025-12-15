package de.murmelmeister.murmelapi.clan.member;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class ClanMemberProviderImpl implements ClanMemberProvider {
    private static final String TABLE_NAME = "clan_member";

    private final Database database;
    private final ClanMemberCache cache;
    private final RefreshType all = RefreshType.CLAN_MEMBERS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_MEMBER;

    public ClanMemberProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new ClanMemberCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public ClanMember findMember(UUID clanId, int userId) {
        return cache.get(clanId, userId);
    }

    @Override
    public List<ClanMember> findClan(UUID clanId) {
        return findAll().stream()
                .filter(member -> member.clanId().equals(clanId))
                .toList();
    }

    @Override
    public List<ClanMember> findAll() {
        return cache.getCachedMembers();
    }

    @Override
    public ClanMember create(UUID clanId, int userId) {
        if (clanId == null || userId < 1)
            return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (clan_id, user_id) VALUES (?, ?)";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, userId);
        });
        if (row < 1) return null;

        String joinedSql = "SELECT joined_at FROM " + TABLE_NAME + " WHERE clan_id = ? AND user_id = ?";
        LocalDateTime joinedAt = database.query(joinedSql, null,
                resultSet -> resultSet.getTimestamp("joined_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, userId);
                });
        if (joinedAt == null) return null;

        ClanMember member = new ClanMember(clanId, userId, joinedAt);
        RefreshUtil.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return member;
    }

    @Override
    public int delete(UUID clanId, int userId) {
        if (clanId == null || userId < 1) return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE clan_id = ? AND user_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, userId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return row;
    }
}

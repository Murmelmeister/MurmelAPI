package de.murmelmeister.murmelapi.clan.member;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ClanMemberProviderImpl implements ClanMemberProvider {
    private static final String TABLE_NAME = "clan_member";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanMemberCache cache;
    private final RefreshType all = RefreshType.CLAN_MEMBERS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_MEMBER;

    public ClanMemberProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanMemberCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ClanMember findMember(@NotNull UUID clanId, int userId) {
        return cache.get(clanId, userId);
    }

    @Override
    public @Nullable List<ClanMember> findClan(@Nullable UUID clanId) {
        return cache.getByClanId(clanId);
    }

    @Override
    public @NotNull List<ClanMember> findAll() {
        return cache.getCachedMembers();
    }

    @Override
    public @Nullable ClanMember create(@NotNull UUID clanId, int userId, @NotNull UUID groupId) {
        if (userId < 1)
            return null;

        @Language("MariaDB")
        String sql = """
                INSERT INTO %s (clan_id, user_id, group_id)
                VALUES (?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, userId);
            stmt.setString(3, groupId.toString());
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String joinedSql = "SELECT joined_at FROM %s WHERE clan_id = ? AND user_id = ?".formatted(TABLE_NAME);
        LocalDateTime joinedAt = database.query(joinedSql, null,
                resultSet -> resultSet.getTimestamp("joined_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, userId);
                });
        if (joinedAt == null) return null;

        ClanMember member = new ClanMember(clanId, userId, joinedAt, groupId);
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return member;
    }

    @Override
    public int delete(@NotNull UUID clanId, int userId) {
        if (userId < 1) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE clan_id = ? AND user_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setInt(2, userId);
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return row;
    }

    @Override
    public @Nullable ClanMember update(@NotNull UUID clanId, int userId, @NotNull UUID groupId) {
        if (userId < 1)
            return null;

        ClanMember existing = cache.get(clanId, userId);
        if (existing == null) return null;

        if (Objects.equals(groupId, existing.groupId())) return existing;

        @Language("MariaDB")
        String sql = "UPDATE %s SET group_id = ? WHERE clan_id = ? AND user_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, groupId.toString());
            stmt.setString(2, clanId.toString());
            stmt.setInt(3, userId);
        });
        if (row < 1) return null;

        ClanMember member = ClanMember.builder(existing)
                .groupId(groupId)
                .build();
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return member;
    }
}

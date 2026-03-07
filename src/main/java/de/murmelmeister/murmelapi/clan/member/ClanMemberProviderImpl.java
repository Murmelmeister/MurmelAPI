package de.murmelmeister.murmelapi.clan.member;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.clan.ClanMemberException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ClanMemberProviderImpl implements ClanMemberProvider {
    private static final String TABLE_NAME = "clan_member";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (clan_id, user_id, group_id)
            VALUES (?, ?, ?)
            RETURNING clan_id, user_id, joined_at, group_id
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE clan_id = ? AND user_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = "UPDATE %s SET group_id = ? WHERE clan_id = ? AND user_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (clan_id, user_id, group_id)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE group_id = VALUES(group_id)
            RETURNING clan_id, user_id, joined_at, group_id
            """.formatted(TABLE_NAME);

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
    public @NotNull @Unmodifiable List<ClanMember> findClan(@NotNull UUID clanId) {
        return cache.getByClanId(clanId);
    }

    @Override
    public @NotNull @Unmodifiable List<ClanMember> findClan(int userId) {
        return cache.getByUserId(userId);
    }

    @Override
    public @NotNull @Unmodifiable List<ClanMember> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable ClanMember create(@NotNull UUID clanId, int userId, @NotNull UUID groupId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        ClanMember member = MurmelExceptionWrapper.dbWrap(
                "Failed to create ClanMember (clanId=" + clanId + ", userId=" + userId + ", groupId=" + groupId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.clanMember(), stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, userId);
                    stmt.setString(3, groupId.toString());
                }),
                ClanMemberException::new
        );

        if (member == null) return null;
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return member;
    }

    @Override
    public int delete(@NotNull UUID clanId, int userId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete (clanId=" + clanId + ", userId=" + userId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, userId);
                }),
                ClanMemberException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return row;
    }

    @Override
    public @Nullable ClanMember update(@NotNull UUID clanId, int userId, @NotNull UUID groupId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        ClanMember existing = cache.get(clanId, userId);
        if (existing == null) return null;

        if (Objects.equals(groupId, existing.groupId())) return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update (clanId=" + clanId + ", userId=" + userId + ", groupId=" + groupId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, groupId.toString());
                    stmt.setString(2, clanId.toString());
                    stmt.setInt(3, userId);
                }),
                ClanMemberException::new
        );
        if (row != 1) return null;

        ClanMember member = ClanMember.builder(existing)
                .groupId(groupId)
                .build();
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return member;
    }

    @Override
    public @Nullable ClanMember upsert(@NotNull UUID clanId, int userId, @NotNull UUID groupId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");
        if (userId < 1) throw new IllegalArgumentException("userId must be >= 1");

        ClanMember existing = cache.get(clanId, userId);
        if (existing != null && Objects.equals(groupId, existing.groupId()))
            return existing;

        ClanMember saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert ClanMember (clanId=" + clanId + ", userId=" + userId + ", groupId=" + groupId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.clanMember(), stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setInt(2, userId);
                    stmt.setString(3, groupId.toString());
                }),
                ClanMemberException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, new ClanMemberCache.Member(clanId, userId));
        return saved;
    }
}

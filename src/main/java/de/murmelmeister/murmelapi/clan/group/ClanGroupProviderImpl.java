package de.murmelmeister.murmelapi.clan.group;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
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

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanGroupProviderImpl implements ClanGroupProvider {
    private static final String TABLE_NAME = "clan_groups";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final ClanGroupCache cache;
    private final RefreshType all = RefreshType.CLAN_GROUPS;
    private final RefreshType single = RefreshType.SINGLE_CLAN_GROUP;

    public ClanGroupProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new ClanGroupCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable ClanGroup findById(@Nullable UUID clanId, @Nullable UUID groupId) {
        return cache.getByKey(clanId, groupId);
    }

    @Override
    public @Nullable List<ClanGroup> findByClanId(@Nullable UUID clanId) {
        return cache.getByClanId(clanId);
    }

    @Override
    public @NotNull List<ClanGroup> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable ClanGroup create(@NotNull UUID clanId, @NotNull String groupName, int priority, boolean defaultGroup, int createdBy) {
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || priority < 0 || createdBy < CONSOLE_USER_ID)
            return null;

        UUID groupId = UUID.randomUUID();
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (group_id, clan_id, group_name, priority, created_by)
                VALUES (?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, groupId.toString());
            stmt.setString(2, clanId.toString());
            stmt.setString(3, normalizedGroupName);
            stmt.setInt(4, priority);
            stmt.setInt(5, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                });
        if (createdAt == null) return null;

        ClanGroup clanGroup = new ClanGroup(clanId, groupId, normalizedGroupName, priority, defaultGroup, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return clanGroup;
    }

    @Override
    public int delete(@NotNull UUID clanId, @NotNull UUID groupId) {
        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, clanId.toString());
            stmt.setString(2, groupId.toString());
        });
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return row;
    }

    @Override
    public @Nullable ClanGroup update(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int changedBy) {
        if (priority < 0 || changedBy < CONSOLE_USER_ID)
            return null;

        ClanGroup existing = cache.getByKey(clanId, groupId);
        if (existing == null) return null;

        if (Objects.equals(groupName, existing.groupName())
                && priority == existing.priority()
                && defaultGroup == existing.defaultGroup())
            return existing;

        String normalizedGroupName = StringUtil.normalize(groupName);
        @Language("MariaDB")
        String sql = """
                UPDATE %s SET
                    group_name = ?,
                    priority = ?,
                    default_group = ?,
                    changed_by = ?
                WHERE clan_id = ? AND group_id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(sql, stmt -> {
            stmt.setString(1, normalizedGroupName);
            stmt.setInt(2, priority);
            stmt.setBoolean(3, defaultGroup);
            stmt.setInt(4, changedBy);
            stmt.setString(5, clanId.toString());
            stmt.setString(6, groupId.toString());
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                });
        if (changedAt == null) return null;

        ClanGroup updated = ClanGroup.builder(existing)
                .groupName(normalizedGroupName)
                .priority(priority)
                .defaultGroup(defaultGroup)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return updated;
    }
}

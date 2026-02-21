package de.murmelmeister.murmelapi.clan.group;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.clan.ClanException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class ClanGroupProviderImpl implements ClanGroupProvider {
    private static final String TABLE_NAME = "clan_groups";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (group_id, clan_id, group_name, priority, created_by)
            VALUES (?, ?, ?, ?, ?)
            RETURNING group_id, clan_id, group_name, priority, is_default, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s SET
                group_name = ?,
                priority = ?,
                default_group = ?,
                changed_by = ?
            WHERE clan_id = ? AND group_id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
                    INSERT INTO %s (group_id, clan_id, group_name, priority, created_by)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        group_name = VALUES(group_name),
                        priority = VALUES(priority),
                        default_group = VALUES(default_group),
                        changed_by = ?
                    RETURNING group_id, clan_id, group_name, priority, is_default, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

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
    public @NotNull @Unmodifiable List<ClanGroup> findByClanId(@NotNull UUID clanId) {
        return cache.getByClanId(clanId);
    }

    @Override
    public @NotNull @Unmodifiable List<ClanGroup> findAll() {
        return cache.getAll();
    }

    @Override
    public @Nullable ClanGroup create(@NotNull UUID clanId, @NotNull String groupName, int priority, boolean defaultGroup, int createdBy) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        UUID groupId = UUID.randomUUID();
        ClanGroup clanGroup = MurmelExceptionWrapper.dbWrap(
                "Failed to create ClanGroup (clanId=" + clanId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.clanGroup(), stmt -> {
                    stmt.setString(1, groupId.toString());
                    stmt.setString(2, clanId.toString());
                    stmt.setString(3, normalizedGroupName);
                    stmt.setInt(4, priority);
                    stmt.setInt(5, createdBy);
                }),
                ClanException::new
        );

        if (clanGroup == null) return null;
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return clanGroup;
    }

    @Override
    public int delete(@NotNull UUID clanId, @NotNull UUID groupId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                }),
                ClanException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return row;
    }

    @Override
    public @Nullable ClanGroup update(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int changedBy) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);

        ClanGroup existing = cache.getByKey(clanId, groupId);
        if (existing == null) return null;

        if (Objects.equals(groupName, existing.groupName())
                && priority == existing.priority()
                && defaultGroup == existing.defaultGroup())
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, normalizedGroupName);
                    stmt.setInt(2, priority);
                    stmt.setBoolean(3, defaultGroup);
                    stmt.setInt(4, changedBy);
                    stmt.setString(5, clanId.toString());
                    stmt.setString(6, groupId.toString());
                }),
                ClanException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to get changedAt for ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        resultSet -> resultSet.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> {
                            stmt.setString(1, clanId.toString());
                            stmt.setString(2, groupId.toString());
                        }),
                ClanException::new
        );
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

    @Override
    public @Nullable ClanGroup upsert(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int executorId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        ClanGroup existing = cache.getByKey(clanId, groupId);
        if (existing != null
                && Objects.equals(groupName, existing.groupName())
                && priority == existing.priority()
                && defaultGroup == existing.defaultGroup())
            return existing;

        ClanGroup saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.clanGroup(), stmt -> {
                    stmt.setString(1, groupId.toString());
                    stmt.setString(2, clanId.toString());
                    stmt.setString(3, normalizedGroupName);
                    stmt.setInt(4, priority);
                    stmt.setInt(5, executorId);
                    stmt.setInt(6, executorId);
                }),
                ClanException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(clanId, groupId));
        return saved;
    }
}

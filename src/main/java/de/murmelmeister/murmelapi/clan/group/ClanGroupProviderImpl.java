package de.murmelmeister.murmelapi.clan.group;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.library.utils.StringUtil;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.clan.ClanGroupException;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

final class ClanGroupProviderImpl implements ClanGroupProvider {
    private static final String TABLE_NAME = "clan_groups";

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

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE clan_id = ? AND group_id = ?".formatted(TABLE_NAME);

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
    public @NotNull Optional<ClanGroup> findById(@NotNull UUID clanId, @NotNull UUID groupId) {
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
    public @NotNull Optional<ClanGroup> upsert(@NotNull UUID clanId, @NotNull UUID groupId, @NotNull String groupName, int priority, boolean defaultGroup, int executorId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupName, "groupName cannot be null");
        String normalizedGroupName = StringUtil.normalize(groupName);
        if (normalizedGroupName == null || normalizedGroupName.isBlank())
            throw new IllegalArgumentException("groupName cannot be blank");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        Optional<ClanGroup> existingOpt = cache.getByKey(clanId, groupId);
        if (existingOpt.isPresent()) {
            ClanGroup existing = existingOpt.get();
            if (Objects.equals(groupName, existing.groupName())
                    && priority == existing.priority()
                    && defaultGroup == existing.defaultGroup())
                return existingOpt;
        }

        ClanGroup saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.query(UPSERT_SQL, null, ClanGroupRowMapper::resultSet, stmt -> {
                    stmt.setString(1, groupId.toString());
                    stmt.setString(2, clanId.toString());
                    stmt.setString(3, normalizedGroupName);
                    stmt.setInt(4, priority);
                    stmt.setInt(5, executorId);
                    stmt.setInt(6, executorId);
                }),
                ClanGroupException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(saved.clanId(), saved.groupId()));
        return Optional.of(saved);
    }

    @Override
    public int delete(@NotNull UUID clanId, @NotNull UUID groupId) {
        Objects.requireNonNull(clanId, "clanId cannot be null");
        Objects.requireNonNull(groupId, "groupId cannot be null");

        Optional<ClanGroup> existingOpt = cache.getByKey(clanId, groupId);
        if (existingOpt.isEmpty()) return 0;
        ClanGroup existing = existingOpt.get();

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete ClanGroup (clanId=" + clanId + ", groupId=" + groupId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, clanId.toString());
                    stmt.setString(2, groupId.toString());
                }),
                ClanGroupException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new ClanGroupCache.GroupKey(existing.clanId(), existing.groupId()));
        return row;
    }
}

package de.murmelmeister.murmelapi.punishment.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentException;
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

public final class PunishmentUserProviderImpl implements PunishmentUserProvider {
    private static final String TABLE_NAME = "punishment_user";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (user_id, type_id, log_id)
            VALUES (?, ?, ?)
            RETURNING user_id, type_id, log_id
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE user_id = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s
            SET log_id = ?
            WHERE user_id = ? AND type_id = ?
            """.formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentUserCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_USERS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_USER;

    public PunishmentUserProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentUserCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable PunishmentUser findPunishedUser(@NotNull UUID userId, int typeId) {
        return cache.get(userId, typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentUser> findPunishedUsers(int typeId) {
        return cache.getByTypeId(typeId);
    }

    @Override
    public @Nullable PunishmentUser create(@NotNull UUID userId, int typeId, @NotNull UUID logId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(logId, "logId cannot be null");

        PunishmentUser punish = MurmelExceptionWrapper.dbWrap(
                "Failed to create PunishmentUser (userId=" + userId + ", typeId=" + typeId + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.punishmentUser(), stmt -> {
                    stmt.setString(1, userId.toString());
                    stmt.setInt(2, typeId);
                    stmt.setString(3, logId.toString());
                }),
                PunishmentException::new
        );

        if (punish == null) return null;
        refreshProvider.fireSingle(single, new PunishmentUserCache.PunishKey(userId, typeId));
        return punish;
    }

    @Override
    public int delete(@NotNull UUID userId, int typeId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PunishmentUser (userId=" + userId + ", typeId=" + typeId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, userId.toString());
                    stmt.setInt(2, typeId);
                }),
                PunishmentException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new PunishmentUserCache.PunishKey(userId, typeId));
        return row;
    }

    @Override
    public @Nullable PunishmentUser update(@NotNull UUID userId, int typeId, @NotNull UUID logId) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(logId, "logId cannot be null");

        PunishmentUser existing = cache.get(userId, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update PunishmentUser (userId=" + userId + ", typeId=" + typeId + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setString(1, logId.toString());
                    stmt.setString(2, userId.toString());
                    stmt.setInt(3, typeId);
                }),
                PunishmentException::new
        );
        if (row != 1) return null;

        PunishmentUser punish = PunishmentUser.builder(existing)
                .logId(logId)
                .build();
        refreshProvider.fireSingle(single, new PunishmentUserCache.PunishKey(userId, typeId));
        return punish;
    }
}

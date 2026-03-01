package de.murmelmeister.murmelapi.punishment.user;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentUserException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class PunishmentUserProviderImpl implements PunishmentUserProvider {
    private static final String TABLE_NAME = "punishment_user";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (mojang_id, type_id, log_id, expires_at)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                log_id = VALUES(log_id),
                expires_at = VALUES(expires_at)
            RETURNING mojang_id, type_id, log_id, expires_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE mojang_id = ? AND type_id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String EXPIRES_SQL = """
            DELETE FROM %s
            WHERE expires_at IS NOT NULL AND expires_at <= CURRENT_TIMESTAMP()
            RETURNING mojang_id, type_id
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
    public @NotNull Optional<PunishmentUser> findPunishedUser(@NotNull UUID mojangId, int typeId) {
        return cache.getByKey(mojangId, typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentUser> findPunishedUsers(int typeId) {
        return cache.getByTypeId(typeId);
    }

    @Override
    public @NotNull Optional<PunishmentUser> upsert(@NotNull UUID mojangId, int typeId, @NotNull UUID logId, @Nullable Long durationSecs) {
        Objects.requireNonNull(mojangId, "mojangId cannot be null");
        Objects.requireNonNull(logId, "logId cannot be null");

        LocalDateTime expiresAt = durationSecs != null ? LocalDateTime.now().plusSeconds(durationSecs) : null;
        Optional<PunishmentUser> optExisting = cache.getByKey(mojangId, typeId);
        if (optExisting.isPresent()) {
            if (Objects.equals(logId, optExisting.get().logId())
                    && Objects.equals(expiresAt, optExisting.get().expiresAt()))
                return optExisting;
        }

        PunishmentUser punish = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert PunishmentUser (mojangId=" + mojangId + ", typeId=" + typeId + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.punishmentUser(), stmt -> {
                    stmt.setString(1, mojangId.toString());
                    stmt.setInt(2, typeId);
                    stmt.setString(3, logId.toString());
                    stmt.setObject(4, expiresAt, Types.TIMESTAMP);
                }),
                PunishmentUserException::new
        );

        if (punish == null) return Optional.empty();
        refreshProvider.fireSingle(single, new PunishmentUserCache.PunishKey(mojangId, typeId));
        return Optional.of(punish);
    }

    @Override
    public int delete(@NotNull UUID mojangId, int typeId) {
        Objects.requireNonNull(mojangId, "mojangId cannot be null");

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PunishmentUser (mojangId=" + mojangId + ", typeId=" + typeId + ")",
                () -> database.update(DELETE_SQL, stmt -> {
                    stmt.setString(1, mojangId.toString());
                    stmt.setInt(2, typeId);
                }),
                PunishmentUserException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, new PunishmentUserCache.PunishKey(mojangId, typeId));
        return row;
    }

    @Override
    public int loadExpired() {
        List<PunishmentUserCache.PunishKey> expiredKeys = MurmelExceptionWrapper.dbWrap(
                "Failed to remove expired Permissions",
                () -> database.queryList(
                        EXPIRES_SQL,
                        resultSet -> {
                            UUID mojangId = UUID.fromString(resultSet.getString("mojang_id"));
                            int typeId = resultSet.getInt("type_id");
                            return new PunishmentUserCache.PunishKey(mojangId, typeId);
                        },
                        null
                ),
                PunishmentUserException::new
        );

        if (expiredKeys == null || expiredKeys.isEmpty())
            return 0;

        expiredKeys.forEach(key -> refreshProvider.fireSingle(single, key));
        return expiredKeys.size();
    }
}

package de.murmelmeister.murmelapi.punishment.reason;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.exceptions.MurmelExceptionWrapper;
import de.murmelmeister.murmelapi.exceptions.punishment.PunishmentReasonException;
import de.murmelmeister.murmelapi.utils.ResultSetUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.sql.Types;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentReasonProviderImpl implements PunishmentReasonProvider {
    private static final String TABLE_NAME = "punishment_reasons";

    @Language("MariaDB")
    private static final String UPSERT_SQL = """
            INSERT INTO %s (id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
                type_id = VALUES(type_id),
                reason_text = VALUES(reason_text),
                duration_secs = VALUES(duration_secs),
                auto_flag_ip = VALUES(auto_flag_ip),
                auto_punish = VALUES(auto_punish),
                changed_by = ?
            RETURNING id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentReasonCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_REASONS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_REASON;

    public PunishmentReasonProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentReasonCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @NotNull Optional<PunishmentReason> findReason(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentReason> findReasons(int typeId) {
        return cache.getByType(typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentReason> findAll() {
        return cache.getAll();
    }


    @Override
    public @NotNull Optional<PunishmentReason> upsert(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs,
                                                      boolean autoFlagIp, boolean autoPunish, int executorId) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (reasonText.isBlank()) throw new IllegalArgumentException("reasonText must not be blank");
        if (durationSecs != null && durationSecs < 0)
            throw new IllegalArgumentException("durationSecs must be null or >= 0");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        Optional<PunishmentReason> optExisting = cache.getById(id);
        if (optExisting.isPresent()) {
            PunishmentReason existing = optExisting.get();
            if (typeId == existing.typeId()
                    && Objects.equals(reasonText, existing.reasonText())
                    && Objects.equals(durationSecs, existing.durationSecs())
                    && autoFlagIp == existing.autoFlagIp()
                    && autoPunish == existing.autoPunish())
                return optExisting;
        }

        PunishmentReason saved = MurmelExceptionWrapper.dbWrap(
                "Failed to upsert PunishmentReason (id=" + id + ")",
                () -> database.query(UPSERT_SQL, null, ResultSetUtil.punishmentReason(), stmt -> {
                    stmt.setInt(1, id);
                    stmt.setInt(2, typeId);
                    stmt.setString(3, reasonText);
                    stmt.setObject(4, durationSecs, Types.BIGINT);
                    stmt.setBoolean(5, autoFlagIp);
                    stmt.setBoolean(6, autoPunish);
                    stmt.setInt(7, executorId);
                    stmt.setInt(8, executorId);
                }),
                PunishmentReasonException::new
        );

        if (saved == null) return Optional.empty();
        refreshProvider.fireSingle(single, saved);
        return Optional.of(saved);
    }

    @Override
    public @NotNull Optional<PunishmentReason> upsert(@NotNull PunishmentReason reason, int executorId) {
        return upsert(
                reason.id(),
                reason.typeId(),
                reason.reasonText(),
                reason.durationSecs(),
                reason.autoFlagIp(),
                reason.autoPunish(),
                executorId
        );
    }

    @Override
    public int delete(int id) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");

        Optional<PunishmentReason> existing = cache.getById(id);
        if (existing.isEmpty()) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PunishmentReason (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, id)),
                PunishmentReasonException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing.get());
        return row;
    }
}

package de.murmelmeister.murmelapi.punishment.reason;

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

import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentReasonProviderImpl implements PunishmentReasonProvider {
    private static final String TABLE_NAME = "punishment_reasons";

    @Language("MariaDB")
    private static final String CREATE_SQL = """
            INSERT INTO %s (id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            RETURNING id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by, created_at, changed_by, changed_at
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String DELETE_SQL = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SQL = """
            UPDATE %s SET
                type_id = ?,
                reason_text = ?,
                duration_secs = ?,
                auto_flag_ip = ?,
                auto_punish = ?,
                changed_by = ?
            WHERE id = ?
            """.formatted(TABLE_NAME);

    @Language("MariaDB")
    private static final String UPDATE_SELECT_SQL = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);

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
    public @Nullable PunishmentReason getReason(int id) {
        return cache.getById(id);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentReason> getReasonsByType(int typeId) {
        return cache.getByType(typeId);
    }

    @Override
    public @NotNull @Unmodifiable List<PunishmentReason> getAllReasons() {
        return cache.getAll();
    }

    @Override
    public @Nullable PunishmentReason create(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (reasonText.isBlank()) throw new IllegalArgumentException("reasonText must not be blank");
        if (durationSecs != null && durationSecs < 0)
            throw new IllegalArgumentException("durationSecs must be null or >= 0");
        if (createdBy < CONSOLE_USER_ID) throw new IllegalArgumentException("createdBy must be >= " + CONSOLE_USER_ID);

        PunishmentReason reason = MurmelExceptionWrapper.dbWrap(
                "Failed to create PunishmentReason (id=" + id + ")",
                () -> database.query(CREATE_SQL, null, ResultSetUtil.punishmentReason(), stmt -> {
                    stmt.setInt(1, id);
                    stmt.setInt(2, typeId);
                    stmt.setString(3, reasonText);
                    stmt.setObject(4, durationSecs, Types.BIGINT);
                    stmt.setBoolean(5, autoFlagIp);
                    stmt.setBoolean(6, autoPunish);
                    stmt.setInt(7, createdBy);
                }),
                PunishmentException::new
        );

        if (reason == null) return null;
        refreshProvider.fireSingle(single, reason);
        return reason;
    }

    @Override
    public int delete(int id) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");

        PunishmentReason existing = cache.getById(id);
        if (existing == null) return 0;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to delete PunishmentReason (id=" + id + ")",
                () -> database.update(DELETE_SQL, stmt -> stmt.setInt(1, id)),
                PunishmentException::new
        );

        if (row != 1) return 0;
        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public @Nullable PunishmentReason update(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (reasonText.isBlank()) throw new IllegalArgumentException("reasonText must not be blank");
        if (durationSecs != null && durationSecs < 0)
            throw new IllegalArgumentException("durationSecs must be null or >= 0");
        if (changedBy < CONSOLE_USER_ID) throw new IllegalArgumentException("changedBy must be >= " + CONSOLE_USER_ID);


        PunishmentReason existing = cache.getById(id);
        if (existing == null) return null;

        if (typeId == existing.typeId()
                && Objects.equals(reasonText, existing.reasonText())
                && Objects.equals(durationSecs, existing.durationSecs())
                && autoFlagIp == existing.autoFlagIp()
                && autoPunish == existing.autoPunish())
            return existing;

        int row = MurmelExceptionWrapper.dbWrap(
                "Failed to update PunishmentReason (id=" + id + ")",
                () -> database.update(UPDATE_SQL, stmt -> {
                    stmt.setInt(1, typeId);
                    stmt.setString(2, reasonText);
                    stmt.setObject(3, durationSecs, Types.BIGINT);
                    stmt.setBoolean(4, autoFlagIp);
                    stmt.setBoolean(5, autoPunish);
                    stmt.setInt(6, changedBy);
                    stmt.setInt(7, id);
                }),
                PunishmentException::new
        );
        if (row != 1) return null;

        LocalDateTime changedAt = MurmelExceptionWrapper.dbWrap(
                "Failed to load changed_at for PunishmentReason (id=" + id + ")",
                () -> database.query(UPDATE_SELECT_SQL, null,
                        result -> result.getTimestamp("changed_at").toLocalDateTime(),
                        stmt -> stmt.setInt(1, id)),
                PunishmentException::new
        );
        if (changedAt == null) return null;

        PunishmentReason reason = PunishmentReason.builder(existing)
                .typeId(typeId)
                .reasonText(reasonText)
                .durationSecs(durationSecs)
                .autoFlagIp(autoFlagIp)
                .autoPunish(autoPunish)
                .changedBy(changedBy)
                .changedAt(changedAt)
                .build();
        refreshProvider.fireSingle(single, reason);
        return reason;
    }

    @Override
    public @Nullable PunishmentReason upsert(int id, int typeId, @NotNull String reasonText, @Nullable Long durationSecs,
                                             boolean autoFlagIp, boolean autoPunish, int executorId) {
        if (id < 1) throw new IllegalArgumentException("id must be >= 1");
        if (typeId < 1) throw new IllegalArgumentException("typeId must be >= 1");
        if (reasonText.isBlank()) throw new IllegalArgumentException("reasonText must not be blank");
        if (durationSecs != null && durationSecs < 0)
            throw new IllegalArgumentException("durationSecs must be null or >= 0");
        if (executorId < CONSOLE_USER_ID)
            throw new IllegalArgumentException("executorId must be >= " + CONSOLE_USER_ID);

        PunishmentReason existing = cache.getById(id);
        if (existing != null
                && typeId == existing.typeId()
                && Objects.equals(reasonText, existing.reasonText())
                && Objects.equals(durationSecs, existing.durationSecs())
                && autoFlagIp == existing.autoFlagIp()
                && autoPunish == existing.autoPunish())
            return existing;

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
                PunishmentException::new
        );

        if (saved == null) return null;
        refreshProvider.fireSingle(single, saved);
        return saved;
    }

    @Override
    public @Nullable PunishmentReason upsert(@NotNull PunishmentReason reason, int executorId) {
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
}

package de.murmelmeister.murmelapi.punishment.reason;

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

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentReasonProviderImpl implements PunishmentReasonProvider {
    private static final String TABLE_NAME = "punishment_reasons";

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
    public @Nullable List<PunishmentReason> getReasonsByType(int typeId) {
        return cache.getByType(typeId);
    }

    @Override
    public @NotNull List<PunishmentReason> getAllReasons() {
        return cache.getCachedPunishReasons();
    }

    @Override
    public @Nullable PunishmentReason create(int id, int typeId, @NotNull String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy) {
        if (id < 1 || typeId < 1 || reasonText.isEmpty() || createdBy < CONSOLE_USER_ID)
            return null;

        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, id);
            stmt.setInt(2, typeId);
            stmt.setString(3, reasonText);
            stmt.setLong(4, durationSecs);
            stmt.setBoolean(5, autoFlagIp);
            stmt.setBoolean(6, autoPunish);
            stmt.setInt(7, createdBy);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                result -> result.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, id));
        if (createdAt == null) return null;

        PunishmentReason reason = new PunishmentReason(id, typeId, reasonText, durationSecs, autoFlagIp, autoPunish, createdBy, createdAt, null, null);
        refreshProvider.fireSingle(single, reason);
        return reason;
    }

    @Override
    public int delete(int id) {
        PunishmentReason existing = cache.getById(id);
        if (existing == null) return 0;

        @Language("MariaDB")
        String sql = "DELETE FROM %s WHERE id = ?".formatted(TABLE_NAME);
        int row = database.update(sql,
                stmt -> stmt.setInt(1, id));
        if (row < 1) return 0;

        refreshProvider.fireSingle(single, existing);
        return row;
    }

    @Override
    public PunishmentReason update(int id, int typeId, @NotNull String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy) {
        if (id < 1 || typeId < 1 || reasonText.isEmpty() || changedBy < CONSOLE_USER_ID)
            return null;

        PunishmentReason existing = cache.getById(id);
        if (existing == null) return null;

        if (typeId == existing.typeId() &&
                Objects.equals(reasonText, existing.reasonText()) &&
                Objects.equals(durationSecs, existing.durationSecs()) &&
                autoFlagIp == existing.autoFlagIp() &&
                autoPunish == existing.autoPunish())
            return existing; // No changes

        @Language("MariaDB")
        String insertSql = """
                UPDATE %s SET
                    type_id = ?,
                    reason_text = ?,
                    duration_secs = ?,
                    auto_flag_ip = ?,
                    auto_punish = ?,
                    changed_by = ?
                WHERE id = ?
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setInt(1, typeId);
            stmt.setString(2, reasonText);
            stmt.setLong(3, durationSecs);
            stmt.setBoolean(4, autoFlagIp);
            stmt.setBoolean(5, autoPunish);
            stmt.setInt(6, changedBy);
            stmt.setInt(7, id);
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT changed_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime changedAt = database.query(selectSql, null,
                result -> result.getTimestamp("changed_at").toLocalDateTime(),
                stmt -> stmt.setInt(1, id));
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
}

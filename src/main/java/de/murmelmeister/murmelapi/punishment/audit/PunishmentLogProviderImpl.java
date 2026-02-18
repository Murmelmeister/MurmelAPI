package de.murmelmeister.murmelapi.punishment.audit;

import com.google.gson.Gson;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.utils.update.RefreshProvider;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

public final class PunishmentLogProviderImpl implements PunishmentLogProvider {
    private static final String TABLE_NAME = "punishment_logs";

    private final Database database;
    private final RefreshProvider refreshProvider;
    private final PunishmentLogCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_LOGS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_LOG;

    public PunishmentLogProviderImpl(Database database, Gson gson, RefreshProvider refreshProvider, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.refreshProvider = refreshProvider;
        this.cache = new PunishmentLogCache(database, gson, refreshProvider, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        refreshProvider.fireCache(all);
    }

    @Override
    public @Nullable PunishmentLog getLog(@Nullable UUID logId) {
        return cache.getById(logId);
    }

    @Override
    public @Nullable List<PunishmentLog> getLogsByUserId(int userId) {
        return cache.getByUser(userId);
    }

    @Override
    public @Nullable List<PunishmentLog> getLogsByIpAddress(@NotNull InetAddress inetAddress) {
        return cache.getByIp(inetAddress);
    }

    @Override
    public @NotNull List<PunishmentLog> getLogs() {
        return cache.getCachedPunishLogs();
    }

    private @Nullable PunishmentLog insertAndLoadLog(@NotNull PunishmentLog.Action action, @Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (
                    id,
                    action,
                    user_id,
                    ip_address,
                    reason_id,
                    reason_type_id,
                    reason_text,
                    reason_duration,
                    reason_auto_flag_ip,
                    reason_auto_punish,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""".formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, action.name());
            stmt.setInt(3, userId == null ? Types.NULL : userId);
            stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
            stmt.setInt(5, reason.id());
            stmt.setInt(6, reason.typeId());
            stmt.setString(7, reason.reasonText());
            stmt.setLong(8, reason.durationSecs() == null ? Types.NULL : reason.durationSecs());
            stmt.setBoolean(9, reason.autoFlagIp());
            stmt.setBoolean(10, reason.autoPunish());
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, logId.toString()));
        if (createdAt == null) return null;

        return new PunishmentLog(logId, action, userId, inetAddress, reason.id(),
                reason.typeId(), reason.reasonText(), reason.durationSecs(),
                reason.autoFlagIp(), reason.autoPunish(), createdBy, createdAt);
    }

    @Override
    public @Nullable PunishmentLog create(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.CREATED;
        PunishmentLog log = insertAndLoadLog(action, userId, inetAddress, reason, createdBy);
        if (log == null) return null;

        refreshProvider.fireSingle(single, log);
        return log;
    }

    @Override
    public @Nullable PunishmentLog modify(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.MODIFIED;
        PunishmentLog log = insertAndLoadLog(action, userId, inetAddress, reason, createdBy);
        if (log == null) return null;

        refreshProvider.fireSingle(single, log);
        return log;
    }

    @Override
    public @Nullable PunishmentLog revoke(@Nullable Integer userId, @Nullable InetAddress inetAddress, @NotNull PunishmentLog log, int createdBy) {
        if ((userId != null && userId < 1) || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        PunishmentLog.Action action = PunishmentLog.Action.REVOKED;
        @Language("MariaDB")
        String insertSql = """
                INSERT INTO %s (
                    id,
                    action,
                    user_id,
                    ip_address,
                    reason_id,
                    reason_type_id,
                    reason_text,
                    reason_duration,
                    reason_auto_flag_ip,
                    reason_auto_punish,
                    created_by
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.formatted(TABLE_NAME);
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, action.name());
            stmt.setInt(3, userId == null ? Types.NULL : userId);
            stmt.setString(4, inetAddress == null ? null : inetAddress.getHostAddress());
            stmt.setInt(5, log.reasonId() == null ? Types.NULL : log.reasonId());
            stmt.setInt(6, log.reasonTypeId());
            stmt.setString(7, log.reasonText());
            stmt.setLong(8, log.reasonDuration() == null ? Types.NULL : log.reasonDuration());
            stmt.setBoolean(9, log.reasonAutoFlagIp());
            stmt.setBoolean(10, log.reasonAutoPunish());
        });
        if (row < 1) return null;

        @Language("MariaDB")
        String selectSql = "SELECT created_at FROM %s WHERE id = ?".formatted(TABLE_NAME);
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, logId.toString()));
        if (createdAt == null) return null;

        PunishmentLog newLog = new PunishmentLog(logId, action, userId, inetAddress, log.reasonId(),
                log.reasonTypeId(), log.reasonText(), log.reasonDuration(),
                log.reasonAutoFlagIp(), log.reasonAutoPunish(), createdBy, createdAt);
        refreshProvider.fireSingle(single, newLog);
        return newLog;
    }
}

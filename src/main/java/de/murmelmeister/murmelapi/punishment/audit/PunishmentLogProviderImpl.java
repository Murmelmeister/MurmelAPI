package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

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
    private final PunishmentLogCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_LOGS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_LOG;

    public PunishmentLogProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new PunishmentLogCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    @Override
    public void refreshCache() {
        RefreshUtil.fireCache(all);
    }

    @Override
    public PunishmentLog getLog(UUID logId) {
        return cache.getById(logId);
    }

    @Override
    public List<PunishmentLog> getLogsByUserId(int userId) {
        return cache.getByUser(userId);
    }

    @Override
    public List<PunishmentLog> getLogsByIpAddress(InetAddress inetAddress) {
        return cache.getByIp(inetAddress);
    }

    @Override
    public List<PunishmentLog> getLogs() {
        return cache.getCachedPunishLogs();
    }

    private PunishmentLog insertAndLoadLog(PunishmentLog.Action action, Integer userId, InetAddress inetAddress, PunishmentReason reason, int createdBy) {
        if (action == null || (userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, action, user_id, ip_address, reason_id, " +
                "reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, " +
                "reason_auto_punish, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, action.name());
            stmt.setInt(3, userId == null ? Types.NULL : userId);
            stmt.setString(4, inetAddress.getHostAddress());
            stmt.setInt(5, reason.id());
            stmt.setInt(6, reason.typeId());
            stmt.setString(7, reason.reasonText());
            stmt.setLong(8, reason.durationSecs());
            stmt.setBoolean(9, reason.autoFlagIp());
            stmt.setBoolean(10, reason.autoPunish());
        });
                /*logId.toString(), action.name(), userId, ipAddress,
                reason.id(), reason.typeId(), reason.reasonText(), reason.durationSecs(),
                reason.autoFlagIp(), reason.autoPunish(), createdBy);*/
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, logId.toString()));
        if (createdAt == null) return null;

        return new PunishmentLog(logId, action, userId, inetAddress, reason.id(),
                reason.typeId(), reason.reasonText(), reason.durationSecs(),
                reason.autoFlagIp(), reason.autoPunish(), createdBy, createdAt);
    }

    @Override
    public PunishmentLog create(Integer userId, InetAddress inetAddress, PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.CREATED;
        PunishmentLog log = insertAndLoadLog(action, userId, inetAddress, reason, createdBy);
        if (log == null) return null;

        RefreshUtil.fireSingle(single, log.id());
        return log;
    }

    @Override
    public PunishmentLog modify(Integer userId, InetAddress inetAddress, PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.MODIFIED;
        PunishmentLog log = insertAndLoadLog(action, userId, inetAddress, reason, createdBy);
        if (log == null) return null;

        RefreshUtil.fireSingle(single, log.id());
        return log;
    }

    @Override
    public PunishmentLog revoke(Integer userId, InetAddress inetAddress, PunishmentLog log, int createdBy) {
        if ((userId != null && userId < 1) || log == null || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        PunishmentLog.Action action = PunishmentLog.Action.REVOKED;
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, action, user_id, ip_address, reason_id, " +
                "reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, " +
                "reason_auto_punish, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setString(2, action.name());
            stmt.setInt(3, userId == null ? Types.NULL : userId);
            stmt.setString(4, inetAddress.getHostAddress());
            stmt.setInt(5, log.reasonId());
            stmt.setInt(6, log.reasonTypeId());
            stmt.setString(7, log.reasonText());
            stmt.setLong(8, log.reasonDuration());
            stmt.setBoolean(9, log.reasonAutoFlagIp());
            stmt.setBoolean(10, log.reasonAutoPunish());
        });
        /*logId.toString(), action.name(), userId, ipAddress,
          log.reasonId(), log.reasonTypeId(), log.reasonText(), log.reasonDuration(),
          log.reasonAutoFlagIp(), log.reasonAutoPunish(), createdBy);*/
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null,
                resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(),
                stmt -> stmt.setString(1, logId.toString()));
        if (createdAt == null) return null;

        PunishmentLog newLog = new PunishmentLog(logId, action, userId, inetAddress, log.reasonId(),
                log.reasonTypeId(), log.reasonText(), log.reasonDuration(),
                log.reasonAutoFlagIp(), log.reasonAutoPunish(), createdBy, createdAt);
        RefreshUtil.fireSingle(single, newLog.id());
        return newLog;
    }
}

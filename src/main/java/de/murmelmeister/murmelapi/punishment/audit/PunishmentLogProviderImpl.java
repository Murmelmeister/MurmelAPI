package de.murmelmeister.murmelapi.punishment.audit;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

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

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "action ENUM('CREATED', 'MODIFIED', 'REVOKED') NOT NULL, " +
                                         "user_id INT NULL, " +
                                         "ip_address VARCHAR(45) NULL, " +
                                         "CONSTRAINT chk_user_or_ip_not_both_null CHECK (user_id IS NOT NULL OR ip_address IS NOT NULL), " +
                                         "reason_id INT NULL, " +
                                         "reason_type_id INT NOT NULL, " +
                                         "reason_text TEXT NOT NULL, " +
                                         "reason_duration BIGINT NULL, " +
                                         "reason_auto_flag_ip BOOLEAN NOT NULL, " +
                                         "reason_auto_punish BOOLEAN NOT NULL, " +
                                         "created_by INT NOT NULL, " +
                                         "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                                         "FOREIGN KEY (user_id) REFERENCES users(id), " +
                                         "FOREIGN KEY (reason_id) REFERENCES punishment_reasons(id) ON DELETE SET NULL ON UPDATE CASCADE, " +
                                         "FOREIGN KEY (created_by) REFERENCES users(id)"
        );
        database.update("CREATE INDEX IF NOT EXISTS idx_audit_user ON " + TABLE_NAME + " (user_id)");
        database.update("CREATE INDEX IF NOT EXISTS idx_audit_ip ON " + TABLE_NAME + " (ip_address)");
        database.update("CREATE INDEX IF NOT EXISTS idx_audit_reason ON " + TABLE_NAME + " (reason_id)");
    }

    @Override
    public void closeCache() {
        cache.close();
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
    public List<PunishmentLog> getLogsByIpAddress(String ipAddress) {
        return cache.getByIp(ipAddress);
    }

    @Override
    public List<PunishmentLog> getLogs() {
        return cache.getCachedPunishLogs();
    }

    private PunishmentLog insertAndLoadLog(PunishmentLog.Action action, Integer userId, String ipAddress, PunishmentReason reason, int createdBy) {
        if (action == null || (userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, action, user_id, ip_address, reason_id, " +
                           "reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, " +
                           "reason_auto_punish, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, logId.toString(), action.name(), userId, ipAddress,
                reason.id(), reason.typeId(), reason.reasonText(), reason.durationSecs(),
                reason.autoFlagIp(), reason.autoPunish(), createdBy);
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null, resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(), logId.toString());
        if (createdAt == null) return null;

        return new PunishmentLog(logId, action, userId, ipAddress, reason.id(),
                reason.typeId(), reason.reasonText(), reason.durationSecs(),
                reason.autoFlagIp(), reason.autoPunish(), createdBy, createdAt);
    }

    @Override
    public PunishmentLog create(Integer userId, String ipAddress, PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.CREATED;
        PunishmentLog log = insertAndLoadLog(action, userId, ipAddress, reason, createdBy);
        if (log == null) return null;

        RefreshUtil.fireSingle(single, log.id());
        cache.put(log);
        return log;
    }

    @Override
    public PunishmentLog modify(Integer userId, String ipAddress, PunishmentReason reason, int createdBy) {
        if ((userId != null && userId < 1) || reason == null || createdBy < CONSOLE_USER_ID)
            return null;

        PunishmentLog.Action action = PunishmentLog.Action.MODIFIED;
        PunishmentLog log = insertAndLoadLog(action, userId, ipAddress, reason, createdBy);
        if (log == null) return null;

        RefreshUtil.fireSingle(single, log.id());
        cache.put(log);
        return log;
    }

    @Override
    public PunishmentLog revoke(Integer userId, String ipAddress, PunishmentLog log, int createdBy) {
        if ((userId != null && userId < 1) || log == null || createdBy < CONSOLE_USER_ID)
            return null;

        UUID logId = UUID.randomUUID();
        PunishmentLog.Action action = PunishmentLog.Action.REVOKED;
        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, action, user_id, ip_address, reason_id, " +
                           "reason_type_id, reason_text, reason_duration, reason_auto_flag_ip, " +
                           "reason_auto_punish, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, logId.toString(), action.name(), userId, ipAddress,
                log.reasonId(), log.reasonTypeId(), log.reasonText(), log.reasonDuration(),
                log.reasonAutoFlagIp(), log.reasonAutoPunish(), createdBy);
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null, resultSet -> resultSet.getTimestamp("created_at").toLocalDateTime(), logId.toString());
        if (createdAt == null) return null;

        PunishmentLog newLog = new PunishmentLog(logId, action, userId, ipAddress, log.reasonId(),
                log.reasonTypeId(), log.reasonText(), log.reasonDuration(),
                log.reasonAutoFlagIp(), log.reasonAutoPunish(), createdBy, createdAt);
        RefreshUtil.fireSingle(single, newLog.id());
        cache.put(newLog);
        return newLog;
    }
}

package de.murmelmeister.murmelapi.punishment.reason;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static de.murmelmeister.murmelapi.user.UserProviderImpl.CONSOLE_USER_ID;

public final class PunishmentReasonProviderImpl implements PunishmentReasonProvider {
    private static final String TABLE_NAME = "punishment_reasons";

    private final Database database;
    private final PunishmentReasonCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_REASONS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_REASON;

    public PunishmentReasonProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new PunishmentReasonCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                                         "type_id INT NOT NULL, " +
                                         "reason_text TEXT NOT NULL, " +
                                         "duration_secs BIGINT NULL, " + // NULL = permanent, >0 = seconds
                                         "auto_flag_ip BOOLEAN NOT NULL DEFAULT FALSE, " +
                                         "auto_punish BOOLEAN NOT NULL DEFAULT FALSE, " +
                                         "created_by INT NOT NULL, " +
                                         "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP(), " +
                                         "changed_by INT NULL, " +
                                         "changed_at DATETIME NULL ON UPDATE CURRENT_TIMESTAMP(), " +
                                         "FOREIGN KEY (type_id) REFERENCES punishment_types(id), " +
                                         "FOREIGN KEY (created_by) REFERENCES users(id), " +
                                         "FOREIGN KEY (changed_by) REFERENCES users(id)"
        );
        database.update("CREATE INDEX IF NOT EXISTS idx_reason_type ON " + TABLE_NAME + " (type_id)");
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
    public PunishmentReason getReason(int id) {
        return cache.getById(id);
    }

    @Override
    public List<PunishmentReason> getReasonsByType(int typeId) {
        return cache.getByType(typeId);
    }

    @Override
    public List<PunishmentReason> getAllReasons() {
        return cache.getCachedPunishReasons();
    }

    @Override
    public PunishmentReason create(int id, int typeId, String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int createdBy) {
        if (id < 1 || typeId < 1 || (reasonText == null || reasonText.isEmpty()) || createdBy < CONSOLE_USER_ID)
            return null;

        String insertSql = "INSERT INTO " + TABLE_NAME + " (id, type_id, reason_text, duration_secs, auto_flag_ip, auto_punish, created_by) " +
                           "VALUES (?, ?, ?, ?, ?, ?, ?)";
        int row = database.update(insertSql, id, typeId, reasonText, durationSecs, autoFlagIp, autoPunish, createdBy);
        if (row < 1) return null;

        String selectSql = "SELECT created_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime createdAt = database.query(selectSql, null, result -> result.getTimestamp("created_at").toLocalDateTime(), id);
        if (createdAt == null) return null;

        PunishmentReason reason = new PunishmentReason(id, typeId, reasonText, durationSecs, autoFlagIp, autoPunish, createdBy, createdAt, null, null);
        RefreshUtil.fireSingle(single, reason.id());
        cache.put(reason);
        return reason;
    }

    @Override
    public int delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int row = database.update(sql, id);
        if (row < 1) return 0;

        cache.remove(id);
        RefreshUtil.fireSingle(single, id);
        return row;
    }

    @Override
    public PunishmentReason update(int id, int typeId, String reasonText, Long durationSecs, boolean autoFlagIp, boolean autoPunish, int changedBy) {
        if (id < 1 || typeId < 1 || (reasonText == null || reasonText.isEmpty()) || changedBy < CONSOLE_USER_ID)
            return null;

        PunishmentReason existing = cache.getById(id);
        if (existing == null) return null;

        if (typeId == existing.typeId() &&
            Objects.equals(reasonText, existing.reasonText()) &&
            Objects.equals(durationSecs, existing.durationSecs()) &&
            autoFlagIp == existing.autoFlagIp() &&
            autoPunish == existing.autoPunish())
            return existing; // No changes

        String insertSql = "UPDATE " + TABLE_NAME + " SET type_id = ?, reason_text = ?, duration_secs = ?, " +
                           "auto_flag_ip = ?, auto_punish = ?, changed_by = ? WHERE id = ?";
        int row = database.update(insertSql, typeId, reasonText, durationSecs, autoFlagIp, autoPunish, changedBy, id);
        if (row < 1) return null;

        String selectSql = "SELECT changed_at FROM " + TABLE_NAME + " WHERE id = ?";
        LocalDateTime changedAt = database.query(selectSql, null, result -> result.getTimestamp("changed_at").toLocalDateTime(), id);
        if (changedAt == null) return null;

        PunishmentReason reason = existing.withUpdateMeta(typeId, reasonText, durationSecs, autoFlagIp, autoPunish, changedBy, changedAt);
        RefreshUtil.fireSingle(single, id);
        cache.put(reason);
        return reason;
    }
}

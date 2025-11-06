package de.murmelmeister.murmelapi.punishment.user;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class PunishmentCurrentUserProviderImpl implements PunishmentCurrentUserProvider {
    private static final String TABLE_NAME = "punishment_current_user";

    private final Database database;
    private final PunishmentCurrentUserCache cache;
    private final RefreshType all = RefreshType.PUNISHMENT_USERS;
    private final RefreshType single = RefreshType.SINGLE_PUNISHMENT_USER;

    public PunishmentCurrentUserProviderImpl(Database database, Long fetchLimit, long cacheCapacity, Duration refreshInterval) {
        this.database = database;
        this.cache = new PunishmentCurrentUserCache(database, TABLE_NAME, fetchLimit, cacheCapacity, refreshInterval);
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "user_id INT NOT NULL, " +
                "type_id INT NOT NULL, " +
                "log_id VARCHAR(36) NOT NULL UNIQUE, " +
                "PRIMARY KEY (user_id, type_id), " +
                "FOREIGN KEY (user_id) REFERENCES users(id), " +
                "FOREIGN KEY (type_id) REFERENCES punishment_types(id), " +
                "FOREIGN KEY (log_id) REFERENCES punishment_logs(id)"
        );
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
    public List<Integer> getAllPunishedUserIds(int typeId) {
        return cache.getCachedPunishUsers().stream()
                .filter(punish -> punish.typeId() == typeId)
                .map(PunishmentCurrentUser::userId)
                .toList();
    }

    @Override
    public PunishmentCurrentUser getPunishedUser(int userId, int typeId) {
        return cache.get(userId, typeId);
    }

    @Override
    public PunishmentCurrentUser create(int userId, int typeId, UUID logId) {
        if (userId < 1 || typeId < 1 || logId == null)
            return null;

        String sql = "INSERT INTO " + TABLE_NAME + " (user_id, type_id, log_id) VALUES (?, ?, ?)";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, typeId);
            stmt.setString(3, logId.toString());
        });
        if (row < 1) return null;

        PunishmentCurrentUser punish = new PunishmentCurrentUser(userId, typeId, logId);
        RefreshUtil.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return punish;
    }

    @Override
    public int delete(int userId, int typeId) {
        if (userId < 1 || typeId < 1)
            return 0;

        String sql = "DELETE FROM " + TABLE_NAME + " WHERE user_id = ? AND type_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setInt(1, userId);
            stmt.setInt(2, typeId);
        });
        if (row < 1) return 0;

        RefreshUtil.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return row;
    }

    @Override
    public PunishmentCurrentUser update(int userId, int typeId, UUID logId) {
        if (userId < 1 || typeId < 1 || logId == null)
            return null;
        PunishmentCurrentUser existing = cache.get(userId, typeId);
        if (existing == null) return null;

        if (Objects.equals(logId, existing.logId()))
            return existing; // No change needed

        String sql = "UPDATE " + TABLE_NAME + " SET log_id = ? WHERE user_id = ? AND type_id = ?";
        int row = database.update(sql, stmt -> {
            stmt.setString(1, logId.toString());
            stmt.setInt(2, userId);
            stmt.setInt(3, typeId);
        });
        if (row < 1) return null;

        PunishmentCurrentUser punish = existing.withUpdateLog(logId);
        RefreshUtil.fireSingle(single, new PunishmentCurrentUserCache.UserTypeKey(userId, typeId));
        return punish;
    }
}

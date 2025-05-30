package de.murmelmeister.murmelapi.punishment.reason;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.sql.Timestamp;
import java.util.List;

public final class ReasonProvider {
    private static final String TABLE_NAME = "punishment_reasons";
    private static final ReasonCache CACHE = new ReasonCache();
    private final Database database;

    public ReasonProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY, " +
                                         "typeId INT, FOREIGN KEY (typeId) REFERENCES punishment_types(id), " +
                                         "reason TEXT, " +
                                         "duration BIGINT, " +
                                         "autoFlagIp BOOL, " +
                                         "autoPunish BOOL, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
    }

    static {
        RefreshUtil.register(cacheName -> {
            if ("reasons".equals(cacheName) || "global".equals(cacheName))
                CACHE.clear();
        });
    }

    public void loadData() {
        CACHE.clear();
        String sql = "SELECT * FROM " + TABLE_NAME;
        List<Reason> reasons = database.queryList(sql, result -> {
            int id = result.getInt("id");
            int typeId = result.getInt("typeId");
            String reasonText = result.getString("reason");
            long duration = result.getLong("duration");
            boolean autoFlagIp = result.getBoolean("autoFlagIp");
            boolean autoPunish = result.getBoolean("autoPunish");
            int createdBy = result.getInt("createdBy");
            Timestamp createdAt = result.getTimestamp("createdAt");
            int updatedBy = result.getInt("updatedBy");
            Timestamp updatedAt = result.getTimestamp("updatedAt");
            return new Reason(id, typeId, reasonText, duration, autoFlagIp, autoPunish, createdBy, createdAt, updatedBy, updatedAt);
        });
        reasons.forEach(CACHE::put);
    }

    private void ensureCache() {
        if (CACHE.isEmpty())
            loadData();
    }

    public Reason get(int id) {
        ensureCache();
        return CACHE.get(id);
    }

    public List<Reason> getReasons() {
        ensureCache();
        return CACHE.getReasons();
    }

    public Reason create(int id, int typeId, String reason, long duration, boolean autoFlagIp, boolean autoPunish, int createdBy) {
        if (typeId < 1 || reason == null || createdBy == -2) return null;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Reason reasonPunishment = new Reason(id, typeId, reason, duration, autoFlagIp, autoPunish, createdBy, now, createdBy, now);
        database.update("INSERT INTO " + TABLE_NAME + " (id, typeId, reason, duration, autoFlagIp, autoPunish, createdBy, updatedBy) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, typeId, reason, duration, autoFlagIp ? 1 : 0, autoPunish ? 1 : 0, createdBy, createdBy);
        CACHE.put(reasonPunishment);
        return reasonPunishment;
    }

    public int delete(int id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int rowsAffected = database.update(sql, id);
        CACHE.remove(id);
        return rowsAffected;
    }

    public Reason update(int id, Reason updatedReason) {
        if (updatedReason == null || updatedReason.getId() != id) return null;
        database.update("UPDATE " + TABLE_NAME + " SET typeId = ?, reason = ?, duration = ?, autoFlagIp = ?, autoPunish = ?, updatedBy = ? WHERE id = ?",
                updatedReason.getTypeId(), updatedReason.getReason(), updatedReason.getDuration(),
                updatedReason.isAutoIpFlag() ? 1 : 0, updatedReason.isAutoPunish() ? 1 : 0,
                updatedReason.getUpdatedBy(), id);
        CACHE.put(updatedReason);
        return updatedReason;
    }
}

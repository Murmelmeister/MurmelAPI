package de.murmelmeister.murmelapi.punishment.log;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * The PunishmentLogProvider class implements the PunishmentLog interface and manages punishment log records
 * in the database. It provides methods to check for log existence, add punishment logs (for both users and IPs),
 * delete logs, retrieve logs by user or IP, and get or update various log properties such as reason, expiration,
 * and creation/update metadata.
 */
public final class PunishmentLogProvider implements PunishmentLog {
    private static final String TABLE_NAME = "punishment_logs";

    private final Database database;
    private final PunishmentReason punishmentReason;

    public PunishmentLogProvider(Database database, PunishmentReason punishmentReason) {
        this.database = database;
        this.punishmentReason = punishmentReason;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id VARCHAR(36) PRIMARY KEY, " +
                                         "reasonId INT, FOREIGN KEY (reasonId) REFERENCES punishment_reasons(id), " +
                                         "userId INT, FOREIGN KEY (userId) REFERENCES users(id), " +
                                         "ipAddress VARCHAR(45), " +
                                         "expiredAt DATETIME, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        database.update("CREATE INDEX IF NOT EXISTS userId_index ON " + TABLE_NAME + " (userId)");
        database.update("CREATE INDEX IF NOT EXISTS ipAddress_index ON " + TABLE_NAME + " (ipAddress)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsLog(UUID id) {
        return id != null && database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), id.toString());
    }

    @Override
    public UUID addLogUser(int reasonId, int userId, String ipAddress, int createdBy) {
        UUID id = UUID.randomUUID();
        long duration = punishmentReason.getDuration(reasonId);
        Timestamp expiredAt = duration == -1 ? null : new Timestamp(System.currentTimeMillis() + duration);
        database.updateCallable(Procedure.CREATE_LOG_USER.getName(), id.toString(), reasonId, userId, ipAddress, expiredAt, createdBy, createdBy);
        return id;
    }

    @Override
    public UUID addLogIp(int reasonId, String ipAddress, int createdBy) {
        UUID id = UUID.randomUUID();
        long duration = punishmentReason.getDuration(reasonId);
        Timestamp expiredAt = duration == -1 ? null : new Timestamp(System.currentTimeMillis() + duration);
        database.updateCallable(Procedure.CREATE_LOG_IP.getName(), id.toString(), reasonId, ipAddress, expiredAt, createdBy, createdBy);
        return id;
    }

    @Override
    public int deleteUserLogs(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.DELETE_LOG_USER.getName(), userId);
    }

    @Override
    public List<UUID> getLogsByUser(int userId) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_DATA_BY_USER.getName(), result -> UUID.fromString(result.getString("id")), userId);
    }

    @Override
    public List<UUID> getLogsByIp(String ipAddress) {
        if (ipAddress == null) return null;
        return database.queryListCallable(Procedure.GET_DATA_BY_IP.getName(), result -> UUID.fromString(result.getString("id")), ipAddress);
    }

    @Override
    public int getUserId(UUID id) {
        if (id == null) return -2;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("userId"), id.toString());
    }

    @Override
    public String getIpAddress(UUID id) {
        if (id == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("ipAddress"), id.toString());
    }

    @Override
    public int getReasonId(UUID id) {
        if (id == null) return -1;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -1, resultSet -> resultSet.getInt("reasonId"), id.toString());
    }

    @Override
    public int setReason(UUID id, int reasonId, int updatedBy) {
        if (id == null || reasonId < 1 || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_REASON.getName(), id.toString(), reasonId, updatedBy);
    }

    @Override
    public Timestamp getExpiredAt(UUID id) {
        if (id == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("expiredAt"), id.toString());
    }

    @Override
    public String getExpiredDate(UUID id) {
        Timestamp expiredAt = getExpiredAt(id);
        return expiredAt == null ? null : getDateFormat().format(expiredAt);
    }

    @Override
    public int setExpiredAt(UUID id, long duration, int updatedBy) {
        if (id == null || updatedBy == -2) return 0;
        Timestamp expiredAt = duration == -1 ? null : new Timestamp(System.currentTimeMillis() + duration);
        return database.updateCallable(Procedure.UPDATE_EXPIRED_TIME.getName(), id.toString(), expiredAt, updatedBy);
    }

    @Override
    public boolean isExpired(UUID id) {
        if (id == null) return false;
        return database.queryCallable(Procedure.IS_EXPIRED.getName(), false, resultSet -> resultSet.getInt("expired") > 0, id.toString());
    }

    @Override
    public int getCreatedBy(UUID id) {
        if (id == null) return -2;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("createdBy"), id.toString());
    }

    @Override
    public Timestamp getCreatedAt(UUID id) {
        if (id == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), id.toString());
    }

    @Override
    public String getCreatedDate(UUID id) {
        Timestamp createdAt = getCreatedAt(id);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(UUID id) {
        if (id == null) return -2;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), id.toString());
    }

    @Override
    public Timestamp getUpdatedAt(UUID id) {
        if (id == null) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), id.toString());
    }

    @Override
    public String getUpdatedDate(UUID id) {
        Timestamp updatedAt = getUpdatedAt(id);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    private enum Procedure {
        CREATE_LOG_USER("punishmentLog_createUser", "p_id VARCHAR(36), p_reasonId INT, p_userId INT, p_ipAddress VARCHAR(45), p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (id, reasonId, userId, ipAddress, expiredAt, createdBy, updatedBy) " +
                "VALUES (p_id, p_reasonId, p_userId, p_ipAddress, p_expiredAt, p_createdBy, p_updatedBy)"),
        CREATE_LOG_IP("punishmentLog_createIp", "p_id VARCHAR(36), p_reasonId INT, p_ipAddress VARCHAR(45), p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (id, reasonId, ipAddress, expiredAt, createdBy, updatedBy) " +
                "VALUES (p_id, p_reasonId, p_ipAddress, p_expiredAt, p_createdBy, p_updatedBy)"),
        DELETE_LOG_USER("punishmentLog_deleteUser", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId"),
        GET_DATA_BY_ID("punishmentLog_getDataById", "p_id VARCHAR(36)", "SELECT * FROM [TABLE] WHERE id=p_id"),
        GET_DATA_BY_USER("punishmentLog_getDataByUser", "p_userId INT", "SELECT id FROM [TABLE] WHERE userId=p_userId"),
        GET_DATA_BY_IP("punishmentLog_getDataByIp", "p_ipAddress VARCHAR(45)", "SELECT id FROM [TABLE] WHERE ipAddress=p_ipAddress"),
        UPDATE_EXPIRED_TIME("punishmentLog_updateExpiredTime", "p_id VARCHAR(36), p_expiredAt DATETIME, p_updatedBy INT",
                "UPDATE [TABLE] SET expiredAt=p_expiredAt, updatedBy=p_updatedBy WHERE id=p_id"),
        UPDATE_REASON("punishmentLog_updateReason", "p_id VARCHAR(36), p_reasonId INT, p_updatedBy INT",
                "UPDATE [TABLE] SET reasonId=p_reasonId, updatedBy=p_updatedBy WHERE id=p_id"),
        IS_EXPIRED("punishmentLog_isExpired", "p_id VARCHAR(36)",
                "SELECT IF(expiredAt IS NULL, 0, expiredAt <= CURRENT_TIMESTAMP()) AS expired FROM [TABLE] WHERE id=p_id;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}

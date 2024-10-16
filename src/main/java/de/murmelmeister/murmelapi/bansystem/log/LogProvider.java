package de.murmelmeister.murmelapi.bansystem.log;

import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;

public final class LogProvider implements Log {
    private final Reason reason;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public LogProvider(String tableName, Reason reason) {
        this.reason = reason;
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    public void createTable(String tableName) {
        Database.createTable(tableName, "LogID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, CreatorID INT, ReasonID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255)");
    }

    @Override
    public boolean existsLog(int logId) {
        return Database.callExists(Procedure.LOG_GET.getName(), logId);
    }

    @Override
    public int addLog(int userId, int creatorId, int reasonId, long time) {
        if (!this.reason.exists(reasonId)) throw new IllegalArgumentException("Reason does not exist");
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.LOG_ADD.getName(), userId, creatorId, reasonId, System.currentTimeMillis(), expired);
        return getLogId(userId);
    }

    @Override
    public void removeLog(int logId) {
        Database.callUpdate(Procedure.LOG_REMOVE.getName(), logId);
    }

    @Override
    public void deleteLog(int userId) {
        Database.callUpdate(Procedure.LOG_DELETE.getName(), userId);
    }

    @Override
    public int getLogId(int userId) {
        return Database.callQuery(-1, "LogID", int.class, Procedure.LOG_ID.getName(), userId);
    }

    @Override
    public List<Integer> getLogs(int userId) {
        return Database.callQueryList("LogID", int.class, Procedure.LOG_ID.getName(), userId);
    }

    @Override
    public int getUserId(int logId) {
        return Database.callQuery(-2, "UserID", int.class, Procedure.LOG_ID.getName(), logId);
    }

    @Override
    public int getCreatorId(int logId) {
        return Database.callQuery(-2, "CreatorID", int.class, Procedure.LOG_ID.getName(), logId);
    }

    @Override
    public long getCreatedTime(int logId) {
        return Database.callQuery(-1L, "CreatedTime", long.class, Procedure.LOG_ID.getName(), logId);
    }

    @Override
    public String getCreatedDate(int logId) {
        return dateFormat.format(getCreatedTime(logId));
    }

    @Override
    public long getExpiredTime(int logId) {
        return Database.callQuery(-2L, "ExpiredTime", long.class, Procedure.LOG_ID.getName(), logId);
    }

    @Override
    public String getExpiredDate(int logId) {
        long time = getExpiredTime(logId);
        return time == -1 ? "never" : dateFormat.format(time);
    }

    @Override
    public String setExpiredTime(int logId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public String addExpiredTime(int logId, long time) {
        long current = getExpiredTime(logId);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.callUpdate(Procedure.LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public String removeExpiredTime(int logId, long time) {
        long current = getExpiredTime(logId);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.callUpdate(Procedure.LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public int getReasonId(int logId) {
        return Database.callQuery(-1, "ReasonID", int.class, Procedure.LOG_ID.getName(), logId);
    }

    @Override
    public void setReasonId(int logId, int reasonId) {
        Database.callUpdate(Procedure.LOG_REASON_UPDATE.getName(), logId, reasonId);
    }

    @Override
    public String getReason(int logId) {
        return this.reason.get(getReasonId(logId));
    }

    private enum Procedure {
        LOG_ADD("Log_Add", "uid INT, cid INT, rid INT, created BIGINT(255), expired BIGINT(255)",
                "INSERT INTO [TABLE] (UserID, CreatorID, ReasonID, CreatedTime, ExpiredTime) VALUES (uid, cid, rid, created, expired);"),
        LOG_REMOVE("Log_Remove", "id INT", "DELETE FROM [TABLE] WHERE LogID=id;"),
        LOG_DELETE("Log_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        LOG_ID("Log_ID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        LOG_GET("Log_Get", "id INT", "SELECT * FROM [TABLE] WHERE LogID=id;"),
        LOG_EXPIRED("Log_Expired", "id INT, expired BIGINT(255)", "UPDATE [TABLE] SET ExpiredTime=expired WHERE LogID=id;"),
        LOG_REASON_UPDATE("Log_Reason_Update", "id INT, rid INT", "UPDATE [TABLE] SET ReasonID=rid WHERE LogID=id;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQueryWithoutObjects(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery(String tableName) {
            return query.replace("[TABLE]", tableName);
        }

        public static void loadAll(String tableName) {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery(tableName));
        }
    }
}

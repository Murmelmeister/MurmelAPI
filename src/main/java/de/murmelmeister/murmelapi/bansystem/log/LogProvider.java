package de.murmelmeister.murmelapi.bansystem.log;

import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;

public final class LogProvider implements Log {
    private final String tableName;
    private final Reason reason;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public LogProvider(String tableName, Reason reason) {
        this.tableName = tableName;
        this.reason = reason;
        createTable();
        Procedure.loadAll(tableName);
    }

    public void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (LogID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, CreatorID INT, ReasonID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", tableName);
    }

    @Override
    public boolean existsLog(int logId) {
        return Database.existsCall(Procedure.PROCEDURE_LOG_GET.getName(), logId);
    }

    @Override
    public int addLog(int userId, int creatorId, int reasonId, long time) {
        if (!this.reason.exists(reasonId)) throw new IllegalArgumentException("Reason does not exist");
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.PROCEDURE_LOG_ADD.getName(), userId, creatorId, reasonId, System.currentTimeMillis(), expired);
        return getLogId(userId);
    }

    @Override
    public void removeLog(int logId) {
        Database.updateCall(Procedure.PROCEDURE_LOG_REMOVE.getName(), logId);
    }

    @Override
    public void deleteLog(int userId) {
        Database.updateCall(Procedure.PROCEDURE_LOG_DELETE.getName(), userId);
    }

    @Override
    public int getLogId(int userId) {
        return Database.getIntCall(-1, "LogID", Procedure.PROCEDURE_LOG_ID.getName(), userId);
    }

    @Override
    public List<Integer> getLogs(int userId) {
        return Database.getIntListCall("LogID", Procedure.PROCEDURE_LOG_ID.getName(), userId);
    }

    @Override
    public int getUserId(int logId) {
        return Database.getIntCall(-1, "UserID", Procedure.PROCEDURE_LOG_ID.getName(), logId);
    }

    @Override
    public int getCreatorId(int logId) {
        return Database.getIntCall(-1, "CreatorID", Procedure.PROCEDURE_LOG_ID.getName(), logId);
    }

    @Override
    public long getCreatedTime(int logId) {
        return Database.getIntCall(-1, "CreatedTime", Procedure.PROCEDURE_LOG_ID.getName(), logId);
    }

    @Override
    public String getCreatedDate(int logId) {
        return DATE_FORMAT.format(getCreatedTime(logId));
    }

    @Override
    public long getExpiredTime(int logId) {
        return Database.getIntCall(-2, "ExpiredTime", Procedure.PROCEDURE_LOG_ID.getName(), logId);
    }

    @Override
    public String getExpiredDate(int logId) {
        return DATE_FORMAT.format(getExpiredTime(logId));
    }

    @Override
    public String setExpiredTime(int logId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.PROCEDURE_LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public String addExpiredTime(int logId, long time) {
        long current = getExpiredTime(logId);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.updateCall(Procedure.PROCEDURE_LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public String removeExpiredTime(int logId, long time) {
        long current = getExpiredTime(logId);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.updateCall(Procedure.PROCEDURE_LOG_EXPIRED.getName(), logId, expired);
        return getExpiredDate(logId);
    }

    @Override
    public int getReasonId(int logId) {
        return Database.getIntCall(-1, "ReasonID", Procedure.PROCEDURE_LOG_ID.getName(), logId);
    }

    @Override
    public void setReasonId(int logId, int reasonId) {
        Database.updateCall(Procedure.PROCEDURE_LOG_REASON_UPDATE.getName(), logId, reasonId);
    }

    @Override
    public String getReason(int logId) {
        return this.reason.get(getReasonId(logId));
    }

    private enum Procedure {
        PROCEDURE_LOG_ADD("Log_Add", "uid INT, cid INT, rid INT, created BIGINT(255), expired BIGINT(255)",
                "INSERT INTO [TABLE] (UserID, CreatorID, ReasonID, CreatedTime, ExpiredTime) VALUES (uid, cid, rid, created, expired);"),
        PROCEDURE_LOG_REMOVE("Log_Remove", "id INT", "DELETE FROM [TABLE] WHERE LogID=id;"),
        PROCEDURE_LOG_DELETE("Log_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        PROCEDURE_LOG_ID("Log_ID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        PROCEDURE_LOG_GET("Log_Get", "id INT", "SELECT * FROM [TABLE] WHERE LogID=id;"),
        PROCEDURE_LOG_EXPIRED("Log_Expired", "id INT, expired BIGINT(255)", "UPDATE [TABLE] SET ExpiredTime=expired WHERE LogID=id;"),
        PROCEDURE_LOG_REASON_UPDATE("Log_Reason_Update", "id INT, rid INT", "UPDATE [TABLE] SET ReasonID=rid WHERE LogID=id;"),
        ;
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

package de.murmelmeister.murmelapi.bansystem.ban;

import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.log.LogProvider;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.bansystem.reason.ReasonProvider;
import de.murmelmeister.murmelapi.utils.Database;

public final class BanProvider implements Ban {
    private final String tableName = "Ban_List";
    private final Reason reason;
    private final Log log;

    public BanProvider() {
        this.reason = new ReasonProvider("Ban_Reason");
        this.log = new LogProvider("Ban_Log", reason);
        createTable();
        Procedure.loadAll(tableName);
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (UserID INT, ExpiredTime BIGINT(255))", tableName);
    }

    @Override
    public void ban(int userId, int creatorId, int reasonId, long time) {
        int logId = log.addLog(userId, creatorId, reasonId, time);
        Database.updateCall(Procedure.PROCEDURE_BAN_ADD.getName(), userId, log.getExpiredTime(logId));
    }

    @Override
    public void unban(int userId) {
        Database.updateCall(Procedure.PROCEDURE_BAN_REMOVE.getName(), userId);
    }

    @Override
    public long getExpiredTime(int userId) {
        return Database.getLongCall(-2, "ExpiredTime", Procedure.PROCEDURE_BAN_GET.getName(), userId);
    }

    @Override
    public boolean isBanned(int userId) {
        long time = this.getExpiredTime(userId);
        return time == -1 || time >= System.currentTimeMillis();
    }

    @Override
    public Reason getReason() {
        return reason;
    }

    @Override
    public Log getLog() {
        return log;
    }

    private enum Procedure {
        PROCEDURE_BAN_ADD("Ban_Add", "uid INT, expired BIGINT(255)", "INSERT INTO [TABLE] VALUES (uid, expired);"),
        PROCEDURE_BAN_REMOVE("Ban_Remove", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        PROCEDURE_BAN_GET("Ban_Get", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
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

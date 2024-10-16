package de.murmelmeister.murmelapi.bansystem.ban;

import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.log.LogProvider;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.bansystem.reason.ReasonProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class BanProvider implements Ban {
    private final Reason reason;
    private final Log log;

    public BanProvider() {
        this.reason = new ReasonProvider("Ban_Reason");
        this.log = new LogProvider("Ban_Log", reason);
        String tableName = "Ban_List";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "UserID INT, ExpiredTime BIGINT(255)");
    }

    @Override
    public void ban(int userId, int creatorId, int reasonId, long time) {
        int logId = log.addLog(userId, creatorId, reasonId, time);
        Database.callUpdate(Procedure.BAN_ADD.getName(), userId, log.getExpiredTime(logId));
    }

    @Override
    public void unban(int userId) {
        Database.callUpdate(Procedure.BAN_REMOVE.getName(), userId);
    }

    @Override
    public long getExpiredTime(int userId) {
        return Database.callQuery(-2L, "ExpiredTime", long.class, Procedure.BAN_GET.getName(), userId);
    }

    @Override
    public String getExpiredDate(int userId) {
        long time = getExpiredTime(userId);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public boolean isBanned(int userId) {
        long time = getExpiredTime(userId);
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
        BAN_ADD("Ban_Add", "uid INT, expired BIGINT(255)", "INSERT INTO [TABLE] VALUES (uid, expired);"),
        BAN_REMOVE("Ban_Remove", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        BAN_GET("Ban_Get", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;");
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

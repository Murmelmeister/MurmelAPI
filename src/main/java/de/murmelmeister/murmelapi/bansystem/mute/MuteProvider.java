package de.murmelmeister.murmelapi.bansystem.mute;

import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.log.LogProvider;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.bansystem.reason.ReasonProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class MuteProvider implements Mute {
    private final Reason reason;
    private final Log log;

    public MuteProvider() {
        this.reason = new ReasonProvider("Mute_Reason");
        this.log = new LogProvider("Mute_Log", reason);
        String tableName = "Mute_List";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "UserID INT, ExpiredTime BIGINT");
    }

    @Override
    public void mute(int userId, int creatorId, int reasonId, long time) {
        int logId = log.addLog(userId, creatorId, reasonId, time);
        Database.callUpdate(Procedure.MUTE_ADD.getName(), userId, log.getExpiredTime(logId));
    }

    @Override
    public void unmute(int userId) {
        Database.callUpdate(Procedure.MUTE_REMOVE.getName(), userId);
    }

    @Override
    public long getExpiredTime(int userId) {
        return Database.callQuery(-2L, "ExpiredTime", long.class, Procedure.MUTE_GET.getName(), userId);
    }

    @Override
    public String getExpiredDate(int userId) {
        long time = getExpiredTime(userId);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public boolean isMuted(int userId) {
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
        MUTE_ADD("Mute_Add", "uid INT, expired BIGINT", "INSERT INTO [TABLE] VALUES (uid, expired);"),
        MUTE_REMOVE("Mute_Remove", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        MUTE_GET("Mute_Get", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
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
            for (Procedure procedure : VALUES) Database.update(procedure.getQuery(tableName));
        }
    }
}

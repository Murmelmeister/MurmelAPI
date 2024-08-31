package de.murmelmeister.murmelapi.bansystem.mute;

import de.murmelmeister.murmelapi.bansystem.log.Log;
import de.murmelmeister.murmelapi.bansystem.log.LogProvider;
import de.murmelmeister.murmelapi.bansystem.reason.Reason;
import de.murmelmeister.murmelapi.bansystem.reason.ReasonProvider;
import de.murmelmeister.murmelapi.utils.Database;

public final class MuteProvider implements Mute {
    private final String tableName = "Mute_List";
    private final Reason reason;
    private final Log log;

    public MuteProvider() {
        this.reason = new ReasonProvider("Mute_Reason");
        this.log = new LogProvider("Mute_Log", reason);
        createTable();
        Procedure.loadAll(tableName);
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (UserID INT, ExpiredTime BIGINT(255))", tableName);
    }

    @Override
    public void mute(int userId, int creatorId, int reasonId, long time) {
        int logId = log.addLog(userId, creatorId, reasonId, time);
        Database.updateCall(Procedure.PROCEDURE_MUTE_ADD.getName(), userId, log.getExpiredTime(logId));
    }

    @Override
    public void unmute(int userId) {
        Database.updateCall(Procedure.PROCEDURE_MUTE_REMOVE.getName(), userId);
    }

    @Override
    public long getExpiredTime(int userId) {
        return Database.getLongCall(-2, "ExpiredTime", Procedure.PROCEDURE_MUTE_GET.getName(), userId);
    }

    @Override
    public boolean isMuted(int userId) {
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
        PROCEDURE_MUTE_ADD("Mute_Add", "uid INT, expired BIGINT(255)", "INSERT INTO [TABLE] VALUES (uid, expired);"),
        PROCEDURE_MUTE_REMOVE("Mute_Remove", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        PROCEDURE_MUTE_GET("Mute_Get", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
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

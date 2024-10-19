package de.murmelmeister.murmelapi.time;

import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;

public final class JoinLoggerProvider implements JoinLogger {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public JoinLoggerProvider() {
        String tableName = "JoinLogger";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "TimeID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, JoinDate BIGINT");
    }

    @Override
    public void createJoinDate(int userId) {
        Database.callUpdate(Procedure.JOIN_LOGGER_INSERT.getName(), userId, System.currentTimeMillis());
    }

    @Override
    public void deleteUser(int userId) {
        Database.callUpdate(Procedure.JOIN_LOGGER_DELETE_USER.getName(), userId);
    }

    @Override
    public List<Integer> getTimeIds(int userId) {
        return Database.callQueryList("TimeID", int.class, Procedure.JOIN_LOGGER_GET_USER.getName(), userId);
    }

    @Override
    public long getJoinTime(int timeId, int userId) {
        return Database.callQuery(-1, "JoinDate", int.class, Procedure.JOIN_LOGGER_GET_TIME.getName(), timeId, userId);
    }

    @Override
    public String getJoinDate(int timeId, int userId) {
        return dateFormat.format(getJoinTime(timeId, userId));
    }

    private enum Procedure {
        JOIN_LOGGER_INSERT("JoinLogger_Insert", "uid INT, date BIGINT", "INSERT INTO [TABLE] (UserID, JoinDate) VALUES (uid, date);"),
        JOIN_LOGGER_DELETE_USER("JoinLogger_DeleteUser", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        JOIN_LOGGER_GET_USER("JoinLogger_GetUser", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        JOIN_LOGGER_GET_TIME("JoinLogger_GetTime", "tid INT, uid INT", "SELECT * FROM [TABLE] WHERE TimeID=tid AND UserID=uid;");
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

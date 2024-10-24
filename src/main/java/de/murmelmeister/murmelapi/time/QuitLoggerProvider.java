package de.murmelmeister.murmelapi.time;

import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;

public final class QuitLoggerProvider implements QuitLogger {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public QuitLoggerProvider() {
        String tableName = "QuitLogger";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "TimeID INT PRIMARY KEY AUTO_INCREMENT, UserID INT, QuitDate BIGINT");
    }

    @Override
    public int createQuitDate(int userId) {
        return Database.callUpdate(-1, "id", int.class, Procedure.QUIT_LOGGER_INSERT.getName(), userId, System.currentTimeMillis());
    }

    @Override
    public void deleteUser(int userId) {
        Database.callUpdate(Procedure.QUIT_LOGGER_DELETE_USER.getName(), userId);
    }

    @Override
    public List<Integer> getTimeIds(int userId) {
        return Database.callQueryList("TimeID", int.class, Procedure.QUIT_LOGGER_GET_USER.getName(), userId);
    }

    @Override
    public long getQuitTime(int timeId, int userId) {
        return Database.callQuery(-1L, "QuitDate", long.class, Procedure.QUIT_LOGGER_GET_TIME.getName(), timeId, userId);
    }

    @Override
    public String getQuitDate(int timeId, int userId) {
        return dateFormat.format(getQuitTime(timeId, userId));
    }

    private enum Procedure {
        QUIT_LOGGER_INSERT("QuitLogger_Insert", "uid INT, qDate BIGINT", "INSERT INTO [TABLE] (UserID, QuitDate) VALUES (uid, qDate); SELECT LAST_INSERT_ID() AS id;"),
        QUIT_LOGGER_DELETE_USER("QuitLogger_DeleteUser", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        QUIT_LOGGER_GET_USER("QuitLogger_GetUser", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        QUIT_LOGGER_GET_TIME("QuitLogger_GetTime", "tid INT, uid INT", "SELECT * FROM [TABLE] WHERE TimeID=tid AND UserID=uid;");
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

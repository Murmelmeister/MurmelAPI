package de.murmelmeister.murmelapi.time;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.util.EnumMap;
import java.util.Map;

public final class PlayTimeProvider implements PlayTime {
    public PlayTimeProvider(User user) {
        String tableName = "PlayTime";
        createTable(tableName);
        Procedure.loadAll(tableName);
        loadTables(user);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "UserID INT PRIMARY KEY, Seconds INT");
    }

    @Override
    public boolean existsUser(int userId) {
        return Database.callExists(Procedure.PLAY_TIME_USER_ID.getName(), userId);
    }

    @Override
    public void createUser(int userId) {
        if (existsUser(userId)) return;
        Database.callUpdate(Procedure.PLAY_TIME_INSERT.getName(), userId, 0);
    }

    @Override
    public void deleteUser(int userId) {
        Database.callUpdate(Procedure.PLAY_TIME_DELETE.getName(), userId);
    }

    @Override
    public int getTime(int userId) {
        return Database.callQuery(-1, "Seconds", int.class, Procedure.PLAY_TIME_USER_ID.getName(), userId);
    }

    @Override
    public void setTime(int userId, int time) {
        Database.callUpdate(Procedure.PLAY_TIME_UPDATE.getName(), userId, time);
    }

    @Override
    public void addTime(int userId) {
        int current = getTime(userId);
        ++current;
        setTime(userId, current);
    }

    @Override
    public void addTime(int userId, PlayTimeType type, int time) {
        int current = getTime(userId);
        current = current + type.toSeconds(time);
        setTime(userId, current);
    }

    @Override
    public void removeTime(int userId) {
        int current = getTime(userId);
        --current;
        setTime(userId, current);
    }

    @Override
    public void removeTime(int userId, PlayTimeType type, int time) {
        int current = getTime(userId);
        current = current - type.toSeconds(time);
        setTime(userId, current);
    }

    @Override
    public void resetTime(int userId) {
        setTime(userId, 0);
    }

    @Override
    public Map<PlayTimeType, Integer> calculatePlayTime(int userId) {
        int totalSeconds = getTime(userId);
        Map<PlayTimeType, Integer> result = new EnumMap<>(PlayTimeType.class);

        for (PlayTimeType type : PlayTimeType.VALUES) {
            int time = type.fromSeconds(totalSeconds);
            totalSeconds -= type.toSeconds(time);
            result.put(type, time);
        }

        return result;
    }

    private void loadTables(User user) {
        for (int userId : user.getIds())
            createUser(userId);
    }

    private enum Procedure {
        PLAY_TIME_USER_ID("PlayTime_UserID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        PLAY_TIME_INSERT("PlayTime_Insert", "uid INT, sec INT", "INSERT INTO [TABLE] VALUES (uid, sec);"),
        PLAY_TIME_DELETE("PlayTime_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        PLAY_TIME_UPDATE("PlayTime_Update", "uid INT, sec INT", "UPDATE [TABLE] SET Seconds=sec WHERE UserID=uid;");
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

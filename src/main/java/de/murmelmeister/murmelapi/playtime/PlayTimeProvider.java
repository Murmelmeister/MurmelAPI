package de.murmelmeister.murmelapi.playtime;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

public final class PlayTimeProvider implements PlayTime {
    public PlayTimeProvider(User user) {
        String tableName = "PlayTime";
        createTable(tableName);
        Procedure.loadAll(tableName);
        loadTables(user);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "UserID INT PRIMARY KEY, Seconds BIGINT(255), Minutes BIGINT(255), Hours BIGINT(255), Days BIGINT(255), Years BIGINT(255)");
    }

    @Override
    public boolean existsUser(int userId) {
        return Database.callExists(Procedure.PLAY_TIME_USER_ID.getName(), userId);
    }

    @Override
    public void createUser(int userId) {
        if (existsUser(userId)) return;
        Database.callUpdate(Procedure.PLAY_TIME_INSERT.getName(), userId, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public void deleteUser(int userId) {
        Database.callUpdate(Procedure.PLAY_TIME_DELETE.getName(), userId);
    }

    @Override
    public long getTime(int userId, PlayTimeType type) {
        String value = switch (type) {
            case SECONDS -> PlayTimeType.SECONDS.getName();
            case MINUTES -> PlayTimeType.MINUTES.getName();
            case HOURS -> PlayTimeType.HOURS.getName();
            case DAYS -> PlayTimeType.DAYS.getName();
            case YEARS -> PlayTimeType.YEARS.getName();
        };
        return Database.callQuery(-1L, value, long.class, Procedure.PLAY_TIME_USER_ID.getName(), userId);
    }

    @Override
    public void setTime(int userId, PlayTimeType type, long time) {
        String name = switch (type) {
            case SECONDS -> Procedure.PLAY_TIME_UPDATE_SECONDS.getName();
            case MINUTES -> Procedure.PLAY_TIME_UPDATE_MINUTES.getName();
            case HOURS -> Procedure.PLAY_TIME_UPDATE_HOURS.getName();
            case DAYS -> Procedure.PLAY_TIME_UPDATE_DAYS.getName();
            case YEARS -> Procedure.PLAY_TIME_UPDATE_YEARS.getName();
        };
        Database.callUpdate(name, userId, time);
    }

    @Override
    public void addTime(int userId, PlayTimeType type) {
        long current = getTime(userId, type);
        ++current;
        setTime(userId, type, current);
    }

    @Override
    public void addTime(int userId, PlayTimeType type, long time) {
        long current = getTime(userId, type);
        current = current + time;
        setTime(userId, type, current);
    }

    @Override
    public void removeTime(int userId, PlayTimeType type) {
        long current = getTime(userId, type);
        --current;
        setTime(userId, type, current);
    }

    @Override
    public void removeTime(int userId, PlayTimeType type, long time) {
        long current = getTime(userId, type);
        current = current - time;
        setTime(userId, type, current);
    }

    @Override
    public void resetTime(int userId, PlayTimeType type) {
        setTime(userId, type, 0L);
    }

    @Override
    public void timer(int userId) {
        long seconds = getTime(userId, PlayTimeType.SECONDS);
        long minutes = getTime(userId, PlayTimeType.MINUTES);
        long hours = getTime(userId, PlayTimeType.HOURS);
        long days = getTime(userId, PlayTimeType.DAYS);

        addTime(userId, PlayTimeType.SECONDS);
        if (seconds >= 59) {
            resetTime(userId, PlayTimeType.SECONDS);
            addTime(userId, PlayTimeType.MINUTES);
        } else if (minutes >= 59) {
            resetTime(userId, PlayTimeType.MINUTES);
            addTime(userId, PlayTimeType.HOURS);
        } else if (hours >= 24) {
            resetTime(userId, PlayTimeType.HOURS);
            addTime(userId, PlayTimeType.DAYS);
        } else if (days >= 365) {
            resetTime(userId, PlayTimeType.DAYS);
            addTime(userId, PlayTimeType.YEARS);
        }
    }

    private void loadTables(User user) {
        for (int userId : user.getIds())
            createUser(userId);
    }

    private enum Procedure {
        PLAY_TIME_USER_ID("PlayTime_UserID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        PLAY_TIME_INSERT("PlayTime_Insert", "uid INT, sec BIGINT(255), min BIGINT(255), hour BIGINT(255), day BIGINT(255), year BIGINT(255)", "INSERT INTO [TABLE] VALUES (uid, sec, min, hour, day, year);"),
        PLAY_TIME_DELETE("PlayTime_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        PLAY_TIME_UPDATE_SECONDS("PlayTime_Update_Seconds", "uid INT, sec BIGINT(255)", "UPDATE [TABLE] SET Seconds=sec WHERE UserID=uid;"),
        PLAY_TIME_UPDATE_MINUTES("PlayTime_Update_Minutes", "uid INT, min BIGINT(255)", "UPDATE [TABLE] SET Minutes=min WHERE UserID=uid;"),
        PLAY_TIME_UPDATE_HOURS("PlayTime_Update_Hours", "uid INT, hour BIGINT(255)", "UPDATE [TABLE] SET Hours=hour WHERE UserID=uid;"),
        PLAY_TIME_UPDATE_DAYS("PlayTime_Update_Days", "uid INT, day BIGINT(255)", "UPDATE [TABLE] SET Days=day WHERE UserID=uid;"),
        PLAY_TIME_UPDATE_YEARS("PlayTime_Update_Years", "uid INT, year BIGINT(255)", "UPDATE [TABLE] SET Years=year WHERE UserID=uid;");
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

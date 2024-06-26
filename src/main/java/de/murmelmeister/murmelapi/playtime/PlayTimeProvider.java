package de.murmelmeister.murmelapi.playtime;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

public final class PlayTimeProvider implements PlayTime {
    private static final String TABLE_NAME = "PlayTime";

    public PlayTimeProvider(User user) {
        createTable();
        Procedure.loadAll();
        loadTables(user);
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (UserID INT PRIMARY KEY, Seconds BIGINT(255), Minutes BIGINT(255), Hours BIGINT(255), Days BIGINT(255), Years BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsUser(int userId) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_USER_ID.getName(), userId);
    }

    @Override
    public void createUser(int userId) {
        if (existsUser(userId)) return;
        Database.update("CALL %s('%s','%s','%s','%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), userId, 0L, 0L, 0L, 0L, 0L);
    }

    @Override
    public void deleteUser(int userId) {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), userId);
    }

    @Override
    public long getTime(int userId, PlayTimeType type) {
        var name = switch (type) {
            case SECONDS -> PlayTimeType.SECONDS.getName();
            case MINUTES -> PlayTimeType.MINUTES.getName();
            case HOURS -> PlayTimeType.HOURS.getName();
            case DAYS -> PlayTimeType.DAYS.getName();
            case YEARS -> PlayTimeType.YEARS.getName();
        };
        return Database.getInt(-1, name, "CALL %s('%s')", Procedure.PROCEDURE_USER_ID.getName(), userId);
    }

    @Override
    public void setTime(int userId, PlayTimeType type, long time) {
        var name = switch (type) {
            case SECONDS -> Procedure.PROCEDURE_UPDATE_SECONDS.getName();
            case MINUTES -> Procedure.PROCEDURE_UPDATE_MINUTES.getName();
            case HOURS -> Procedure.PROCEDURE_UPDATE_HOURS.getName();
            case DAYS -> Procedure.PROCEDURE_UPDATE_DAYS.getName();
            case YEARS -> Procedure.PROCEDURE_UPDATE_YEARS.getName();
        };
        Database.update("CALL %s('%s','%s')", name, userId, time);
    }

    @Override
    public void addTime(int userId, PlayTimeType type) {
        var current = getTime(userId, type);
        ++current;
        setTime(userId, type, current);
    }

    @Override
    public void addTime(int userId, PlayTimeType type, long time) {
        var current = getTime(userId, type);
        current = current + time;
        setTime(userId, type, current);
    }

    @Override
    public void removeTime(int userId, PlayTimeType type) {
        var current = getTime(userId, type);
        --current;
        setTime(userId, type, current);
    }

    @Override
    public void removeTime(int userId, PlayTimeType type, long time) {
        var current = getTime(userId, type);
        current = current - time;
        setTime(userId, type, current);
    }

    @Override
    public void resetTime(int userId, PlayTimeType type) {
        setTime(userId, type, 0L);
    }

    @Override
    public void timer(int userId) {
        var seconds = getTime(userId, PlayTimeType.SECONDS);
        var minutes = getTime(userId, PlayTimeType.MINUTES);
        var hours = getTime(userId, PlayTimeType.HOURS);
        var days = getTime(userId, PlayTimeType.DAYS);

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
        for (var userId : user.getIds())
            createUser(userId);
    }

    private enum Procedure {
        PROCEDURE_USER_ID("PlayTime_UserID", Database.getProcedureQuery("PlayTime_UserID", "uid INT", "SELECT * FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_INSERT("PlayTime_Insert", Database.getProcedureQuery("PlayTime_Insert", "uid INT, sec BIGINT(255), min BIGINT(255), hour BIGINT(255), day BIGINT(255), year BIGINT(255)", "INSERT INTO %s VALUES (uid, sec, min, hour, day, year);", TABLE_NAME)),
        PROCEDURE_DELETE("PlayTime_Delete", Database.getProcedureQuery("PlayTime_Delete", "uid INT", "DELETE FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_SECONDS("PlayTime_Update_Seconds", Database.getProcedureQuery("PlayTime_Update_Seconds", "uid INT, sec BIGINT(255)", "UPDATE %s SET Seconds=sec WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_MINUTES("PlayTime_Update_Minutes", Database.getProcedureQuery("PlayTime_Update_Minutes", "uid INT, min BIGINT(255)", "UPDATE %s SET Minutes=min WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_HOURS("PlayTime_Update_Hours", Database.getProcedureQuery("PlayTime_Update_Hours", "uid INT, hour BIGINT(255)", "UPDATE %s SET Hours=hour WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_DAYS("PlayTime_Update_Days", Database.getProcedureQuery("PlayTime_Update_Days", "uid INT, day BIGINT(255)", "UPDATE %s SET Days=day WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_YEARS("PlayTime_Update_Years", Database.getProcedureQuery("PlayTime_Update_Years", "uid INT, year BIGINT(255)", "UPDATE %s SET Years=year WHERE UserID=uid;", TABLE_NAME));
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String query) {
            this.name = name;
            this.query = query;
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public static void loadAll() {
            for (var procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

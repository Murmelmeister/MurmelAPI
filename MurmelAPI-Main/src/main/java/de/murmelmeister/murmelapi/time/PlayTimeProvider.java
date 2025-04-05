package de.murmelmeister.murmelapi.time;

import de.murmelmeister.murmelapi.database.Database;

public final class PlayTimeProvider implements PlayTime {
    private static final String TABLE_NAME = "PlayTime";

    private final Database database;

    public PlayTimeProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT PRIMARY KEY, FOREIGN KEY (UserID) REFERENCES Users(ID), Seconds INT");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsUser(int userId) {
        return database.existsCallable(Procedure.GET_SECONDS.getName(), userId);
    }

    @Override
    public void createUser(int userId) {
        database.updateCallable(Procedure.CREATE.getName(), userId, 0);
    }

    @Override
    public void deleteUser(int userId) {
        database.updateCallable(Procedure.DELETE.getName(), userId);
    }

    @Override
    public int getTime(int userId) {
        return database.queryCallable(Procedure.GET_SECONDS.getName(), -1, resultSet -> resultSet.getInt("Seconds"), userId);
    }

    @Override
    public void setTime(int userId, int time) {
        database.updateCallable(Procedure.UPDATE_SECONDS.getName(), time, userId);
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

    private enum Procedure {
        CREATE("PlayTime_Create", "uid INT, sec INT", "INSERT INTO [TABLE] VALUES (uid, sec);"),
        DELETE("PlayTime_Delete", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_SECONDS("PlayTime_GetSeconds", "uid INT", "SELECT Seconds FROM [TABLE] WHERE UserID=uid;"),
        UPDATE_SECONDS("PlayTime_UpdateSeconds", "sec INT, uid INT", "UPDATE [TABLE] SET Seconds=sec WHERE UserID=uid;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES) database.update(procedure.getQuery());
        }
    }
}

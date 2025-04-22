package de.murmelmeister.murmelapi.time;


import de.murmelmeister.murmelapi.database.Database;

/**
 * The PlayTimeProvider class provides methods to manage playtime data in the database.
 * It implements the PlayTime interface and uses stored procedures for database operations.
 */
public final class PlayTimeProvider implements PlayTime {
    private static final String TABLE_NAME = "playtime";

    private final Database database;

    public PlayTimeProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "userId INT PRIMARY KEY, seconds INT DEFAULT 0, " +
                                         "FOREIGN KEY (userId) REFERENCES users(id)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsUser(int userId) {
        return userId > 0 && database.existsCallable(Procedure.GET_DATA.getName(), userId);
    }

    @Override
    public int createUser(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.CREATE.getName(), userId);
    }

    @Override
    public int deleteUser(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), userId);
    }

    @Override
    public int getTime(int userId) {
        if (userId < 1) return -1;
        return database.queryCallable(Procedure.GET_DATA.getName(), -1, resultSet -> resultSet.getInt("seconds"), userId);
    }

    @Override
    public void updateTime(int userId, int seconds) {
        if (userId < 1) return;
        database.updateCallable(Procedure.UPDATE_SECONDS.getName(), userId, seconds);
    }

    @Override
    public void addTime(int userId) {
        if (userId < 1) return;
        int currentTime = getTime(userId);
        ++currentTime;
        updateTime(userId, currentTime);
    }

    private enum Procedure {
        CREATE("playtime_create", "p_userId INT", "INSERT INTO [TABLE] (userId) VALUES (p_userId);"),
        DELETE("playtime_delete", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA("playtime_getData", "p_userId INT", "SELECT * FROM [TABLE] WHERE userId=p_userId;"),
        UPDATE_SECONDS("playtime_updateSeconds", "p_userId INT, p_seconds INT",
                "UPDATE [TABLE] SET seconds=p_seconds WHERE userId=p_userId;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}

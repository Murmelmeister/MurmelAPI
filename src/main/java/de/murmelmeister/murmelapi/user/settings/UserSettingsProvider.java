package de.murmelmeister.murmelapi.user.settings;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class UserSettingsProvider implements UserSettings {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public UserSettingsProvider(User user) {
        String tableName = "UserSettings";
        createTable(tableName);
        Procedure.loadAll(tableName);
        loadTablesIfNotCreated(user);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "ID INT PRIMARY KEY, FirstJoin BIGINT, Online BOOL");
    }

    @Override
    public boolean existsUser(int id) {
        return Database.callExists(Procedure.USER_SETTINGS_ID.getName(), id);
    }

    @Override
    public void createUser(int id) {
        if (existsUser(id)) return;
        Database.callUpdate(Procedure.USER_SETTINGS_INSERT.getName(), id, System.currentTimeMillis(), 0);
    }

    @Override
    public void deleteUser(int id) {
        Database.callUpdate(Procedure.USER_SETTINGS_DELETE.getName(), id);
    }

    @Override
    public long getFirstJoinTime(int id) {
        return Database.callQuery(-1L, "FirstJoin", long.class, Procedure.USER_SETTINGS_ID.getName(), id);
    }

    @Override
    public String getFirstJoinDate(int id) {
        return dateFormat.format(getFirstJoinTime(id));
    }

    @Override
    public void setOnline(int id, boolean isOnline) {
        Database.callUpdate(Procedure.USER_SETTINGS_UPDATE_ONLINE.getName(), id, isOnline ? (byte) 1 : (byte) 0);
    }

    @Override
    public boolean isOnline(int id) {
        return Database.callQuery((byte) 0, "Online", byte.class, Procedure.USER_SETTINGS_ID.getName(), id) == 1;
    }

    private void loadTablesIfNotCreated(User user) {
        for (int userId : user.getIds())
            createUser(userId);
    }

    private enum Procedure {
        USER_SETTINGS_ID("UserSettings_ID", "uid INT", "SELECT * FROM [TABLE] WHERE ID=uid;"),
        USER_SETTINGS_INSERT("UserSettings_Insert", "uid INT, first BIGINT, isOnline BOOL", "INSERT INTO [TABLE] VALUES (uid, first, isOnline);"),
        USER_SETTINGS_DELETE("UserSettings_Delete", "uid INT", "DELETE FROM [TABLE] WHERE ID=uid;"),
        USER_SETTINGS_UPDATE_ONLINE("UserSettings_UpdateOnline", "uid INT, isOnline BOOL", "UPDATE [TABLE] SET Online=isOnline WHERE ID=uid;");
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

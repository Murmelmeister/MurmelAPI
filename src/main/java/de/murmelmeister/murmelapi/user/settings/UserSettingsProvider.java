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
        Database.createTable(tableName, "ID INT PRIMARY KEY, FirstJoin BIGINT(255), LastQuit BIGINT(255), IsOnline BOOL");
    }

    @Override
    public boolean existsUser(int id) {
        return Database.callExists(Procedure.USER_SETTINGS_ID.getName(), id);
    }

    @Override
    public void createUser(int id) {
        if (existsUser(id)) return;
        Database.callUpdate(Procedure.USER_SETTINGS_INSERT.getName(), id, System.currentTimeMillis(), System.currentTimeMillis(), 0);
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
    public void setLastQuitTime(int id, long time) {
        Database.callUpdate(Procedure.USER_SETTINGS_UPDATE_LAST_QUIT.getName(), id, time);
    }

    @Override
    public long getLastQuitTime(int id) {
        return Database.callQuery(-1L, "LastQuit", long.class, Procedure.USER_SETTINGS_ID.getName(), id);
    }

    @Override
    public String getLastQuitDate(int id) {
        return dateFormat.format(getLastQuitTime(id));
    }

    @Override
    public void setOnline(int id, int online) {
        Database.callUpdate(Procedure.USER_SETTINGS_UPDATE_ONLINE.getName(), id, online);
    }

    @Override
    public int getOnline(int id) {
        return Database.callQuery(0, "IsOnline", int.class, Procedure.USER_SETTINGS_ID.getName(), id);
    }

    private void loadTablesIfNotCreated(User user) {
        for (int userId : user.getIds())
            createUser(userId);
    }

    private enum Procedure {
        USER_SETTINGS_ID("UserSettings_ID", "uid INT", "SELECT * FROM [TABLE] WHERE ID=uid;"),
        USER_SETTINGS_INSERT("UserSettings_Insert", "uid INT, first BIGINT(255), last BIGINT(255), online BOOL", "INSERT INTO [TABLE] VALUES (uid, first, last, online);"),
        USER_SETTINGS_DELETE("UserSettings_Delete", "uid INT", "DELETE FROM [TABLE] WHERE ID=uid;"),
        USER_SETTINGS_UPDATE_ONLINE("UserSettings_UpdateOnline", "uid INT, online BOOL", "UPDATE [TABLE] SET IsOnline=online WHERE ID=uid;"),
        USER_SETTINGS_UPDATE_LAST_QUIT("UserSettings_UpdateLastQuit", "uid INT, last BIGINT(255)", "UPDATE [TABLE] SET LastQuit=last WHERE ID=uid;");
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

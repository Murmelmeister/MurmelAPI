package de.murmelmeister.murmelapi.user.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;

public final class UserSettingsProvider implements UserSettings {
    private static final String TABLE_NAME = "UserSettings";

    public UserSettingsProvider() {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY, FirstJoin BIGINT(255), LastQuit BIGINT(255), IsOnline BOOL)", TABLE_NAME);
    }

    @Override
    public boolean existsUser(int id) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public void createUser(int id) {
        if (existsUser(id)) return;
        Database.update("CALL %s('%s','%s','%s','%s')", Procedure.PROCEDURE_INSERT.getName(), id, System.currentTimeMillis(), System.currentTimeMillis(), 0);
    }

    @Override
    public void deleteUser(int id) {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public long getFirstJoinTime(int id) {
        return Database.getLong(-1, "FirstJoin", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public String getFirstJoinDate(int id) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getFirstJoinTime(id));
    }

    @Override
    public void setLastQuitTime(int id, long time) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_UPDATE_LAST_QUIT.getName(), id, time);
    }

    @Override
    public long getLastQuitTime(int id) {
        return Database.getLong(-1, "LastQuit", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public String getLastQuitDate(int id) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getLastQuitTime(id));
    }

    @Override
    public void setOnline(int id, int online) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_UPDATE_ONLINE.getName(), id, online);
    }

    @Override
    public int getOnline(int id) {
        return Database.getInt(0, "IsOnline", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    private enum Procedure {
        PROCEDURE_ID("UserSettings_ID", Database.getProcedureQuery("UserSettings_ID", "uid INT", "SELECT * FROM %s WHERE ID=uid;", TABLE_NAME)),
        PROCEDURE_INSERT("UserSettings_Insert", Database.getProcedureQuery("UserSettings_Insert", "uid INT, first BIGINT(255), last BIGINT(255), online BOOL", "INSERT INTO %s VALUES (uid, first, last, online);", TABLE_NAME)),
        PROCEDURE_DELETE("UserSettings_Delete", Database.getProcedureQuery("UserSettings_Delete", "uid INT", "DELETE FROM %s WHERE ID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_ONLINE("UserSettings_UpdateOnline", Database.getProcedureQuery("UserSettings_UpdateOnline", "uid INT, online BOOL", "UPDATE %s SET IsOnline=online WHERE ID=uid;", TABLE_NAME)),
        PROCEDURE_UPDATE_LAST_QUIT("UserSettings_UpdateLastQuit", Database.getProcedureQuery("UserSettings_UpdateLastQuit", "uid INT, last BIGINT(255)", "UPDATE %s SET LastQuit=last WHERE ID=uid;", TABLE_NAME));
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

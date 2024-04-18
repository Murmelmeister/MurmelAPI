package de.murmelmeister.murmelapi.user.settings;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class UserSettingsProvider implements UserSettings {
    private static final String TABLE_NAME = "UserSettings";

    public UserSettingsProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY, FOREIGN KEY (ID) REFERENCES User(ID), FirstJoin BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsUser(int id) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public void createUser(int id) throws SQLException {
        if (existsUser(id)) return;
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_INSERT.getName(), id, System.currentTimeMillis());
    }

    @Override
    public void deleteUser(int id) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), checkArgumentSQL(id));
    }

    @Override
    public long getFirstJoinTime(int id) throws SQLException {
        return Database.getLong(-1, "FirstJoin", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public String getFirstJoinDate(int id) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getFirstJoinTime(checkArgumentSQL(id)));
    }

    private enum Procedure {
        PROCEDURE_ID("UserSettings_ID", String.format("""
                CREATE PROCEDURE IF NOT EXISTS UserSettings_ID(uid INT)
                BEGIN
                    SELECT * FROM %s WHERE ID=uid;
                END;""", TABLE_NAME)),
        PROCEDURE_INSERT("UserSettings_Insert", String.format("""
                CREATE PROCEDURE IF NOT EXISTS UserSettings_Insert(uid INT, time BIGINT(255))
                BEGIN
                    INSERT INTO %s VALUES (uid, time);
                END;""", TABLE_NAME)),
        PROCEDURE_DELETE("UserSettings_Delete", String.format("""
                CREATE PROCEDURE IF NOT EXISTS UserSettings_Delete(uid INT)
                BEGIN
                    DELETE FROM %s WHERE ID=uid;
                END;""", TABLE_NAME));
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

        public static void loadAll() throws SQLException {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

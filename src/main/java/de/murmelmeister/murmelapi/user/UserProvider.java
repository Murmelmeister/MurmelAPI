package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class UserProvider implements User {
    private static final String TABLE_NAME = "User";

    public UserProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY AUTO_INCREMENT, UUID VARCHAR(36), Username VARCHAR(100))", TABLE_NAME);
    }

    @Override
    public boolean existsUser(UUID uuid) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_UNIQUE_ID.getName(), checkArgumentSQL(uuid));
    }

    @Override
    public boolean existsUser(String username) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_USERNAME.getName(), checkArgumentSQL(username));
    }

    @Override
    public void createNewUser(UUID uuid, String username) throws SQLException {
        if (existsUser(uuid)) return;
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_INSERT.getName(), uuid, username);
    }

    @Override
    public void deleteUser(UUID uuid) throws SQLException {
        var id = getId(checkArgumentSQL(uuid));
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public int getId(UUID uuid) throws SQLException {
        return Database.getInt(-1, "ID", "CALL %s('%s')", Procedure.PROCEDURE_UNIQUE_ID.getName(), checkArgumentSQL(uuid));
    }

    @Override
    public int getId(String username) throws SQLException {
        return Database.getInt(-1, "ID", "CALL %s('%s')", Procedure.PROCEDURE_USERNAME.getName(), checkArgumentSQL(username));
    }

    @Override
    public UUID getUniqueId(String username) throws SQLException {
        var id = getId(checkArgumentSQL(username));
        return Database.getUniqueId(null, "UUID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public String getUsername(UUID uuid) throws SQLException {
        var id = getId(checkArgumentSQL(uuid));
        return Database.getString(null, "Username", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public void rename(UUID uuid, String newName) throws SQLException {
        var id = getId(checkArgumentSQL(uuid));
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME.getName(), id, newName);
    }

    @Override
    public List<UUID> getUniqueIds() throws SQLException {
        return Database.getUniqueIdList(null, "UUID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<String> getUsernames() throws SQLException {
        return Database.getStringList(null, "Username", "CALL %s", Procedure.PROCEDURE_USERNAME.getName());
    }

    @Override
    public List<Integer> getIds() throws SQLException {
        return Database.getIntList(null, "ID", "CALL %s", Procedure.PROCEDURE_ID.getName());
    }

    @Override
    public void joinUser(UUID uuid, String username) throws SQLException {
        createNewUser(uuid, username);
        if (!getUsername(uuid).equals(username)) rename(uuid, username);
    }

    private enum Procedure {
        PROCEDURE_UNIQUE_ID("User_UniqueID", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_UniqueID(uid VARCHAR(36))
                BEGIN
                    SELECT * FROM %s WHERE UUID=uid;
                END;""", TABLE_NAME)),
        PROCEDURE_USERNAME("User_Username", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_Username(user VARCHAR(100))
                BEGIN
                    SELECT * FROM %s WHERE Username=user;
                END;""", TABLE_NAME)),
        PROCEDURE_ID("User_ID", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_ID(uid INT)
                BEGIN
                    SELECT * FROM %s WHERE ID=uid;
                END;""", TABLE_NAME)),
        PROCEDURE_ALL("User_All", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_All()
                BEGIN
                    SELECT * FROM %s;
                END;""", TABLE_NAME)),
        PROCEDURE_INSERT("User_Insert", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_Insert(uid VARCHAR(36), user VARCHAR(100))
                BEGIN
                    INSERT INTO %s (UUID, Username) VALUES (uid, user);
                END;""", TABLE_NAME)),
        PROCEDURE_DELETE("User_Delete", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_Delete(uid VARCHAR(36))
                BEGIN
                    DELETE FROM %s WHERE UUID=uid;
                END;""", TABLE_NAME)),
        PROCEDURE_RENAME("User_Rename", String.format("""
                CREATE PROCEDURE IF NOT EXISTS User_Rename(uid INT, user VARCHAR(100))
                BEGIN
                    UPDATE %s SET Username=user WHERE ID=uid;
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

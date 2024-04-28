package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.user.settings.UserSettings;
import de.murmelmeister.murmelapi.user.settings.UserSettingsProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class UserProvider implements User {
    private static final String TABLE_NAME = "User";

    private final UserSettings settings;
    private final UserParent parent;
    private final UserPermission permission;

    public UserProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
        this.settings = new UserSettingsProvider();
        this.parent = new UserParentProvider();
        this.permission = new UserPermissionProvider();
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
        var id = getId(uuid);
        settings.createUser(id);
    }

    @Override
    public void deleteUser(UUID uuid) throws SQLException {
        var id = getId(uuid);
        permission.clearPermission(id);
        parent.clearParent(id);
        settings.deleteUser(id);
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public int getId(UUID uuid) throws SQLException {
        return Database.getInt(-2, "ID", "CALL %s('%s')", Procedure.PROCEDURE_UNIQUE_ID.getName(), uuid);
    }

    @Override
    public int getId(String username) throws SQLException {
        return Database.getInt(-2, "ID", "CALL %s('%s')", Procedure.PROCEDURE_USERNAME.getName(), checkArgumentSQL(username));
    }

    @Override
    public UUID getUniqueId(String username) throws SQLException {
        var id = getId(username);
        return Database.getUniqueId(null, "UUID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public UUID getUniqueId(int id) throws SQLException {
        return Database.getUniqueId(null, "UUID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public String getUsername(UUID uuid) throws SQLException {
        var id = getId(uuid);
        return Database.getString(null, "Username", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public String getUsername(int id) throws SQLException {
        var uid = checkArgumentSQL(id);
        return uid == -1 ? "CONSOLE" : Database.getString(null, "Username", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), uid);
    }

    @Override
    public void rename(UUID uuid, String newName) throws SQLException {
        var id = getId(uuid);
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME.getName(), id, checkArgumentSQL(newName));
    }

    @Override
    public List<UUID> getUniqueIds() throws SQLException {
        return Database.getUniqueIdList(new ArrayList<>(), "UUID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<String> getUsernames() throws SQLException {
        return Database.getStringList(new ArrayList<>(), "Username", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<Integer> getIds() throws SQLException {
        return Database.getIntList(new ArrayList<>(), "ID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public void joinUser(UUID uuid, String username) throws SQLException {
        createNewUser(uuid, username);
        if (!getUsername(uuid).equals(username)) rename(uuid, username);
    }

    @Override
    public void loadExpired() throws SQLException {
        parent.loadExpired(this);
        permission.loadExpired(this);
    }

    @Override
    public UserSettings getSettings() {
        return settings;
    }

    @Override
    public UserParent getParent() {
        return parent;
    }

    @Override
    public UserPermission getPermission() {
        return permission;
    }

    private enum Procedure {
        PROCEDURE_UNIQUE_ID("User_UniqueID", Database.getProcedureQuery("User_UniqueID", "uid VARCHAR(36)", "SELECT * FROM %s WHERE UUID=uid;", TABLE_NAME)),
        PROCEDURE_USERNAME("User_Username", Database.getProcedureQuery("User_Username", "user VARCHAR(100)", "SELECT * FROM %s WHERE Username=user;", TABLE_NAME)),
        PROCEDURE_ID("User_ID", Database.getProcedureQuery("User_ID", "uid INT", "SELECT * FROM %s WHERE ID=uid;", TABLE_NAME)),
        PROCEDURE_ALL("User_All", Database.getProcedureQuery("User_All", "", "SELECT * FROM %s;", TABLE_NAME)),
        PROCEDURE_INSERT("User_Insert", Database.getProcedureQuery("User_Insert", "uid VARCHAR(36), user VARCHAR(100)", "INSERT INTO %s (UUID, Username) VALUES (uid, user);", TABLE_NAME)),
        PROCEDURE_DELETE("User_Delete", Database.getProcedureQuery("User_Delete", "uid VARCHAR(36)", "DELETE FROM %s WHERE UUID=uid;", TABLE_NAME)),
        PROCEDURE_RENAME("User_Rename", Database.getProcedureQuery("User_Rename", "uid INT, user VARCHAR(100)", "UPDATE %s SET Username=user WHERE ID=uid;", TABLE_NAME));
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

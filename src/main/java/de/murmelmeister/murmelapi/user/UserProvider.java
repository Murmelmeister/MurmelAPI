package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.playtime.PlayTimeProvider;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.user.settings.UserSettings;
import de.murmelmeister.murmelapi.user.settings.UserSettingsProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.util.List;
import java.util.UUID;

public final class UserProvider implements User {
    private static final String TABLE_NAME = "User";

    private final UserSettings settings;
    private final UserParent parent;
    private final UserPermission permission;

    private final PlayTime playTime;

    public UserProvider() {
        this.createTable();
        Procedure.loadAll();
        this.settings = new UserSettingsProvider(this);
        this.parent = new UserParentProvider();
        this.permission = new UserPermissionProvider();
        this.playTime = new PlayTimeProvider(this);
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY AUTO_INCREMENT, UUID VARCHAR(36), Username VARCHAR(100))", TABLE_NAME);
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_UNIQUE_ID.getName(), uuid);
    }

    @Override
    public boolean existsUser(String username) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_USERNAME.getName(), username);
    }

    @Override
    public void createNewUser(UUID uuid, String username) {
        if (existsUser(uuid)) return;
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_INSERT.getName(), uuid, username);
        var id = getId(uuid);
        settings.createUser(id);
        playTime.createUser(id);
    }

    @Override
    public void deleteUser(UUID uuid) {
        var id = getId(uuid);
        playTime.deleteUser(id);
        permission.clearPermission(id);
        parent.clearParent(id);
        settings.deleteUser(id);
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public int getId(UUID uuid) {
        return Database.getInt(-2, "ID", "CALL %s('%s')", Procedure.PROCEDURE_UNIQUE_ID.getName(), uuid);
    }

    @Override
    public int getId(String username) {
        return Database.getInt(-2, "ID", "CALL %s('%s')", Procedure.PROCEDURE_USERNAME.getName(), username);
    }

    @Override
    public UUID getUniqueId(String username) {
        var id = getId(username);
        return Database.getUniqueId(null, "UUID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public UUID getUniqueId(int id) {
        if (id == -1) return null;
        return UUID.fromString(Database.getString(null, "UUID", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id));
    }

    @Override
    public String getUsername(UUID uuid) {
        var id = getId(uuid);
        return Database.getString(null, "Username", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public String getUsername(int id) {
        return id == -1 ? "CONSOLE" : Database.getString(null, "Username", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public void rename(UUID uuid, String newName) {
        var id = getId(uuid);
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME.getName(), id, newName);
    }

    @Override
    public List<UUID> getUniqueIds() {
        return Database.getUniqueIdList("UUID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<String> getUsernames() {
        return Database.getStringList("Username", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<Integer> getIds() {
        return Database.getIntList("ID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public void joinUser(UUID uuid, String username) {
        createNewUser(uuid, username);
        if (!getUsername(uuid).equals(username)) rename(uuid, username);
    }

    @Override
    public void loadExpired() {
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

    @Override
    public PlayTime getPlayTime() {
        return playTime;
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

        public static void loadAll() {
            for (var procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

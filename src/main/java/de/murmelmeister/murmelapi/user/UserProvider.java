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
    private final UserSettings settings;
    private final UserParent parent;
    private final UserPermission permission;

    private final PlayTime playTime;

    public UserProvider() {
        String tableName = "User";
        createTable(tableName);
        Procedure.loadAll(tableName);
        this.settings = new UserSettingsProvider(this);
        this.parent = new UserParentProvider();
        this.permission = new UserPermissionProvider();
        this.playTime = new PlayTimeProvider(this);
    }

    private void createTable(String tableName) {
        Database.createTable("ID INT PRIMARY KEY AUTO_INCREMENT, UUID VARCHAR(36), Username VARCHAR(100)", tableName);
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return Database.existsCall(Procedure.USER_UNIQUE_ID.getName(), uuid);
    }

    @Override
    public boolean existsUser(String username) {
        return Database.existsCall(Procedure.USER_USERNAME.getName(), username);
    }

    @Override
    public void createNewUser(UUID uuid, String username) {
        if (existsUser(uuid)) return;
        Database.updateCall(Procedure.USER_INSERT.getName(), uuid, username);
        int id = getId(uuid);
        settings.createUser(id);
        playTime.createUser(id);
    }

    @Override
    public void deleteUser(UUID uuid) {
        int id = getId(uuid);
        playTime.deleteUser(id);
        permission.clearPermission(id);
        parent.clearParent(id);
        settings.deleteUser(id);
        Database.updateCall(Procedure.USER_DELETE.getName(), id);
    }

    @Override
    public int getId(UUID uuid) {
        return Database.getIntCall(-2, "ID", Procedure.USER_UNIQUE_ID.getName(), uuid);
    }

    @Override
    public int getId(String username) {
        return Database.getIntCall(-2, "ID", Procedure.USER_USERNAME.getName(), username);
    }

    @Override
    public UUID getUniqueId(String username) {
        int id = getId(username);
        return Database.getUniqueIdCall(null, "UUID", Procedure.USER_ID.getName(), id);
    }

    @Override
    public UUID getUniqueId(int id) {
        return id == -1 ? null : UUID.fromString(Database.getStringCall(null, "UUID", Procedure.USER_ID.getName(), id));
    }

    @Override
    public String getUsername(UUID uuid) {
        int id = getId(uuid);
        return Database.getStringCall(null, "Username", Procedure.USER_ID.getName(), id);
    }

    @Override
    public String getUsername(int id) {
        return id == -1 ? "CONSOLE" : Database.getStringCall(null, "Username", Procedure.USER_ID.getName(), id);
    }

    @Override
    public void rename(UUID uuid, String newName) {
        int id = getId(uuid);
        Database.updateCall(Procedure.USER_RENAME.getName(), id, newName);
    }

    @Override
    public List<UUID> getUniqueIds() {
        return Database.getUniqueIdListCall("UUID", Procedure.USER_ALL.getName());
    }

    @Override
    public List<String> getUsernames() {
        return Database.getStringListCall("Username", Procedure.USER_ALL.getName());
    }

    @Override
    public List<Integer> getIds() {
        return Database.getIntListCall("ID", Procedure.USER_ALL.getName());
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
        USER_UNIQUE_ID("User_UniqueID", "uid VARCHAR(36)", "SELECT * FROM [TABLE] WHERE UUID=uid;"),
        USER_USERNAME("User_Username", "user VARCHAR(100)", "SELECT * FROM [TABLE] WHERE Username=user;"),
        USER_ID("User_ID", "uid INT", "SELECT * FROM [TABLE] WHERE ID=uid;"),
        USER_ALL("User_All", "", "SELECT * FROM [TABLE];"),
        USER_INSERT("User_Insert", "uid VARCHAR(36), user VARCHAR(100)", "INSERT INTO [TABLE] (UUID, Username) VALUES (uid, user);"),
        USER_DELETE("User_Delete", "uid VARCHAR(36)", "DELETE FROM [TABLE] WHERE UUID=uid;"),
        USER_RENAME("User_Rename", "uid INT, user VARCHAR(100)", "UPDATE [TABLE] SET Username=user WHERE ID=uid;");
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

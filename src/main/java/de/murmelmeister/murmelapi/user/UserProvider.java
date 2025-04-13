package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * UserProvider class to manage users in the database.
 * This class implements the User interface and provides methods to interact with user data.
 */
public final class UserProvider implements User {
    private static final String TABLE_NAME = "users";

    private final Database database;
    private UserParent parent;
    private UserPermission permission;

    public UserProvider(Database database) {
        this.database = database;
        this.parent = getParent();
        this.permission = getPermission();
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "mojangId VARCHAR(36) UNIQUE, " +
                                         "username VARCHAR(16), " +
                                         "firstJoin DATETIME, " +
                                         "isDebugUser BOOLEAN DEFAULT FALSE, " +
                                         "isDebugActive BOOLEAN DEFAULT FALSE");
        database.update("CREATE INDEX IF NOT EXISTS username_index ON " + TABLE_NAME + " (username)");
        Procedure.loadAll(database);
        createConsoleUser(database);
    }

    private static void createConsoleUser(Database database) {
        int id = -1;
        if (database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), id)) return;
        database.updateCallable(Procedure.CREATE_CONSOLE.getName(), id);
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return uuid != null && database.existsCallable(Procedure.GET_ID_BY_MOJANG_ID.getName(), uuid.toString());
    }

    @Override
    public boolean existsUser(String username) {
        return username != null && database.existsCallable(Procedure.GET_ID_BY_USERNAME.getName(), username);
    }

    @Override
    public int createUser(UUID uuid, String username) {
        if (uuid == null || username == null) return 0;
        return database.updateCallable(Procedure.CREATE.getName(), uuid.toString(), username);
    }

    @Override
    public int deleteUser(int id) {
        if (id < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), id);
    }

    @Override
    public int getId(UUID uuid) {
        return uuid == null ? -2 : database.queryCallable(Procedure.GET_ID_BY_MOJANG_ID.getName(), -2, resultSet -> resultSet.getInt("id"), uuid.toString());
    }

    @Override
    public int getId(String username) {
        return username == null ? -2 : database.queryCallable(Procedure.GET_ID_BY_USERNAME.getName(), -2, resultSet -> resultSet.getInt("id"), username);
    }

    @Override
    public UUID getUniqueId(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> UUID.fromString(resultSet.getString("mojangId")), id);
    }

    @Override
    public String getUsername(int id) {
        return id == -1 ? "Console" :
                id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("username"), id);
    }

    @Override
    public int renameUser(int id, String username) {
        if (id < 1 || username == null) return 0;
        return database.updateCallable(Procedure.UPDATE_USERNAME.getName(), id, username);
    }

    @Override
    public List<UUID> getUniqueIds() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), resultSet -> UUID.fromString(resultSet.getString("mojangId")))
                .stream().filter(Objects::nonNull).toList();
    }

    @Override
    public List<String> getUsernames() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), resultSet -> resultSet.getString("username"))
                .stream().filter(Objects::nonNull).toList();
    }

    @Override
    public Timestamp getFirstJoin(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("firstJoin"), id);
    }

    @Override
    public String getFirstJoinDate(int id) {
        Timestamp firstJoin = getFirstJoin(id);
        return firstJoin == null ? null : getDateFormat().format(firstJoin);
    }

    @Override
    public int setFirstJoin(int id, Timestamp firstJoin) {
        if (id < 1 || firstJoin == null) return 0;
        return database.updateCallable(Procedure.UPDATE_FIRST_JOIN.getName(), id, firstJoin);
    }

    @Override
    public boolean isDebugUser(int id) {
        return id > 0 && database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), false, resultSet -> resultSet.getBoolean("isDebugUser"), id);
    }

    @Override
    public int setDebugUser(int id, boolean isDebugUser) {
        if (id < 1) return 0;
        return database.updateCallable(Procedure.UPDATE_DEBUG_USER.getName(), id, isDebugUser);
    }

    @Override
    public boolean isDebugActive(int id) {
        return id > 0 && database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), false, resultSet -> resultSet.getBoolean("isDebugActive"), id);
    }

    @Override
    public int setDebugActive(int id, boolean isDebugActive) {
        if (id < 1) return 0;
        return database.updateCallable(Procedure.UPDATE_DEBUG_ACTIVE.getName(), id, isDebugActive);
    }

    @Override
    public boolean isDebugMode(int id) {
        return isDebugUser(id) && isDebugActive(id);
    }

    @Override
    public int joinUser(UUID uuid, String username) {
        if (uuid == null || username == null) return 0;

        int userId = getId(uuid);

        if (userId == -1) return 0;
        if (userId == -2)
            return createUser(uuid, username);

        String currentUsername = getUsername(userId);
        if (currentUsername == null)
            return renameUser(userId, username);

        if (!currentUsername.equals(username))
            return renameUser(userId, username);
        return 0;
    }

    @Override
    public int loadExpired() {
        int parentRows = parent.loadExpired();
        int permissionRows = permission.loadExpired();
        return parentRows + permissionRows;
    }

    @Override
    public UserParent getParent() {
        if (parent == null)
            parent = new UserParentProvider(database);
        return parent;
    }

    @Override
    public UserPermission getPermission() {
        if (permission == null)
            permission = new UserPermissionProvider(database);
        return permission;
    }

    private enum Procedure {
        CREATE("users_create", "p_mojangId VARCHAR(36), p_username VARCHAR(16)",
                "INSERT INTO [TABLE] (mojangId, username) VALUES (p_mojangId, p_username);"),
        CREATE_CONSOLE("users_createConsole", "p_id INT", "INSERT INTO [TABLE] VALUES (p_id, NULL, NULL, NULL, TRUE, TRUE);"),
        DELETE("users_delete", "p_id INT", "DELETE FROM [TABLE] WHERE id=p_id;"),
        GET_DATA("users_getData", "", "SELECT id, mojangId, username FROM [TABLE];"),
        GET_DATA_BY_ID("users_getDataById", "p_id INT", "SELECT mojangId, username, firstJoin, isDebugUser, isDebugActive FROM [TABLE] WHERE id=p_id;"),
        GET_ID_BY_MOJANG_ID("users_getIdByMojangId", "p_mojangId VARCHAR(36)", "SELECT id FROM [TABLE] WHERE mojangId=p_mojangId;"),
        GET_ID_BY_USERNAME("users_getIdByUsername", "p_username VARCHAR(16)", "SELECT id FROM [TABLE] WHERE username=p_username;"),
        UPDATE_USERNAME("users_updateUsername", "p_id INT, p_username VARCHAR(16)", "UPDATE [TABLE] SET username=p_username WHERE id=p_id;"),
        UPDATE_FIRST_JOIN("users_updateFirstJoin", "p_id INT, p_firstJoin DATETIME", "UPDATE [TABLE] SET firstJoin=p_firstJoin WHERE id=p_id;"),
        UPDATE_DEBUG_USER("users_updateDebugUser", "p_id INT, p_isDebugUser BOOLEAN", "UPDATE [TABLE] SET isDebugUser=p_isDebugUser WHERE id=p_id;"),
        UPDATE_DEBUG_ACTIVE("users_updateDebugActive", "p_id INT, p_isDebugActive BOOLEAN", "UPDATE [TABLE] SET isDebugActive=p_isDebugActive WHERE id=p_id;");
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

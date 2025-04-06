package de.murmelmeister.murmelapi.user;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.utils.CacheManager;
import de.murmelmeister.murmelapi.utils.MojangUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class UserProvider implements User {
    private static final String TABLE_NAME = "Users";

    private final Database database;

    private final CacheManager<String, Integer> usernameCache = new CacheManager<>();
    private final CacheManager<UUID, Integer> uuidCache = new CacheManager<>();

    /*
    TODO: Add a debug list for function they are not used (delete user, create user, etc.)
        -> Permission for debug command is console only or debug user
     */

    private UserParent parent;
    private UserPermission permission;

    public UserProvider(Database database) {
        this.database = database;
        this.parent = getParent();
        this.permission = getPermission();
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "ID INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "MojangID UUID UNIQUE, " +
                                         "Username VARCHAR(100), " +
                                         "FirstJoinTime DATETIME");
        Procedure.loadAll(database);
        createConsoleUser(database);
    }

    private static void createConsoleUser(Database database) {
        int id = -1;
        if (database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), id)) return;
        database.updateCallable(Procedure.CREATE_CONSOLE.getName(), id);
    }

    @Override
    public int createOrGetUser(String username) {
        if (username == null) return -2;
        int userId = getId(username);
        if (userId != -2) return userId;
        try {
            UUID uuid = MojangUtils.getUUID(username);
            createUser(uuid, username);
        } catch (IOException | URISyntaxException e) {
            throw new CompletionException(e);
        }
        return getId(username);
    }

    @Override
    public CompletableFuture<Integer> createOrGetUserAsync(UUID uuid) {
        if (uuid == null) return CompletableFuture.completedFuture(-2);
        return CompletableFuture.supplyAsync(() -> createOrGetUser(uuid));
    }

    @Override
    public int createOrGetUser(UUID uuid) {
        if (uuid == null) return -2;
        int userId = getId(uuid);
        if (userId != -2) return userId;
        try {
            String username = MojangUtils.getUsername(uuid);
            createUser(uuid, username);
        } catch (IOException | URISyntaxException e) {
            throw new CompletionException(e);
        }
        return getId(uuid);
    }

    @Override
    public CompletableFuture<Integer> createOrGetUserAsync(String username) {
        if (username == null) return CompletableFuture.completedFuture(-2);
        return CompletableFuture.supplyAsync(() -> createOrGetUser(username));
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return uuid != null && database.existsCallable(Procedure.GET_ID_BY_UUID.getName(), uuid.toString());
    }

    @Override
    public CompletableFuture<Boolean> existsUserAsync(UUID uuid) {
        return uuid == null ? CompletableFuture.completedFuture(false) : database.existsCallableAsync(Procedure.GET_ID_BY_UUID.getName(), uuid.toString());
    }

    @Override
    public boolean existsUser(String username) {
        return username != null && database.existsCallable(Procedure.GET_ID_BY_USERNAME.getName(), username);
    }

    @Override
    public CompletableFuture<Boolean> existsUserAsync(String username) {
        return username == null ? CompletableFuture.completedFuture(false) : database.existsCallableAsync(Procedure.GET_ID_BY_USERNAME.getName(), username);
    }

    @Override
    public void createUser(UUID uuid, String username) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        database.updateCallable(Procedure.CREATE.getName(), uuid.toString(), username);
    }

    @Override
    public CompletableFuture<Integer> createUserAsync(UUID uuid, String username) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        Objects.requireNonNull(username, "username cannot be null");
        return database.updateCallableAsync(Procedure.CREATE.getName(), uuid.toString(), username);
    }

    @Override
    public void deleteUser(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        String username = getUsername(uuid);
        database.updateCallable(Procedure.DELETE.getName(), uuid.toString());
        if (username != null) usernameCache.remove(username);
        if (uuidCache.get(uuid) != null) uuidCache.remove(uuid);
    }

    @Override
    public CompletableFuture<Integer> deleteUserAsync(UUID uuid) {
        Objects.requireNonNull(uuid, "uuid cannot be null");
        return database.updateCallableAsync(Procedure.DELETE.getName(), uuid.toString()).thenApply(result -> {
            String username = getUsername(uuid);
            if (username != null) usernameCache.remove(username);
            if (uuidCache.get(uuid) != null) uuidCache.remove(uuid);
            return result;
        });
    }

    @Override
    public int getId(UUID uuid) {
        if (uuid == null) return -2;
        Integer cached = uuidCache.get(uuid);
        return cached != null ? cached : database.queryCallable(Procedure.GET_ID_BY_UUID.getName(), -2,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -2) uuidCache.put(uuid, id, 1, TimeUnit.HOURS);
                    return id;
                }, uuid.toString());
    }

    @Override
    public CompletableFuture<Integer> getIdAsync(UUID uuid) {
        if (uuid == null) return CompletableFuture.completedFuture(-2);
        return database.queryCallableAsync(Procedure.GET_ID_BY_UUID.getName(), -2,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -2) uuidCache.put(uuid, id, 1, TimeUnit.HOURS);
                    return id;
                }, uuid.toString());
    }

    @Override
    public int getId(String username) {
        if (username == null) return -2;
        Integer cached = usernameCache.get(username);
        return cached != null ? cached : database.queryCallable(Procedure.GET_ID_BY_USERNAME.getName(), -2,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -2) usernameCache.put(username, id, 1, TimeUnit.HOURS);
                    return id;
                }, username);
    }

    @Override
    public CompletableFuture<Integer> getIdAsync(String username) {
        if (username == null) return CompletableFuture.completedFuture(-2);
        return database.queryCallableAsync(Procedure.GET_ID_BY_USERNAME.getName(), -2,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -2) usernameCache.put(username, id, 1, TimeUnit.HOURS);
                    return id;
                }, username);
    }

    @Override
    public UUID getUniqueId(int userId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String id = resultSet.getString("MojangID");
                    return id == null ? null : UUID.fromString(id);
                }, userId);
    }

    @Override
    public CompletableFuture<UUID> getUniqueIdAsync(int userId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String id = resultSet.getString("MojangID");
                    return id == null ? null : UUID.fromString(id);
                }, userId);
    }

    @Override
    public UUID getUniqueId(String username) {
        if (username == null) return null;
        int userId = getId(username);
        if (userId == -2) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String mojangId = resultSet.getString("MojangID");
                    return mojangId == null ? null : UUID.fromString(mojangId);
                }, userId);
    }

    @Override
    public CompletableFuture<UUID> getUniqueIdAsync(String username) {
        if (username == null) return CompletableFuture.completedFuture(null);
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String mojangId = resultSet.getString("MojangID");
                    return mojangId == null ? null : UUID.fromString(mojangId);
                }, getId(username));
    }

    @Override
    public String getUsername(int userId) {
        return userId == -1 ? "CONSOLE" : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> resultSet.getString("Username"), userId);
    }

    @Override
    public CompletableFuture<String> getUsernameAsync(int userId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                        resultSet -> resultSet.getString("Username"), userId)
                .thenApply(username -> {
                    if (userId == -1) return "CONSOLE";
                    return username;
                });
    }

    @Override
    public String getUsername(UUID uuid) {
        if (uuid == null) return null;
        int userId = getId(uuid);
        if (userId == -2) return null;
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> resultSet.getString("Username"), userId);
    }

    @Override
    public CompletableFuture<String> getUsernameAsync(UUID uuid) {
        if (uuid == null) return CompletableFuture.completedFuture(null);
        int userId = getId(uuid);
        if (userId == -2) return CompletableFuture.completedFuture(null);
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> resultSet.getString("Username"), userId);
    }

    @Override
    public void rename(int userId, String newUsername) {
        if (userId == -1 || userId == -2 || newUsername == null) return;
        database.updateCallable(Procedure.RENAME.getName(), userId, newUsername);
    }

    @Override
    public CompletableFuture<Integer> renameAsync(int userId, String newUsername) {
        if (userId == -1 || userId == -2 || newUsername == null) return CompletableFuture.completedFuture(-2);
        return database.updateCallableAsync(Procedure.RENAME.getName(), userId, newUsername);
    }

    @Override
    public void rename(UUID uuid, String newUsername) {
        if (uuid == null || newUsername == null) return;
        int userId = getId(uuid);
        if (userId == -2) return;
        database.updateCallable(Procedure.RENAME.getName(), userId, newUsername);
    }

    @Override
    public CompletableFuture<Integer> renameAsync(UUID uuid, String newUsername) {
        if (uuid == null || newUsername == null) return CompletableFuture.completedFuture(-2);
        int userId = getId(uuid);
        if (userId == -2) return CompletableFuture.completedFuture(-2);
        return database.updateCallableAsync(Procedure.RENAME.getName(), userId, newUsername);
    }

    @Override
    public List<UUID> getUniqueIds() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), new LinkedList<>(), resultSet -> {
            String id = resultSet.getString("MojangID");
            return id == null ? null : UUID.fromString(id);
        }).stream().filter(Objects::nonNull).toList();
    }

    @Override
    public CompletableFuture<List<UUID>> getUniqueIdsAsync() {
        return database.queryListCallableAsync(Procedure.GET_DATA.getName(), new LinkedList<>(), resultSet -> {
            String id = resultSet.getString("MojangID");
            return id == null ? null : UUID.fromString(id);
        }).thenApply(list -> list.stream().filter(Objects::nonNull).toList());
    }

    @Override
    public List<String> getUsernames() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), new LinkedList<>(), resultSet -> resultSet.getString("Username"))
                .stream().filter(Objects::nonNull).toList();
    }

    @Override
    public CompletableFuture<List<String>> getUsernamesAsync() {
        return database.queryListCallableAsync(Procedure.GET_DATA.getName(), new LinkedList<>(), resultSet -> resultSet.getString("Username"))
                .thenApply(list -> list.stream().filter(Objects::nonNull).toList());
    }

    @Override
    public Timestamp getFirstJoinTime(int userId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> resultSet.getTimestamp("FirstJoinTime"), userId);
    }

    @Override
    public CompletableFuture<Timestamp> getFirstJoinTimeAsync(int userId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> resultSet.getTimestamp("FirstJoinTime"), userId);
    }

    @Override
    public String getFirstJoinDate(int userId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    Timestamp time = resultSet.getTimestamp("FirstJoinTime");
                    return time == null ? "never" : getDateFormat().format(time);
                }, userId);
    }

    @Override
    public CompletableFuture<String> getFirstJoinDateAsync(int userId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    Timestamp time = resultSet.getTimestamp("FirstJoinTime");
                    return time == null ? "never" : getDateFormat().format(time);
                }, userId);
    }

    @Override
    public void setFirstJoinTime(int userId) {
        if (userId == -2 || userId == -1) return;
        database.updateCallable(Procedure.SET_FIRST_JOIN.getName(), userId,
                new Timestamp(System.currentTimeMillis()).toString());
    }

    @Override
    public CompletableFuture<Integer> setFirstJoinTimeAsync(int userId) {
        if (userId == -2 || userId == -1) return CompletableFuture.completedFuture(-2);
        return database.updateCallableAsync(Procedure.SET_FIRST_JOIN.getName(), userId,
                new Timestamp(System.currentTimeMillis()).toString());
    }

    @Override
    public void joinUser(UUID uuid, String username) {
        if (uuid == null || username == null) return;
        int userId = getId(uuid);
        if (userId == -1) return;
        if (userId == -2) {
            createUser(uuid, username);
            return;
        }

        String currentUsername = getUsername(userId);
        if (currentUsername == null) {
            rename(userId, username);
            return;
        }

        if (!currentUsername.equals(username)) rename(userId, username);
    }

    @Override
    public CompletableFuture<Void> joinUserAsync(UUID uuid, String username) {
        return CompletableFuture.runAsync(() -> joinUser(uuid, username));
    }

    @Override
    public void loadExpired() {
        parent.loadExpired();
        permission.loadExpired();
    }

    @Override
    public UserParent getParent() {
        if (parent == null) parent = new UserParentProvider(database);
        return parent;
    }

    @Override
    public UserPermission getPermission() {
        if (permission == null) permission = new UserPermissionProvider(database);
        return permission;
    }

    private enum Procedure {
        GET_DATA_BY_ID("Users_GetDataById", "uid INT", "SELECT MojangID, Username, FirstJoinTime FROM [TABLE] WHERE ID=uid;"),
        GET_ID_BY_UUID("Users_GetIdByUUID", "uid UUID", "SELECT ID FROM [TABLE] WHERE MojangID=uid;"),
        GET_ID_BY_USERNAME("Users_GetIdByUsername", "uname VARCHAR(100)", "SELECT ID FROM [TABLE] WHERE Username=uname;"),
        GET_DATA("Users_GetData", "", "SELECT ID, MojangID, Username FROM [TABLE];"),
        CREATE("Users_Create", "uid UUID, uname VARCHAR(100)",
                "INSERT INTO [TABLE] (MojangID,Username) VALUES (uid,uname);"),
        DELETE("Users_Delete", "uid UUID", "DELETE FROM [TABLE] WHERE MojangID=uid;"),
        RENAME("Users_Rename", "uid INT, uname VARCHAR(100)", "UPDATE [TABLE] SET Username=uname WHERE ID=uid;"),
        SET_FIRST_JOIN("Users_SetFirstJoin", "uid INT, time DATETIME", "UPDATE [TABLE] SET FirstJoinTime=time WHERE ID=uid;"),
        CREATE_CONSOLE("Users_CreateConsole", "uid INT", "INSERT INTO [TABLE] (ID) VALUES (uid);");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query.replace("[TABLE]", TABLE_NAME);
        }

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES) database.update(procedure.getQuery());
        }
    }
}

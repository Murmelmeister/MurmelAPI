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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
        if (database.exists(Procedure.USERS_GET_ALL_BY_ID.getName(), id)) return;
        database.callUpdate(Procedure.USERS_CREATE_CONSOLE.getName(), id);
    }

    // --- Asynchronous Methods ---

    public CompletableFuture<Integer> createOrGetUserAsync(String username) {
        return getIdAsync(username).thenCompose(id -> {
            if (id != -2) return CompletableFuture.completedFuture(id);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return MojangUtils.getUUID(username);
                } catch (IOException | URISyntaxException e) {
                    throw new CompletionException(e);
                }
            }).thenCompose(uuid -> createUserAsync(uuid, username)
                    .thenCompose(v -> getIdAsync(uuid)));
        });
    }

    public CompletableFuture<Integer> createOrGetUserAsync(UUID uuid) {
        return getIdAsync(uuid).thenCompose(id -> {
            if (id != -2) return CompletableFuture.completedFuture(id);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return MojangUtils.getUsername(uuid);
                } catch (IOException | URISyntaxException e) {
                    throw new CompletionException(e);
                }
            }).thenCompose(username -> createUserAsync(uuid, username)
                    .thenCompose(v -> getIdAsync(uuid)));
        });
    }

    public CompletableFuture<Boolean> existsUserAsync(UUID uuid) {
        return database.asyncExists(Procedure.USERS_GET_ALL_BY_UUID.getName(), uuid.toString());
    }

    public CompletableFuture<Boolean> existsUserAsync(String username) {
        return database.asyncExists(Procedure.USERS_GET_ALL_BY_USERNAME.getName(), username);
    }

    public CompletableFuture<Void> createUserAsync(UUID uuid, String username) {
        return database.asyncUpdate(Procedure.USERS_CREATE.getName(), uuid.toString(), username);
    }

    public CompletableFuture<Void> deleteUserAsync(UUID uuid) {
        return getUsernameAsync(uuid).thenCompose(username ->
                database.asyncUpdate(Procedure.USERS_DELETE.getName(), uuid.toString())
                        .thenRun(() -> {
                            if (username != null) usernameCache.remove(username);
                            if (uuidCache.get(uuid) != null) uuidCache.remove(uuid);
                        })
        );
    }

    public CompletableFuture<Integer> getIdAsync(UUID uuid) {
        Integer cached = uuidCache.get(uuid);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return database.asyncQuery(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_UUID.getName(), uuid.toString())
                .thenApply(id -> {
                    if (id != -2) uuidCache.put(uuid, id, 1, TimeUnit.HOURS);
                    return id;
                });
    }

    public CompletableFuture<Integer> getIdAsync(String username) {
        Integer cached = usernameCache.get(username);
        if (cached != null) return CompletableFuture.completedFuture(cached);
        return database.asyncQuery(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_USERNAME.getName(), username)
                .thenApply(id -> {
                    if (id != -2) usernameCache.put(username, id, 1, TimeUnit.HOURS);
                    return id;
                });
    }

    public CompletableFuture<UUID> getUniqueIdAsync(int userId) {
        if (userId == -1) return CompletableFuture.completedFuture(null);
        return database.asyncQuery(null, "MojangID", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId)
                .thenApply(str -> str == null ? null : UUID.fromString(str));
    }

    public CompletableFuture<UUID> getUniqueIdAsync(String username) {
        return getIdAsync(username).thenCompose(id ->
                database.asyncQuery(null, "MojangID", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), id)
                        .thenApply(str -> str == null ? null : UUID.fromString(str))
        );
    }

    public CompletableFuture<String> getUsernameAsync(int userId) {
        if (userId == -1) return CompletableFuture.completedFuture("CONSOLE");
        return database.asyncQuery(null, "Username", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId);
    }

    public CompletableFuture<String> getUsernameAsync(UUID uuid) {
        return getIdAsync(uuid).thenCompose(id ->
                id == -1 ? CompletableFuture.completedFuture("CONSOLE") :
                        database.asyncQuery(null, "Username", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), id)
        );
    }

    public CompletableFuture<Void> renameAsync(int userId, String newUsername) {
        return database.asyncUpdate(Procedure.USERS_RENAME.getName(), newUsername, userId);
    }

    public CompletableFuture<Void> renameAsync(UUID uuid, String newUsername) {
        return getIdAsync(uuid).thenCompose(id -> database.asyncUpdate(Procedure.USERS_RENAME.getName(), newUsername, id));
    }

    public CompletableFuture<List<UUID>> getUniqueIdsAsync() {
        return database.asyncQueryList(new LinkedList<>(), "MojangID", String.class, Procedure.USERS_GET_ALL.getName())
                .thenApply(list -> list.parallelStream()
                        .filter(Objects::nonNull)
                        .map(UUID::fromString)
                        .toList());
    }

    public CompletableFuture<List<String>> getUsernamesAsync() {
        return database.asyncQueryList(new LinkedList<>(), "Username", String.class, Procedure.USERS_GET_ALL.getName())
                .thenApply(list -> list.parallelStream().filter(Objects::nonNull).toList());
    }

    public CompletableFuture<Timestamp> getFirstJoinTimeAsync(int userId) {
        return database.asyncQuery(null, "FirstJoinTime", Timestamp.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId);
    }

    public CompletableFuture<String> getFirstJoinDateAsync(int userId) {
        return getFirstJoinTimeAsync(userId)
                .thenApply(time -> time == null ? "never" : getDateFormat().format(time));
    }

    public CompletableFuture<Void> setFirstJoinTimeAsync(int userId) {
        return getFirstJoinTimeAsync(userId).thenCompose(time -> {
            if (time != null) return CompletableFuture.completedFuture(null);
            return database.asyncUpdate(Procedure.USERS_SET_FIRST_JOIN.getName(), userId,
                    new Timestamp(System.currentTimeMillis()).toString());
        });
    }

    public CompletableFuture<Void> joinUserAsync(UUID uuid, String username) {
        return getIdAsync(uuid).thenCompose(id -> {
            if (id == -2) {
                return createUserAsync(uuid, username)
                        .thenCompose(v -> getIdAsync(uuid))
                        .thenCompose(newId -> joinUserAsync(uuid, username));
            }
            return getUsernameAsync(id).thenCompose(currentUsername -> {
                if (!currentUsername.equals(username)) {
                    return renameAsync(id, username);
                }
                return CompletableFuture.completedFuture(null);
            });
        });
    }

    /*public CompletableFuture<Void> loadExpiredAsync() {
        return CompletableFuture.allOf(
                parent.loadExpiredAsync(this),
                permission.loadExpiredAsync(this)
        );
    }*/

    public CompletableFuture<UserParent> getParentAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (parent == null) {
                parent = new UserParentProvider(database);
            }
            return parent;
        });
    }

    public CompletableFuture<UserPermission> getPermissionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            if (permission == null) {
                permission = new UserPermissionProvider(database);
            }
            return permission;
        });
    }

    // --- Synchronous Methods ---

    @Override
    public int createOrGetUser(String username) {
        return createOrGetUserAsync(username).join();
    }

    @Override
    public int createOrGetUser(UUID uuid) {
        return createOrGetUserAsync(uuid).join();
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return existsUserAsync(uuid).join();
    }

    @Override
    public boolean existsUser(String username) {
        return existsUserAsync(username).join();
    }

    @Override
    public void createUser(UUID uuid, String username) {
        createUserAsync(uuid, username).join();
    }

    @Override
    public void deleteUser(UUID uuid) {
        deleteUserAsync(uuid).join();
    }

    @Override
    public int getId(UUID uuid) {
        return getIdAsync(uuid).join();
    }

    @Override
    public int getId(String username) {
        return getIdAsync(username).join();
    }

    private int loadIdByUUID(UUID uuid) {
        return database.query(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_UUID.getName(), uuid.toString());
    }

    private int loadIdByUsername(String username) {
        return database.query(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_USERNAME.getName(), username);
    }

    @Override
    public UUID getUniqueId(int userId) {
        return userId == -1 ? null : getUniqueIdAsync(userId).join();
    }

    @Override
    public UUID getUniqueId(String username) {
        return getUniqueIdAsync(username).join();
    }

    @Override
    public String getUsername(int userId) {
        return userId == -1 ? "CONSOLE" : getUsernameAsync(userId).join();
    }

    @Override
    public String getUsername(UUID uuid) {
        return getUsernameAsync(uuid).join();
    }

    @Override
    public void rename(int userId, String newUsername) {
        renameAsync(userId, newUsername).join();
    }

    @Override
    public void rename(UUID uuid, String newUsername) {
        renameAsync(uuid, newUsername).join();
    }

    @Override
    public List<UUID> getUniqueIds() {
        return getUniqueIdsAsync().join();
    }

    @Override
    public List<String> getUsernames() {
        return getUsernamesAsync().join();
    }

    @Override
    public Timestamp getFirstJoinTime(int userId) {
        return getFirstJoinTimeAsync(userId).join();
    }

    @Override
    public String getFirstJoinDate(int userId) {
        return getFirstJoinDateAsync(userId).join();
    }

    @Override
    public void setFirstJoinTime(int userId) {
        setFirstJoinTimeAsync(userId).join();
    }

    @Override
    public void joinUser(UUID uuid, String username) {
        joinUserAsync(uuid, username).join();
    }

    @Override
    public void loadExpired() {
        parent.loadExpired(this);
        permission.loadExpired(this);
    }

    @Override
    public UserParent getParent() {
        return getParentAsync().join();
    }

    @Override
    public UserPermission getPermission() {
        return getPermissionAsync().join();
    }

    private enum Procedure {
        USERS_GET_ALL_BY_UUID("Users_GetAllByUUID", "uid UUID", "SELECT ID FROM [TABLE] WHERE MojangID=uid;"),
        USERS_GET_ALL_BY_USERNAME("Users_GetAllByUsername", "uname VARCHAR(100)", "SELECT ID FROM [TABLE] WHERE Username=uname;"),
        USERS_GET_ALL_BY_ID("Users_GetAllById", "uid INT", "SELECT * FROM [TABLE] WHERE ID=uid;"),
        USERS_GET_ALL_BY_IP("Users_GetAllByIP", "ip INET6", "SELECT * FROM [TABLE] WHERE IPAddress=ip;"),
        USERS_GET_ALL("Users_GetAll", "", "SELECT ID, MojangID, Username FROM [TABLE];"),
        USERS_CREATE("Users_Create", "uid UUID, uname VARCHAR(100)",
                "INSERT INTO [TABLE] (MojangID,Username) VALUES (uid,uname);"),
        USERS_DELETE("Users_Delete", "uid UUID", "DELETE FROM [TABLE] WHERE MojangID=uid;"),
        USERS_RENAME("Users_Rename", "uname VARCHAR(100), uid INT", "UPDATE [TABLE] SET Username=uname WHERE ID=uid;"),
        USERS_SET_FIRST_JOIN("Users_SetFirstJoin", "uid INT, time DATETIME", "UPDATE [TABLE] SET FirstJoinTime=time WHERE ID=uid;"),
        USERS_CREATE_CONSOLE("Users_CreateConsole", "uid INT", "INSERT INTO [TABLE] (ID) VALUES (uid);");
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

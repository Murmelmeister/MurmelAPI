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

    @Override
    public int createOrGetUser(String username) {
        int id = getId(username);
        if (id != -2) return id;
        try {
            UUID uuid = MojangUtils.getUUID(username);
            createUser(uuid, username);
            return getId(uuid);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Couldn't find any profile with username: " + username);
        }
    }

    @Override
    public int createOrGetUser(UUID uuid) {
        int id = getId(uuid);
        if (id != -2) return id;
        try {
            String username = MojangUtils.getUsername(uuid);
            createUser(uuid, username);
            return getId(uuid);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException("Couldn't find any profile with uuid: " + uuid);
        }
    }

    @Override
    public boolean existsUser(UUID uuid) {
        return database.exists(Procedure.USERS_GET_ALL_BY_UUID.getName(), uuid.toString());
    }

    @Override
    public boolean existsUser(String username) {
        return database.exists(Procedure.USERS_GET_ALL_BY_USERNAME.getName(), username);
    }

    @Override
    public void createUser(UUID uuid, String username) {
        database.callUpdate(Procedure.USERS_CREATE.getName(), uuid.toString(), username);
    }

    @Override
    public void deleteUser(UUID uuid) {
        String username = getUsername(uuid);
        database.update(Procedure.USERS_DELETE.getName(), uuid.toString());
        if (username != null) usernameCache.remove(username);
        if (uuidCache.get(uuid) != null) uuidCache.remove(uuid);
    }

    @Override
    public int getId(UUID uuid) {
        Integer id = uuidCache.get(uuid);
        if (id == null) {
            id = loadIdByUUID(uuid);
            if (id != -2) uuidCache.put(uuid, id, 1, TimeUnit.HOURS);
        }
        return id;
    }

    @Override
    public int getId(String username) {
        Integer id = usernameCache.get(username);
        if (id == null) {
            id = loadIdByUsername(username);
            if (id != -2) usernameCache.put(username, id, 1, TimeUnit.HOURS);
        }
        return id;
    }

    private int loadIdByUUID(UUID uuid) {
        return database.query(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_UUID.getName(), uuid.toString());
    }

    private int loadIdByUsername(String username) {
        return database.query(-2, "ID", int.class, Procedure.USERS_GET_ALL_BY_USERNAME.getName(), username);
    }

    @Override
    public UUID getUniqueId(int userId) {
        return userId == -1 ? null : UUID.fromString(database.query(null, "MojangID", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId));
    }

    @Override
    public UUID getUniqueId(String username) {
        int id = getId(username);
        return UUID.fromString(database.query(null, "MojangID", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), id));
    }

    @Override
    public String getUsername(int userId) {
        return userId == -1 ? "CONSOLE" : database.query(null, "Username", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId);
    }

    @Override
    public String getUsername(UUID uuid) {
        int id = getId(uuid);
        return id == -1 ? "CONSOLE" : database.query(null, "Username", String.class, Procedure.USERS_GET_ALL_BY_ID.getName(), id);
    }

    @Override
    public void rename(int userId, String newUsername) {
        database.callUpdate(Procedure.USERS_RENAME.getName(), newUsername, userId);
    }

    @Override
    public void rename(UUID uuid, String newUsername) {
        int id = getId(uuid);
        database.callUpdate(Procedure.USERS_RENAME.getName(), newUsername, id);
    }

    @Override
    public List<UUID> getUniqueIds() {
        return database.queryList(new LinkedList<>(), "MojangID", String.class, Procedure.USERS_GET_ALL.getName())
                .parallelStream()
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();
    }

    @Override
    public List<String> getUsernames() {
        return database.queryList(new LinkedList<>(), "Username", String.class, Procedure.USERS_GET_ALL.getName())
                .parallelStream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Timestamp getFirstJoinTime(int userId) {
        return database.query(null, "FirstJoinTime", Timestamp.class, Procedure.USERS_GET_ALL_BY_ID.getName(), userId);
    }

    @Override
    public String getFirstJoinDate(int userId) {
        Timestamp time = getFirstJoinTime(userId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void setFirstJoinTime(int userId) {
        if (getFirstJoinTime(userId) != null) return;
        database.callUpdate(Procedure.USERS_SET_FIRST_JOIN.getName(), userId, new Timestamp(System.currentTimeMillis()).toString());
    }

    @Override
    public void joinUser(UUID uuid, String username) {
        int id = getId(uuid);
        if (id == -2) {
            createUser(uuid, username);
            id = getId(uuid);
        }
        if (!getUsername(id).equals(username)) rename(id, username);
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

package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.utils.CacheManager;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public final class GroupProvider implements Group {
    private static final String TABLE_NAME = "Groups";

    private final CacheManager<Integer, String> groupIdCache = new CacheManager<>();
    private final CacheManager<String, Integer> groupNameCache = new CacheManager<>();

    private final Database database;
    private GroupColor color;
    private GroupParent parent;
    private GroupPermission permission;

    public GroupProvider(Database database) {
        this.database = database;
        this.color = getColor();
        this.parent = getParent();
        this.permission = getPermission();
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "ID INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "GroupName VARCHAR(100) UNIQUE, " +
                                         "Priority INT, " +
                                         "TeamSort VARCHAR(100), " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    // === Asynchrone API-Methoden ===

    public CompletableFuture<Boolean> existsGroupAsync(int groupId) {
        return database.asyncExists(Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Boolean> existsGroupAsync(String groupName) {
        return database.asyncExists(Procedure.GROUPS_GET_ALL_BY_NAME.getName(), groupName);
    }

    public CompletableFuture<Void> createNewGroupAsync(String groupName, int createdBy, int priority, String teamId) {
        String team = teamId + groupName;
        return database.asyncUpdate(Procedure.GROUPS_CREATE.getName(), groupName, priority, team, createdBy, createdBy)
                .thenCompose(v -> getUniqueIdAsync(groupName))
                .thenAccept(id -> {
                    color.createGroup(createdBy, id);
                });
    }

    public CompletableFuture<Void> deleteGroupAsync(int executorId, int groupId) {
        return getNameAsync(groupId).thenCompose(name ->
                database.asyncUpdate(Procedure.GROUPS_DELETE.getName(), groupId)
        ).thenRun(() -> {
            String cachedName = groupIdCache.get(groupId);
            if (cachedName != null) {
                groupNameCache.remove(cachedName);
            }
            if (groupIdCache.get(groupId) != null) {
                groupIdCache.remove(groupId);
            }
        });
    }

    public CompletableFuture<Integer> getUniqueIdAsync(String groupName) {
        Integer cached = groupNameCache.get(groupName);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return database.asyncQuery(-1, "ID", int.class, Procedure.GROUPS_GET_ALL_BY_NAME.getName(), groupName)
                .thenApply(id -> {
                    groupNameCache.put(groupName, id, 1, TimeUnit.HOURS);
                    return id;
                });
    }

    public CompletableFuture<String> getNameAsync(int groupId) {
        String cached = groupIdCache.get(groupId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        return database.asyncQuery(null, "GroupName", String.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId)
                .thenApply(name -> {
                    groupIdCache.put(groupId, name, 1, TimeUnit.HOURS);
                    return name;
                });
    }

    public CompletableFuture<Void> renameAsync(int executorId, int groupId, String newName) {
        return database.asyncUpdate(Procedure.GROUPS_SET_GROUP_NAME.getName(), newName, groupId, executorId);
    }

    public CompletableFuture<List<Integer>> getUniqueIdsAsync() {
        return database.asyncQueryList(new LinkedList<>(), "ID", int.class, Procedure.GROUPS_GET_ALL.getName());
    }

    public CompletableFuture<List<String>> getNamesAsync() {
        return database.asyncQueryList(new LinkedList<>(), "GroupName", String.class, Procedure.GROUPS_GET_ALL.getName());
    }

    public CompletableFuture<Integer> getPriorityAsync(int groupId) {
        return database.asyncQuery(-1, "Priority", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> setPriorityAsync(int executorId, int groupId, int priority) {
        return database.asyncUpdate(Procedure.GROUPS_SET_PRIORITY.getName(), priority, groupId, executorId);
    }

    public CompletableFuture<String> getTeamSortAsync(int groupId) {
        return database.asyncQuery(null, "TeamSort", String.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> setTeamSortAsync(int executorId, int groupId, String teamSort) {
        return database.asyncUpdate(Procedure.GROUPS_SET_TEAM_SORT.getName(), teamSort, groupId, executorId);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int groupId) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int groupId) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int groupId) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int groupId) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    public CompletableFuture<Void> createDefaultGroupAsync(String groupName) {
        return existsGroupAsync(groupName).thenCompose(exists -> {
            if (exists) return CompletableFuture.completedFuture(null);
            int createdBy = -1;
            int priority = 1;
            String teamId = 9999 + groupName;
            return database.asyncUpdate(Procedure.GROUPS_CREATE.getName(), groupName, priority, teamId, createdBy, createdBy)
                    .thenCompose(v -> getUniqueIdAsync(groupName))
                    .thenAccept(id -> color.createGroup(createdBy, id, "<gray>", "", "", "", "", "<gray>", "", "", "7"));
        });
    }

    public CompletableFuture<Void> loadExpiredAsync() {
        return getUniqueIdsAsync().thenAccept(ids -> {
            parent.loadExpired(this);
            permission.loadExpired(this);
        });
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsGroup(int groupId) {
        return existsGroupAsync(groupId).join();
    }

    @Override
    public boolean existsGroup(String groupName) {
        return existsGroupAsync(groupName).join();
    }

    @Override
    public void createNewGroup(String groupName, int createdBy, int priority, String teamId) {
        createNewGroupAsync(groupName, createdBy, priority, teamId).join();
    }

    @Override
    public void deleteGroup(int executorId, int groupId) {
        deleteGroupAsync(executorId, groupId).join();
    }

    @Override
    public int getUniqueId(String groupName) {
        return getUniqueIdAsync(groupName).join();
    }

    @Override
    public String getName(int groupId) {
        return getNameAsync(groupId).join();
    }

    @Override
    public void rename(int executorId, int groupId, String newName) {
        renameAsync(executorId, groupId, newName).join();
    }

    @Override
    public List<Integer> getUniqueIds() {
        return getUniqueIdsAsync().join();
    }

    @Override
    public List<String> getNames() {
        return getNamesAsync().join();
    }

    @Override
    public int getPriority(int groupId) {
        return getPriorityAsync(groupId).join();
    }

    @Override
    public void setPriority(int executorId, int groupId, int priority) {
        setPriorityAsync(executorId, groupId, priority).join();
    }

    @Override
    public String getTeamSort(int groupId) {
        return getTeamSortAsync(groupId).join();
    }

    @Override
    public void setTeamSort(int executorId, int groupId, String teamSort) {
        setTeamSortAsync(executorId, groupId, teamSort).join();
    }

    @Override
    public int getCreatedBy(int groupId) {
        return getCreatedByAsync(groupId).join();
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        return getCreatedAtAsync(groupId).join();
    }

    @Override
    public int getModifiedBy(int groupId) {
        return getModifiedByAsync(groupId).join();
    }

    @Override
    public Timestamp getModifiedAt(int groupId) {
        return getModifiedAtAsync(groupId).join();
    }

    @Override
    public void createDefaultGroup(String groupName) {
        createDefaultGroupAsync(groupName).join();
    }

    @Override
    public void loadExpired() {
        loadExpiredAsync().join();
    }

    @Override
    public GroupColor getColor() {
        if (color == null) color = new GroupColorProvider(database);
        return color;
    }

    @Override
    public GroupParent getParent() {
        if (parent == null) parent = new GroupParentProvider(database);
        return parent;
    }

    @Override
    public GroupPermission getPermission() {
        if (permission == null) permission = new GroupPermissionProvider(database);
        return permission;
    }

    private enum Procedure {
        GROUPS_GET_ALL_BY_ID("Groups_GetAllById", "gid INT", "SELECT * FROM [TABLE] WHERE ID=gid;"),
        GROUPS_GET_ALL_BY_NAME("Groups_GetAllByName", "gname VARCHAR(100)", "SELECT ID FROM [TABLE] WHERE GroupName=gname;"),
        GROUPS_GET_ALL("Groups_GetAll", "", "SELECT * FROM [TABLE];"),
        GROUPS_CREATE("Groups_Create", "gname VARCHAR(100), prio INT, tsort VARCHAR(100), created INT, modified INT",
                "INSERT INTO [TABLE] (GroupName,Priority,TeamSort,CreatedBy,ModifiedBy) VALUES (gname,prio,tsort,created,modified);"),
        GROUPS_DELETE("Groups_Delete", "gid INT", "DELETE FROM [TABLE] WHERE ID=gid;"),
        GROUPS_SET_PRIORITY("Groups_SetPriority", "prio INT, gid INT, modified INT", "UPDATE [TABLE] SET Priority=prio, ModifiedBy=modified WHERE ID=gid;"),
        GROUPS_SET_TEAM_SORT("Groups_SetTeamSort", "tsort VARCHAR(100), gid INT, modified INT", "UPDATE [TABLE] SET TeamSort=tsort, ModifiedBy=modified WHERE ID=gid;"),
        GROUPS_SET_GROUP_NAME("Groups_SetGroupName", "gname VARCHAR(100), gid INT, modified INT", "UPDATE [TABLE] SET GroupName=gname, ModifiedBy=modified WHERE ID=gid;");
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

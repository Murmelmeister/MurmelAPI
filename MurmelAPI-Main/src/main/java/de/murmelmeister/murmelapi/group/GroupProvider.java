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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

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

    @Override
    public boolean existsGroup(int groupId) {
        return groupId != -1 && database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), groupId);
    }

    @Override
    public CompletableFuture<Boolean> existsGroupAsync(int groupId) {
        return groupId == -1 ? CompletableFuture.completedFuture(false) : database.existsCallableAsync(Procedure.GET_DATA_BY_ID.getName(), groupId);
    }

    @Override
    public boolean existsGroup(String groupName) {
        return groupName != null && database.existsCallable(Procedure.GET_ID_BY_NAME.getName(), groupName);
    }

    @Override
    public CompletableFuture<Boolean> existsGroupAsync(String groupName) {
        return groupName == null ? CompletableFuture.completedFuture(false) : database.existsCallableAsync(Procedure.GET_ID_BY_NAME.getName(), groupName);
    }

    @Override
    public void createGroup(int executorId, String groupName, int priority, String teamId) {
        Objects.requireNonNull(groupName, "Group name cannot be null");
        Objects.requireNonNull(teamId, "Team ID cannot be null");
        String team = teamId + groupName;
        database.updateCallable(Procedure.CREATE.getName(), groupName, priority, team, executorId, executorId);
        int groupId = getUniqueId(groupName);
        if (groupId != -1) color.createGroup(executorId, groupId);
    }

    @Override
    public CompletableFuture<Integer> createGroupAsync(int executorId, String groupName, int priority, String teamId) {
        Objects.requireNonNull(groupName, "Group name cannot be null");
        Objects.requireNonNull(teamId, "Team ID cannot be null");
        String team = teamId + groupName;
        return database.updateCallableAsync(Procedure.CREATE.getName(), groupName, priority, team, executorId, executorId)
                .thenApply(result -> {
                    int groupId = getUniqueId(groupName);
                    if (groupId != -1) color.createGroup(executorId, groupId);
                    return result;
                });
    }

    @Override
    public void deleteGroup(int executorId, int groupId) {
        if (executorId == -2 || groupId == -1) return;
        String name = getName(groupId);
        database.updateCallable(Procedure.DELETE.getName(), groupId);
        groupNameCache.remove(name);
        groupIdCache.remove(groupId);
    }

    @Override
    public CompletableFuture<Integer> deleteGroupAsync(int executorId, int groupId) {
        if (executorId == -2 || groupId == -1) return CompletableFuture.completedFuture(-1);
        String name = getName(groupId);
        return database.updateCallableAsync(Procedure.DELETE.getName(), groupId)
                .thenApply(result -> {
                    groupNameCache.remove(name);
                    groupIdCache.remove(groupId);
                    return result;
                });
    }

    @Override
    public int getUniqueId(String groupName) {
        if (groupName == null) return -1;
        Integer cached = groupNameCache.get(groupName);
        return cached != null ? cached : database.queryCallable(Procedure.GET_ID_BY_NAME.getName(), -1,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -1) groupNameCache.put(groupName, id, 1, TimeUnit.HOURS);
                    return id;
                }, groupName);
    }

    @Override
    public CompletableFuture<Integer> getUniqueIdAsync(String groupName) {
        if (groupName == null) return CompletableFuture.completedFuture(-1);
        return database.queryCallableAsync(Procedure.GET_ID_BY_NAME.getName(), -1,
                resultSet -> {
                    int id = resultSet.getInt("ID");
                    if (id != -1) groupNameCache.put(groupName, id, 1, TimeUnit.HOURS);
                    return id;
                }, groupName);
    }

    @Override
    public String getName(int groupId) {
        if (groupId == -1) return null;
        String cached = groupIdCache.get(groupId);
        return cached != null ? cached : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String name = resultSet.getString("GroupName");
                    if (name != null) groupIdCache.put(groupId, name, 1, TimeUnit.HOURS);
                    return name;
                }, groupId);
    }

    @Override
    public CompletableFuture<String> getNameAsync(int groupId) {
        if (groupId == -1) return CompletableFuture.completedFuture(null);
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null,
                resultSet -> {
                    String name = resultSet.getString("GroupName");
                    if (name != null) groupIdCache.put(groupId, name, 1, TimeUnit.HOURS);
                    return name;
                }, groupId);
    }

    @Override
    public void rename(int executorId, int groupId, String newName) {
        if (executorId == -2 || groupId == -1 || newName == null) return;
        database.updateCallable(Procedure.SET_GROUP_NAME.getName(), newName, groupId, executorId);
        rename(groupId, newName);
    }

    @Override
    public CompletableFuture<Integer> renameAsync(int executorId, int groupId, String newName) {
        if (executorId == -2 || groupId == -1 || newName == null) return CompletableFuture.completedFuture(-1);
        return database.updateCallableAsync(Procedure.SET_GROUP_NAME.getName(), newName, groupId, executorId)
                .thenApply(result -> {
                    rename(groupId, newName);
                    return result;
                });
    }

    private void rename(int groupId, String newName) {
        String oldName = groupIdCache.get(groupId);
        if (oldName != null) {
            groupNameCache.remove(oldName);
            groupIdCache.remove(groupId);
        }
        groupNameCache.put(newName, groupId, 1, TimeUnit.HOURS);
        groupIdCache.put(groupId, newName, 1, TimeUnit.HOURS);
    }

    @Override
    public List<Integer> getUniqueIds() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), new LinkedList<>(),
                resultSet -> resultSet.getInt("ID"));
    }

    @Override
    public CompletableFuture<List<Integer>> getUniqueIdsAsync() {
        return database.queryListCallableAsync(Procedure.GET_DATA.getName(), new LinkedList<>(),
                resultSet -> resultSet.getInt("ID"));
    }

    @Override
    public List<String> getNames() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), new LinkedList<>(),
                resultSet -> resultSet.getString("GroupName"));
    }

    @Override
    public CompletableFuture<List<String>> getNamesAsync() {
        return database.queryListCallableAsync(Procedure.GET_DATA.getName(), new LinkedList<>(),
                resultSet -> resultSet.getString("GroupName"));
    }

    @Override
    public int getPriority(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -1, resultSet -> resultSet.getInt("Priority"), groupId);
    }

    @Override
    public CompletableFuture<Integer> getPriorityAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), -1, resultSet -> resultSet.getInt("Priority"), groupId);
    }

    @Override
    public void setPriority(int executorId, int groupId, int priority) {
        if (executorId == -2 || groupId == -1) return;
        database.updateCallable(Procedure.SET_PRIORITY.getName(), priority, groupId, executorId);
    }

    @Override
    public CompletableFuture<Integer> setPriorityAsync(int executorId, int groupId, int priority) {
        if (executorId == -2 || groupId == -1) return CompletableFuture.completedFuture(-1);
        return database.updateCallableAsync(Procedure.SET_PRIORITY.getName(), priority, groupId, executorId);
    }

    @Override
    public String getTeamSort(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("TeamSort"), groupId);
    }

    @Override
    public CompletableFuture<String> getTeamSortAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("TeamSort"), groupId);
    }

    @Override
    public void setTeamSort(int executorId, int groupId, String teamSort) {
        if (executorId == -2 || groupId == -1 || teamSort == null) return;
        database.updateCallable(Procedure.SET_TEAM_SORT.getName(), teamSort, groupId, executorId);
    }

    @Override
    public CompletableFuture<Integer> setTeamSortAsync(int executorId, int groupId, String teamSort) {
        if (executorId == -2 || groupId == -1 || teamSort == null) return CompletableFuture.completedFuture(-1);
        return database.updateCallableAsync(Procedure.SET_TEAM_SORT.getName(), teamSort, groupId, executorId);
    }

    @Override
    public int getCreatedBy(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), groupId);
    }

    @Override
    public CompletableFuture<Integer> getCreatedByAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), groupId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), groupId);
    }

    @Override
    public CompletableFuture<Timestamp> getCreatedAtAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), groupId);
    }

    @Override
    public String getCreatedDate(int groupId) {
        Timestamp time = getCreatedAt(groupId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public CompletableFuture<String> getCreatedDateAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> {
            Timestamp time = resultSet.getTimestamp("CreatedAt");
            return time == null ? "never" : getDateFormat().format(time);
        }, groupId);
    }

    @Override
    public int getModifiedBy(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), groupId);
    }

    @Override
    public CompletableFuture<Integer> getModifiedByAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), groupId);
    }

    @Override
    public Timestamp getModifiedAt(int groupId) {
        return database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), groupId);
    }

    @Override
    public CompletableFuture<Timestamp> getModifiedAtAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), groupId);
    }

    @Override
    public String getModifiedDate(int groupId) {
        Timestamp time = getModifiedAt(groupId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public CompletableFuture<String> getModifiedDateAsync(int groupId) {
        return database.queryCallableAsync(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> {
            Timestamp time = resultSet.getTimestamp("ModifiedAt");
            return time == null ? "never" : getDateFormat().format(time);
        }, groupId);
    }

    @Override
    public void createDefaultGroup(String groupName) {
        if (groupName == null) return;
        if (existsGroup(groupName)) return;
        int createdBy = -1;
        int priority = 1;
        String teamId = 9999 + groupName;
        createGroup(createdBy, groupName, priority, teamId);
        color.createGroup(createdBy, getUniqueId(groupName), "<gray>", "", "", "", "", "<gray>", "", "", "7");
    }

    @Override
    public CompletableFuture<Void> createDefaultGroupAsync(String groupName) {
        return CompletableFuture.runAsync(() -> createDefaultGroup(groupName));
    }

    @Override
    public void loadExpired() {
        parent.loadExpired();
        permission.loadExpired();
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
        GET_DATA_BY_ID("Groups_GetDataById", "gid INT", "SELECT * FROM [TABLE] WHERE ID=gid;"),
        GET_ID_BY_NAME("Groups_GetIdByName", "gname VARCHAR(100)", "SELECT ID FROM [TABLE] WHERE GroupName=gname;"),
        GET_DATA("Groups_GetData", "", "SELECT ID, GroupName FROM [TABLE];"),
        CREATE("Groups_Create", "gname VARCHAR(100), prio INT, tsort VARCHAR(100), created INT, modified INT",
                "INSERT INTO [TABLE] (GroupName,Priority,TeamSort,CreatedBy,ModifiedBy) VALUES (gname,prio,tsort,created,modified);"),
        DELETE("Groups_Delete", "gid INT", "DELETE FROM [TABLE] WHERE ID=gid;"),
        SET_PRIORITY("Groups_SetPriority", "prio INT, gid INT, modified INT", "UPDATE [TABLE] SET Priority=prio, ModifiedBy=modified WHERE ID=gid;"),
        SET_TEAM_SORT("Groups_SetTeamSort", "tsort VARCHAR(100), gid INT, modified INT", "UPDATE [TABLE] SET TeamSort=tsort, ModifiedBy=modified WHERE ID=gid;"),
        SET_GROUP_NAME("Groups_SetGroupName", "gname VARCHAR(100), gid INT, modified INT", "UPDATE [TABLE] SET GroupName=gname, ModifiedBy=modified WHERE ID=gid;");
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

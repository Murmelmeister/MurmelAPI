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

    @Override
    public boolean existsGroup(int groupId) {
        return database.exists(Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public boolean existsGroup(String groupName) {
        return database.exists(Procedure.GROUPS_GET_ALL_BY_NAME.getName(), groupName);
    }

    @Override
    public void createNewGroup(String groupName, int createdBy, int priority, String teamId) {
        String team = teamId + groupName;
        database.callUpdate(Procedure.GROUPS_CREATE.getName(), groupName, priority, team, createdBy, createdBy);
        color.createGroup(createdBy, getUniqueId(groupName));
    }

    @Override
    public void deleteGroup(int executorId, int groupId) {
        String groupName = getName(groupId);
        database.callUpdate(Procedure.GROUPS_DELETE.getName(), groupId);
        if (groupName != null) groupNameCache.remove(groupName);
        if (groupIdCache.get(groupId) != null) groupIdCache.remove(groupId);
    }

    @Override
    public int getUniqueId(String groupName) {
        Integer groupId = groupNameCache.get(groupName);
        if (groupId == null) {
            groupId = loadIdByGroupName(groupName);
            groupNameCache.put(groupName, groupId, 1, TimeUnit.HOURS);
        }
        return groupId;
    }

    private int loadIdByGroupName(String groupName) {
        return database.query(-1, "ID", int.class, Procedure.GROUPS_GET_ALL_BY_NAME.getName(), groupName);
    }

    @Override
    public String getName(int groupId) {
        String groupName = groupIdCache.get(groupId);
        if (groupName == null) {
            groupName = loadNameById(groupId);
            groupIdCache.put(groupId, groupName, 1, TimeUnit.HOURS);
        }
        return groupName;
    }

    private String loadNameById(int groupId) {
        return database.query(null, "GroupName", String.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public void rename(int executorId, int groupId, String newName) {
        database.callUpdate(Procedure.GROUPS_SET_GROUP_NAME.getName(), newName, groupId, executorId);
    }

    @Override
    public List<Integer> getUniqueIds() {
        return database.queryList(new LinkedList<>(), "ID", int.class, Procedure.GROUPS_GET_ALL.getName());
    }

    @Override
    public List<String> getNames() {
        return database.queryList(new LinkedList<>(), "GroupName", String.class, Procedure.GROUPS_GET_ALL.getName());
    }

    @Override
    public int getPriority(int groupId) {
        return database.query(-1, "Priority", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public void setPriority(int executorId, int groupId, int priority) {
        database.callUpdate(Procedure.GROUPS_SET_PRIORITY.getName(), priority, groupId, executorId);
    }

    @Override
    public String getTeamSort(int groupId) {
        return database.query(null, "TeamSort", String.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public void setTeamSort(int executorId, int groupId, String teamSort) {
        database.callUpdate(Procedure.GROUPS_SET_TEAM_SORT.getName(), teamSort, groupId, executorId);
    }

    @Override
    public int getCreatedBy(int groupId) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public int getModifiedBy(int groupId) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public Timestamp getModifiedAt(int groupId) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GROUPS_GET_ALL_BY_ID.getName(), groupId);
    }

    @Override
    public void createDefaultGroup(String groupName) {
        if (existsGroup(groupName)) return;
        int createdBy = -1;
        int priority = 1;
        String teamId = 9999 + groupName;
        database.callUpdate(Procedure.GROUPS_CREATE.getName(), groupName, priority, teamId, createdBy, createdBy);
        int id = getUniqueId(groupName);
        color.createGroup(createdBy, id, "<gray>", "", "", "", "", "<gray>", "", "", "7");
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

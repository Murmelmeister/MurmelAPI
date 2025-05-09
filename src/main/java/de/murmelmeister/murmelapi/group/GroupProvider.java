package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;

import java.sql.Timestamp;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * GroupProvider is a class that provides methods to manage groups in the database.
 * It implements the Group interface and uses the Database class to interact with the database.
 */
public final class GroupProvider implements Group {
    private static final String TABLE_NAME = "groups";

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
        database.createTable(TABLE_NAME, "id INT PRIMARY KEY AUTO_INCREMENT, " +
                                         "groupName VARCHAR(100) UNIQUE, " +
                                         "priority INT, " +
                                         "teamSort VARCHAR(100), " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsGroup(int id) {
        return id > 0 && database.existsCallable(Procedure.GET_DATA_BY_ID.getName(), id);
    }

    @Override
    public boolean existsGroup(String groupName) {
        return groupName != null && database.existsCallable(Procedure.GET_DATA_BY_NAME.getName(), groupName);
    }

    @Override
    public int createGroup(String groupName, int priority, String teamId, int createdBy) {
        if (groupName == null || priority < 0 || teamId == null || createdBy == -2) return 0;
        String teamSort = teamId + groupName;
        return database.updateCallable(Procedure.CREATE.getName(), groupName, priority, teamSort, createdBy, createdBy);
    }

    @Override
    public int deleteGroup(int id) {
        if (id < 1) return 0;
        return database.updateCallable(Procedure.DELETE.getName(), id);
    }

    @Override
    public int getId(String groupName) {
        return groupName == null ? -1 : database.queryCallable(Procedure.GET_DATA_BY_NAME.getName(), -1, resultSet -> resultSet.getInt("id"), groupName);
    }

    @Override
    public String getGroupName(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("groupName"), id);
    }

    @Override
    public int rename(int id, String newName, int updatedBy) {
        if (id < 1 || newName == null || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_NAME.getName(), id, newName, updatedBy);
    }

    @Override
    public List<Integer> getIds() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), resultSet -> resultSet.getInt("id"));
    }

    @Override
    public List<String> getGroupNames() {
        return database.queryListCallable(Procedure.GET_DATA.getName(), resultSet -> resultSet.getString("groupName"));
    }

    @Override
    public int getPriority(int id) {
        return id < 1 ? 0 : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), 0, resultSet -> resultSet.getInt("priority"), id);
    }

    @Override
    public int setPriority(int id, int priority, int updatedBy) {
        if (id < 1 || priority < 0 || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_PRIORITY.getName(), id, priority, updatedBy);
    }

    @Override
    public String getTeamSort(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getString("teamSort"), id);
    }

    @Override
    public int setTeamSort(int id, String teamSort, int updatedBy) {
        if (id < 1 || teamSort == null || updatedBy == -2) return 0;
        return database.updateCallable(Procedure.UPDATE_TEAM_SORT.getName(), id, teamSort, updatedBy);
    }

    @Override
    public int getCreatedBy(int id) {
        return id < 1 ? -2 : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("createdBy"), id);
    }

    @Override
    public Timestamp getCreatedAt(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), id);
    }

    @Override
    public String getCreatedDate(int id) {
        Timestamp time = getCreatedAt(id);
        return time == null ? null : getDateFormat().format(time);
    }

    @Override
    public int getUpdatedBy(int id) {
        return id < 1 ? -2 : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), id);
    }

    @Override
    public Timestamp getUpdatedAt(int id) {
        return id < 1 ? null : database.queryCallable(Procedure.GET_DATA_BY_ID.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), id);
    }

    @Override
    public String getUpdatedDate(int id) {
        Timestamp time = getUpdatedAt(id);
        return time == null ? null : getDateFormat().format(time);
    }

    @Override
    public void createDefaultGroup(String groupName) {
        if (groupName == null) return;
        if (existsGroup(groupName)) return;
        int createdBy = -1; // Console
        int priority = 1; // Default priority
        String teamSort = String.valueOf(9999);
        createGroup(groupName, priority, teamSort, createdBy);
        color.createGroup(getId(groupName), null, null, "gray", " <gray>» ",
                null, null, "gray", null, null, "gray", createdBy);
    }

    @Override
    public int loadExpired() {
        int parentRows = parent.loadExpired();
        int permissionRows = permission.loadExpired();
        return parentRows + permissionRows;
    }

    @Override
    public GroupColor getColor() {
        if (color == null)
            color = new GroupColorProvider(database);
        return color;
    }

    @Override
    public GroupParent getParent() {
        if (parent == null)
            parent = new GroupParentProvider(database);
        return parent;
    }

    @Override
    public GroupPermission getPermission() {
        if (permission == null)
            permission = new GroupPermissionProvider(database);
        return permission;
    }

    private enum Procedure {
        CREATE("groups_create", "p_groupName VARCHAR(100), p_priority INT, p_teamSort VARCHAR(100), p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (groupName, priority, teamSort, createdBy, updatedBy) VALUES (p_groupName, p_priority, p_teamSort, p_createdBy, p_updatedBy);"),
        DELETE("groups_delete", "p_id INT", "DELETE FROM [TABLE] WHERE id=p_id;"),
        GET_DATA_BY_ID("groups_getDataById", "p_id INT", "SELECT * FROM [TABLE] WHERE id=p_id;"),
        GET_DATA_BY_NAME("groups_getDataByName", "p_groupName VARCHAR(100)", "SELECT * FROM [TABLE] WHERE groupName=p_groupName;"),
        GET_DATA("groups_getData", "", "SELECT * FROM [TABLE];"),
        UPDATE_PRIORITY("groups_updatePriority", "p_id INT, p_priority INT, p_updatedBy INT",
                "UPDATE [TABLE] SET priority=p_priority, updatedBy=p_updatedBy WHERE id=p_id;"),
        UPDATE_TEAM_SORT("groups_updateTeamSort", "p_id INT, p_teamSort VARCHAR(100), p_updatedBy INT",
                "UPDATE [TABLE] SET teamSort=p_teamSort, updatedBy=p_updatedBy WHERE id=p_id;"),
        UPDATE_NAME("groups_updateName", "p_id INT, p_groupName VARCHAR(100), p_updatedBy INT",
                "UPDATE [TABLE] SET groupName=p_groupName, updatedBy=p_updatedBy WHERE id=p_id;");
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

        private static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}

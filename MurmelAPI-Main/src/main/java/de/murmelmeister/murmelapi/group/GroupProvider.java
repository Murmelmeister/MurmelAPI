package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.settings.*;
import de.murmelmeister.murmelapi.utils.Database;

import java.util.List;

public final class GroupProvider implements Group {
    private final GroupSettings settings;
    private final GroupColorSettings colorSettings;
    private final GroupParent parent;
    private final GroupPermission permission;

    public GroupProvider() {
        String tableName = "Groups";
        createTable(tableName);
        Procedure.loadAll(tableName);
        this.settings = new GroupSettingsProvider();
        this.colorSettings = new GroupColorSettingsProvider();
        this.parent = new GroupParentProvider();
        this.permission = new GroupPermissionProvider();
        createDefaultGroup();
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "ID INT PRIMARY KEY AUTO_INCREMENT, GroupName VARCHAR(100)");
    }

    @Override
    public boolean existsGroup(int id) {
        return Database.callExists(Procedure.GROUP_ID.getName(), id);
    }

    @Override
    public boolean existsGroup(String name) {
        return Database.callExists(Procedure.GROUP_NAME.getName(), name);
    }

    @Override
    public void createNewGroup(String name, int creatorId, int sortId, String teamId) {
        if (existsGroup(name)) return;
        Database.callUpdate(Procedure.GROUP_INSERT.getName(), name);
        int id = getUniqueId(name);
        String team = teamId + getName(id);
        settings.createGroup(id, creatorId, sortId, team);
        colorSettings.createGroup(id, creatorId);
    }

    @Override
    public void deleteGroup(int id) {
        permission.clearPermission(id);
        parent.clearParent(id);
        colorSettings.deleteGroup(id);
        settings.deleteGroup(id);
        Database.callUpdate(Procedure.GROUP_DELETE.getName(), id);
    }

    @Override
    public int getUniqueId(String name) {
        return Database.callQuery(-1, "ID", int.class, Procedure.GROUP_NAME.getName(), name);
    }

    @Override
    public String getName(int id) {
        return Database.callQuery(null, "GroupName", String.class, Procedure.GROUP_ID.getName(), id);
    }

    @Override
    public void rename(int id, String newName) {
        Database.callUpdate(Procedure.GROUP_RENAME_BY_ID.getName(), id, newName);
    }

    @Override
    public void rename(String oldName, String newName) {
        Database.callUpdate(Procedure.GROUP_RENAME_BY_NAME.getName(), oldName, newName);
    }

    @Override
    public List<Integer> getUniqueIds() {
        return Database.callQueryList("ID", int.class, Procedure.GROUP_ALL.getName());
    }

    @Override
    public List<String> getNames() {
        return Database.callQueryList("GroupName", String.class, Procedure.GROUP_ALL.getName());
    }

    @Override
    public void loadExpired() {
        parent.loadExpired(this);
        permission.loadExpired(this);
    }

    private int createDefaultGroup() {
        int id = 1;
        if (existsGroup(id)) return id;
        String name = "default";
        Database.callUpdate(Procedure.GROUP_INSERT.getName(), name);
        int creatorId = -1;
        String team = 9999 + getName(id);
        settings.createGroup(id, creatorId, 0, team);
        colorSettings.createGroup(id, creatorId, "<gray>", "", "", "", "", "<gray>", "", "", "7");
        return id;
    }

    @Override
    public int getDefaultGroup() {
        return createDefaultGroup();
    }

    @Override
    public GroupSettings getSettings() {
        return settings;
    }

    @Override
    public GroupColorSettings getColorSettings() {
        return colorSettings;
    }

    @Override
    public GroupParent getParent() {
        return parent;
    }

    @Override
    public GroupPermission getPermission() {
        return permission;
    }

    private enum Procedure {
        GROUP_ID("Groups_ID", "gid INT", "SELECT * FROM [TABLE] WHERE ID=gid;"),
        GROUP_NAME("Groups_Name", "gName VARCHAR(100)", "SELECT * FROM [TABLE] WHERE GroupName=gName;"),
        GROUP_INSERT("Groups_Insert", "gName VARCHAR(100)", "INSERT INTO [TABLE] (GroupName) VALUES (gName);"),
        GROUP_DELETE("Groups_Delete", "gid INT", "DELETE FROM [TABLE] WHERE ID=gid;"),
        GROUP_ALL("Groups_All", "", "SELECT * FROM [TABLE];"),
        GROUP_RENAME_BY_ID("Groups_RenameByID", "gid INT, gName VARCHAR(100)", "UPDATE [TABLE] SET GroupName=gName WHERE ID=gid;"),
        GROUP_RENAME_BY_NAME("Groups_RenameByName", "oldGroup VARCHAR(100), newGroup VARCHAR(100)", "UPDATE [TABLE] SET GroupName=newGroup WHERE GroupName=oldGroup;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(final String name, final String input, final String query) {
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
            for (Procedure procedure : VALUES) Database.update(procedure.getQuery(tableName));
        }
    }
}

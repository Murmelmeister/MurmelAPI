package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.settings.*;
import de.murmelmeister.murmelapi.utils.Database;

import java.util.List;

public final class GroupProvider implements Group {
    private static final String TABLE_NAME = "Groups";

    private final GroupSettings settings;
    private final GroupColorSettings colorSettings;
    private final GroupParent parent;
    private final GroupPermission permission;

    public GroupProvider() {
        this.createTable();
        Procedure.loadAll();
        this.settings = new GroupSettingsProvider();
        this.colorSettings = new GroupColorSettingsProvider();
        this.parent = new GroupParentProvider();
        this.permission = new GroupPermissionProvider();
        createDefaultGroup();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY AUTO_INCREMENT, GroupName VARCHAR(100))", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int id) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public boolean existsGroup(String name) {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_NAME.getName(), name);
    }

    @Override
    public void createNewGroup(String name, int creatorId, int sortId, String teamId) {
        if (existsGroup(name)) return;
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_INSERT.getName(), name);
        var id = getUniqueId(name);
        var team = teamId + getName(id);
        settings.createGroup(id, creatorId, sortId, team);
        colorSettings.createGroup(id, creatorId);
    }

    @Override
    public void deleteGroup(int id) {
        permission.clearPermission(id);
        parent.clearParent(id);
        colorSettings.deleteGroup(id);
        settings.deleteGroup(id);
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), id);
    }

    @Override
    public int getUniqueId(String name) {
        return Database.getInt(-1, "ID", "CALL %s('%s')", Procedure.PROCEDURE_NAME.getName(), name);
    }

    @Override
    public String getName(int id) {
        return Database.getString(null, "GroupName", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), id);
    }

    @Override
    public void rename(int id, String newName) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME_BY_ID.getName(), id, newName);
    }

    @Override
    public void rename(String oldName, String newName) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME_BY_NAME.getName(), oldName, newName);
    }

    @Override
    public List<Integer> getUniqueIds() {
        return Database.getIntList("ID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<String> getNames() {
        return Database.getStringList("GroupName", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public void loadExpired() {
        parent.loadExpired(this);
        permission.loadExpired(this);
    }

    private int createDefaultGroup() {
        var id = 1;
        if (existsGroup(id)) return id;
        var name = "default";
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_INSERT.getName(), name);
        var creatorId = -1;
        var team = 9999 + getName(id);
        settings.createGroup(id, creatorId, 0, team);
        colorSettings.createGroup(id, creatorId, "&7", "", "", "", "", "&7", "", "", "&7");
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
        PROCEDURE_ID("Groups_ID", Database.getProcedureQuery("Groups_ID", "gid INT", "SELECT * FROM %s WHERE ID=gid;", TABLE_NAME)),
        PROCEDURE_NAME("Groups_Name", Database.getProcedureQuery("Groups_Name", "gName VARCHAR(100)", "SELECT * FROM %s WHERE GroupName=gName;", TABLE_NAME)),
        PROCEDURE_INSERT("Groups_Insert", Database.getProcedureQuery("Groups_Insert", "gName VARCHAR(100)", "INSERT INTO %s (GroupName) VALUES (gName);", TABLE_NAME)),
        PROCEDURE_DELETE("Groups_Delete", Database.getProcedureQuery("Groups_Delete", "gid INT", "DELETE FROM %s WHERE ID=gid;", TABLE_NAME)),
        PROCEDURE_ALL("Groups_All", Database.getProcedureQuery("Groups_All", "", "SELECT * FROM %s;", TABLE_NAME)),
        PROCEDURE_RENAME_BY_ID("Groups_RenameByID", Database.getProcedureQuery("Groups_RenameByID", "gid INT, gName VARCHAR(100)", "UPDATE %s SET GroupName=gName WHERE ID=gid;", TABLE_NAME)),
        PROCEDURE_RENAME_BY_NAME("Groups_RenameByName", Database.getProcedureQuery("Groups_RenameByName", "oldGroup VARCHAR(100), newGroup VARCHAR(100)", "UPDATE %s SET GroupName=newGroup WHERE GroupName=oldGroup;", TABLE_NAME));
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String query) {
            this.name = name;
            this.query = query;
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public static void loadAll() {
            for (var procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;
import de.murmelmeister.murmelapi.group.settings.GroupSettingsProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupProvider implements Group {
    private static final String TABLE_NAME = "Groups";

    private final GroupSettings settings;
    private final GroupParent parent;
    private final GroupPermission permission;

    public GroupProvider() throws SQLException {
        this.createTable();
        Procedure.loadAll();
        this.settings = new GroupSettingsProvider();
        this.parent = new GroupParentProvider();
        this.permission = new GroupPermissionProvider();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (ID INT PRIMARY KEY AUTO_INCREMENT, GroupName VARCHAR(100))", TABLE_NAME);
    }

    @Override
    public boolean existsGroup(int id) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public boolean existsGroup(String name) throws SQLException {
        return Database.exists("CALL %s('%s')", Procedure.PROCEDURE_NAME.getName(), checkArgumentSQL(name));
    }

    @Override
    public void createNewGroup(String name, int creatorId) throws SQLException {
        if (existsGroup(name)) return;
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_INSERT.getName(), name);
        var id = getUniqueId(name);
        settings.createGroup(id, creatorId);
    }

    @Override
    public void deleteGroup(int id) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_DELETE.getName(), checkArgumentSQL(id));
    }

    @Override
    public int getUniqueId(String name) throws SQLException {
        return Database.getInt(-1, "ID", "CALL %s('%s')", Procedure.PROCEDURE_NAME.getName(), checkArgumentSQL(name));
    }

    @Override
    public String getName(int id) throws SQLException {
        return Database.getString(null, "GroupName", "CALL %s('%s')", Procedure.PROCEDURE_ID.getName(), checkArgumentSQL(id));
    }

    @Override
    public void rename(int id, String newName) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME_BY_ID.getName(), checkArgumentSQL(id), newName);
    }

    @Override
    public void rename(String oldName, String newName) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_RENAME_BY_NAME.getName(), checkArgumentSQL(oldName), newName);
    }

    @Override
    public List<Integer> getUniqueIds() throws SQLException {
        return Database.getIntList(new ArrayList<>(), "ID", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public List<String> getNames() throws SQLException {
        return Database.getStringList(new ArrayList<>(), "GroupName", "CALL %s", Procedure.PROCEDURE_ALL.getName());
    }

    @Override
    public void loadExpired() throws SQLException {
        parent.loadExpired(this);
        permission.loadExpired(this);
    }

    @Override
    public int getDefaultGroup() throws SQLException {
        String group = "default";
        createNewGroup(group, -1);
        return getUniqueId(group);
    }

    @Override
    public GroupSettings getSettings() {
        return settings;
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

        public static void loadAll() throws SQLException {
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

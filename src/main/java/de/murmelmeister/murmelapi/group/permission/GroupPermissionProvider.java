package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "GroupPermission";

    public GroupPermissionProvider() throws SQLException {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT, CreatorID INT, Permission VARCHAR(1000), CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsPermission(int groupId, String permission) throws SQLException {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission));
    }

    @Override
    public void addPermission(int groupId, int creatorId, String permission, long time) throws SQLException {
        if (existsPermission(groupId, permission)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), groupId, creatorId, permission, System.currentTimeMillis(), expired);
    }

    @Override
    public void removePermission(int groupId, String permission) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission));
    }

    @Override
    public void clearPermission(int groupId) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public List<String> getPermissions(int groupId) throws SQLException {
        return Database.getStringList(new ArrayList<>(), "Permission", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), checkArgumentSQL(groupId));
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) throws SQLException {
        List<String> permissions = new ArrayList<>(getPermissions(groupId));
        for (int parent : groupParent.getParentIds(groupId)) permissions.addAll(getAllPermissions(groupParent, parent));
        return permissions;
    }

    @Override
    public int getCreatorId(int groupId, String permission) throws SQLException {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission));
    }

    @Override
    public long getCreatedTime(int groupId, String permission) throws SQLException {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission));
    }

    @Override
    public String getCreatedDate(int groupId, String permission) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId, permission));
    }

    @Override
    public long getExpiredTime(int groupId, String permission) throws SQLException {
        return Database.getLong(-2, "Expired", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission));
    }

    @Override
    public String getExpiredDate(int groupId, String permission) throws SQLException {
        long time = getExpiredTime(groupId, permission);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public void setExpiredTime(int groupId, String permission, long time) throws SQLException {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), checkArgumentSQL(groupId), checkArgumentSQL(permission), expired);
    }

    @Override
    public String addExpiredTime(int groupId, String permission, long time) throws SQLException {
        if (Long.toString(time).startsWith("-")) return "No negative time";
        var expired = getExpiredTime(groupId, permission) + (System.currentTimeMillis() + time);
        setExpiredTime(groupId, permission, expired);
        return "";
    }

    @Override
    public String removeExpiredTime(int groupId, String permission, long time) throws SQLException {
        if (Long.toString(time).startsWith("-")) return "No negative time";
        var expired = getExpiredTime(groupId, permission) - (System.currentTimeMillis() + time);
        setExpiredTime(groupId, permission, expired);
        return "";
    }

    @Override
    public void loadExpired(Group group) throws SQLException {
        for (int groupId : group.getUniqueIds())
            for (String permission : getPermissions(groupId)) {
                var time = getExpiredTime(groupId, permission);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removePermission(groupId, permission);
            }
    }

    private enum Procedure {
        PROCEDURE_GROUP_ID("GroupPermission_GroupID", Database.getProcedureQuery("GroupPermission_GroupID", "gid INT", "SELECT * FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_PERMISSION("GroupPermission_Permission", Database.getProcedureQuery("GroupPermission_Permission", "gid INT, perm VARCHAR(1000)", "SELECT * FROM %s WHERE GroupID=gid AND Permission=perm;", TABLE_NAME)),
        PROCEDURE_ADD("GroupPermission_Add", Database.getProcedureQuery("GroupPermission_Add", "gid INT, creator INT, perm VARCHAR(1000), created BIGINT(255), expired BIGINT(255)", "INSERT INTO %s VALUES (gid, creator, perm, created, expired);", TABLE_NAME)),
        PROCEDURE_REMOVE("GroupPermission_Remove", Database.getProcedureQuery("GroupPermission_Remove", "gid INT, perm VARCHAR(1000)", "DELETE FROM %s WHERE GroupID=gid AND Permission=perm;", TABLE_NAME)),
        PROCEDURE_CLEAR("GroupPermission_Clear", Database.getProcedureQuery("GroupPermission_Clear", "gid INT", "DELETE FROM %s WHERE GroupID=gid;", TABLE_NAME)),
        PROCEDURE_EXPIRED("GroupPermission_Expired", Database.getProcedureQuery("GroupPermission_Expired", "gid INT, perm VARCHAR(1000), expired BIGINT(255)", "UPDATE %s SET ExpiredTime=expired WHERE GroupID=gid AND Permission=perm;", TABLE_NAME));
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

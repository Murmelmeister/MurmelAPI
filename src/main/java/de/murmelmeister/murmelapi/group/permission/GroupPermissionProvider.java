package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.*;

public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "GroupPermission";

    public GroupPermissionProvider() {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (GroupID INT, CreatorID INT, Permission VARCHAR(1000), CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public void addPermission(int groupId, int creatorId, String permission, long time) {
        if (existsPermission(groupId, permission)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), groupId, creatorId, permission, System.currentTimeMillis(), expired);
    }

    @Override
    public void removePermission(int groupId, String permission) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_REMOVE.getName(), groupId, permission);
    }

    @Override
    public void clearPermission(int groupId) {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_CLEAR.getName(), groupId);
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return Database.getStringList("Permission", "CALL %s('%s')", Procedure.PROCEDURE_GROUP_ID.getName(), groupId);
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        Set<String> permissions = Collections.synchronizedSet(new LinkedHashSet<>(getPermissions(groupId)));
        groupParent.getParentIds(groupId).parallelStream().map(parentId -> getAllPermissions(groupParent, parentId)).forEach(permissions::addAll);
        return new ArrayList<>(permissions);
    }

    @Override
    public int getCreatorId(int groupId, String permission) {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public long getCreatedTime(int groupId, String permission) {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public String getCreatedDate(int groupId, String permission) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(groupId, permission));
    }

    @Override
    public long getExpiredTime(int groupId, String permission) {
        return Database.getLong(-2, "ExpiredTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public String getExpiredDate(int groupId, String permission) {
        var time = getExpiredTime(groupId, permission);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public String setExpiredTime(int groupId, String permission, long time) {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public String addExpiredTime(int groupId, String permission, long time) {
        var current = getExpiredTime(groupId, permission);
        var expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public String removeExpiredTime(int groupId, String permission, long time) {
        var current = getExpiredTime(groupId, permission);
        var expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public void loadExpired(Group group) {
        for (var groupId : group.getUniqueIds())
            for (var permission : getPermissions(groupId)) {
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

        public static void loadAll() {
            for (var procedure : VALUES)
                Database.update(procedure.getQuery());
        }
    }
}

package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.*;

public final class GroupPermissionProvider implements GroupPermission {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public GroupPermissionProvider() {
        String tableName = "GroupPermission";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "GroupID INT, CreatorID INT, Permission VARCHAR(1000), CreatedTime BIGINT, ExpiredTime BIGINT");
    }

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return Database.callExists(Procedure.GROUP_PERMISSION_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public void addPermission(int groupId, int creatorId, String permission, long time) {
        if (existsPermission(groupId, permission)) return;
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.GROUP_PERMISSION_ADD.getName(), groupId, creatorId, permission, System.currentTimeMillis(), expired);
    }

    @Override
    public void removePermission(int groupId, String permission) {
        Database.callUpdate(Procedure.GROUP_PERMISSION_REMOVE.getName(), groupId, permission);
    }

    @Override
    public void clearPermission(int groupId) {
        Database.callUpdate(Procedure.GROUP_PERMISSION_CLEAR.getName(), groupId);
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return Database.callQueryList("Permission", String.class, Procedure.GROUP_PERMISSION_GROUP_ID.getName(), groupId);
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        Set<String> permissions = Collections.synchronizedSet(new LinkedHashSet<>(getPermissions(groupId)));
        groupParent.getParentIds(groupId).parallelStream().map(parentId -> getAllPermissions(groupParent, parentId)).forEach(permissions::addAll);
        return new ArrayList<>(permissions);
    }

    @Override
    public int getCreatorId(int groupId, String permission) {
        return Database.callQuery(-2, "CreatorID", int.class, Procedure.GROUP_PERMISSION_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public long getCreatedTime(int groupId, String permission) {
        return Database.callQuery(-1L, "CreatedTime", long.class, Procedure.GROUP_PERMISSION_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public String getCreatedDate(int groupId, String permission) {
        return dateFormat.format(getCreatedTime(groupId, permission));
    }

    @Override
    public long getExpiredTime(int groupId, String permission) {
        return Database.callQuery(-2L, "ExpiredTime", long.class, Procedure.GROUP_PERMISSION_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public String getExpiredDate(int groupId, String permission) {
        long time = getExpiredTime(groupId, permission);
        return time == -1 ? "never" : dateFormat.format(time);
    }

    @Override
    public String setExpiredTime(int groupId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.GROUP_PERMISSION_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public String addExpiredTime(int groupId, String permission, long time) {
        long current = getExpiredTime(groupId, permission);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.callUpdate(Procedure.GROUP_PERMISSION_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public String removeExpiredTime(int groupId, String permission, long time) {
        long current = getExpiredTime(groupId, permission);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.callUpdate(Procedure.GROUP_PERMISSION_EXPIRED.getName(), groupId, permission, expired);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public void loadExpired(Group group) {
        for (int groupId : group.getUniqueIds())
            for (String permission : getPermissions(groupId)) {
                long time = getExpiredTime(groupId, permission);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removePermission(groupId, permission);
            }
    }

    private enum Procedure {
        GROUP_PERMISSION_GROUP_ID("GroupPermission_GroupID", "gid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_PERMISSION_PERMISSION("GroupPermission_Permission", "gid INT, perm VARCHAR(1000)", "SELECT * FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        GROUP_PERMISSION_ADD("GroupPermission_Add", "gid INT, creator INT, perm VARCHAR(1000), created BIGINT, expired BIGINT", "INSERT INTO [TABLE] VALUES (gid, creator, perm, created, expired);"),
        GROUP_PERMISSION_REMOVE("GroupPermission_Remove", "gid INT, perm VARCHAR(1000)", "DELETE FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        GROUP_PERMISSION_CLEAR("GroupPermission_Clear", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GROUP_PERMISSION_EXPIRED("GroupPermission_Expired", "gid INT, perm VARCHAR(1000), expired BIGINT", "UPDATE [TABLE] SET ExpiredTime=expired WHERE GroupID=gid AND Permission=perm;");
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

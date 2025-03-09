package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.*;

public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "GroupPermission";
    private final Database database;

    public GroupPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "GroupID INT, Permission VARCHAR(200), PRIMARY KEY (GroupID, Permission), " +
                                         "FOREIGN KEY (GroupID) REFERENCES Groups(ID), " +
                                         "ExpiredTime BIGINT, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return database.exists(Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public void addPermission(int executorId, int groupId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.CREATE.getName(), groupId, permission, expired, executorId, executorId);
    }

    @Override
    public void removePermission(int groupId, String permission) {
        database.callUpdate(Procedure.DELETE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public void clearPermission(int groupId) {
        database.callUpdate(Procedure.DELETE_GROUP.getName(), groupId);
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return database.queryList(new LinkedList<>(), "Permission", String.class, Procedure.GET_BY_GROUP.getName(), groupId);
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        Set<String> permissions = new LinkedHashSet<>();
        Stack<Integer> stack = new Stack<>();
        stack.push(groupId);

        while (!stack.isEmpty()) {
            int currentGroupId = stack.pop();
            permissions.addAll(getPermissions(currentGroupId));

            List<Integer> parentIds = groupParent.getParentIds(currentGroupId);
            for (int i = parentIds.size() - 1; i >= 0; i--)
                stack.push(parentIds.get(i));
        }
        return new LinkedList<>(permissions);
    }

    @Override
    public long getExpiredTime(int groupId, String permission) {
        return database.query(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public String getExpiredDate(int groupId, String permission) {
        long time = getExpiredTime(groupId, permission);
        return time == -1 ? "never" : MurmelAPI.getDateFormat().format(time);
    }

    @Override
    public String setExpiredTime(int executorId, int groupId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.SET_EXPIRED_TIME.getName(), groupId, permission, expired, executorId);
        return getExpiredDate(groupId, permission);
    }

    @Override
    public boolean isExpired(int groupId, String permission) {
        return database.query((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), groupId, permission) == 1;
    }

    @Override
    public int getCreatedBy(int groupId, String permission) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, String permission) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public int getModifiedBy(int groupId, String permission) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public Timestamp getModifiedAt(int groupId, String permission) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public void loadExpired(Group group) {
        List<Integer> groupIds = group.getUniqueIds();
        for (int i = groupIds.size() - 1; i >= 0; i--) {
            int groupId = groupIds.get(i);
            List<String> permissions = getPermissions(groupId);

            for (int j = permissions.size() - 1; j >= 0; j--) {
                String permission = permissions.get(j);
                long time = getExpiredTime(groupId, permission);
                if (time != -1 && time <= System.currentTimeMillis())
                    removePermission(groupId, permission);
            }
        }
    }

    private enum Procedure {
        CREATE("GroupPermission_Create", "gid INT, perm VARCHAR(200), et BIGINT, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,Permission,ExpiredTime,CreatedBy,ModifiedBy) VALUES (gid,perm,et,created,modified);"),
        DELETE_PERMISSION("GroupPermission_DeletePermission", "gid INT, perm VARCHAR(200)", "DELETE FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        DELETE_GROUP("GroupPermission_DeleteGroup", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GET_ALL("GroupPermission_GetAll", "gid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        GET_BY_GROUP("GroupPermission_GetByGroup", "gid INT", "SELECT Permission FROM [TABLE] WHERE GroupID=gid;"),
        SET_EXPIRED_TIME("GroupPermission_SetExpiredTime", "gid INT, perm VARCHAR(200), et BIGINT, modified INT",
                "UPDATE [TABLE] SET ExpiredTime=et, ModifiedBy=modified WHERE GroupID=gid AND Permission=perm;"),
        IS_EXPIRED("GroupPermission_IsExpired", "gid INT, perm VARCHAR(200)",
                "SELECT IF(ExpiredTime = -1, 0, ExpiredTime <= CURRENT_TIMESTAMP()) AS Expired FROM [TABLE] WHERE GroupID=gid AND Permission=perm;");
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

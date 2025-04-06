package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.*;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "GroupPermission";
    private final Database database;

    public GroupPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "GroupID INT, Permission VARCHAR(200), PRIMARY KEY (GroupID, Permission), " +
                                         "FOREIGN KEY (GroupID) REFERENCES Groups(ID), " +
                                         "ExpiredAt DATETIME, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return database.existsCallable(Procedure.GET_ALL.getName(), groupId, permission);
    }

    @Override
    public void addPermission(int executorId, int groupId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.CREATE.getName(), groupId, permission, expired, executorId, executorId);
    }

    @Override
    public void removePermission(int groupId, String permission) {
        database.updateCallable(Procedure.REMOVE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public void clearPermission(int groupId) {
        database.updateCallable(Procedure.CLEAR_PERMISSION.getName(), groupId);
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PERMISSION.getName(), new LinkedList<>(), resultSet -> resultSet.getString("Permission"), groupId);
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(groupId));
        for (int parentId : groupParent.getParentIds(groupId)) {
            permissions.addAll(getAllPermissions(groupParent, parentId));
        }
        return new LinkedList<>(permissions);
    }

    @Override
    public Timestamp getExpiredAt(int groupId, String permission) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("ExpiredAt"), groupId, permission);
    }

    @Override
    public String getExpiredDate(int groupId, String permission) {
        Timestamp time = getExpiredAt(groupId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void setExpiredAt(int executorId, int groupId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.SET_EXPIRED_AT.getName(), groupId, permission, expired, executorId);
    }

    @Override
    public int getCreatedBy(int groupId, String permission) {
        return database.queryCallable(Procedure.GET_ALL.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), groupId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, String permission) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), groupId, permission);
    }

    @Override
    public String getCreatedDate(int groupId, String permission) {
        Timestamp time = getCreatedAt(groupId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public int getModifiedBy(int groupId, String permission) {
        return database.queryCallable(Procedure.GET_ALL.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), groupId, permission);
    }

    @Override
    public Timestamp getModifiedAt(int groupId, String permission) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), groupId, permission);
    }

    @Override
    public String getModifiedDate(int groupId, String permission) {
        Timestamp time = getModifiedAt(groupId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void loadExpired() {
        database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("GroupPermission_Create", "gid INT, perm VARCHAR(200), et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,Permission,ExpiredAt,CreatedBy,ModifiedBy) VALUES (gid,perm,et,created,modified);"),
        REMOVE_PERMISSION("GroupPermission_Remove", "gid INT, perm VARCHAR(200)", "DELETE FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        CLEAR_PERMISSION("GroupPermission_Clear", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GET_ALL("GroupPermission_GetAll", "gid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        GET_ACTIVE_PERMISSION("GroupPermission_GetActive", "gid INT",
                "SELECT Permission FROM [TABLE] WHERE GroupID=gid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP());"),
        SET_EXPIRED_AT("GroupPermission_SetExpiredAt", "gid INT, perm VARCHAR(200), et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE GroupID=gid AND Permission=perm;"),
        UPDATE_EXPIRED("GroupPermission_UpdateExpired", "", "DELETE FROM [TABLE] WHERE ExpiredAt IS NOT NULL AND ExpiredAt <= CURRENT_TIMESTAMP();");
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

package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.parent.GroupParent;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * GroupPermissionProvider is a class that provides methods to manage group permissions in the database.
 * It implements the GroupPermission interface and uses the Database class to interact with the database.
 */
public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "group_permission";

    private final Database database;

    public GroupPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "groupId INT, permission VARCHAR(200), " +
                                         "PRIMARY KEY (groupId, permission), " +
                                         "FOREIGN KEY (groupId) REFERENCES groups(id), " +
                                         "expiredAt DATETIME, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        database.update("CREATE INDEX IF NOT EXISTS idx_group_perm_groupId_exp ON " + TABLE_NAME + " (groupId, expiredAt)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return groupId > 0 && permission != null && database.existsCallable(Procedure.GET_DATA.getName(), groupId, permission);
    }

    @Override
    public int addPermission(int groupId, String permission, long time, int createdBy) {
        if (groupId < 1 || permission == null || createdBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.CREATE.getName(), groupId, permission, expiredAt, createdBy, createdBy);
    }

    @Override
    public int removePermission(int groupId, String permission) {
        if (groupId < 1 || permission == null) return 0;
        return database.updateCallable(Procedure.REMOVE_PERMISSION.getName(), groupId, permission);
    }

    @Override
    public int clearPermission(int groupId) {
        if (groupId < 1) return 0;
        return database.updateCallable(Procedure.CLEAR_PERMISSION.getName(), groupId);
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PERMISSION.getName(), resultSet -> resultSet.getString("permission"), groupId);
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(groupId));
        for (int parentId : groupParent.getParentIds(groupId))
            permissions.addAll(getAllPermissions(groupParent, parentId));
        return new LinkedList<>(permissions);
    }

    @Override
    public Timestamp getExpiredAt(int groupId, String permission) {
        if (groupId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("expiredAt"), groupId, permission);
    }

    @Override
    public String getExpiredDate(int groupId, String permission) {
        Timestamp expiredAt = getExpiredAt(groupId, permission);
        return expiredAt == null ? null : getDateFormat().format(expiredAt);
    }

    @Override
    public int setExpiredAt(int groupId, String permission, long time, int updatedBy) {
        if (groupId < 1 || permission == null || updatedBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.UPDATE_EXPIRED_AT.getName(), groupId, permission, expiredAt, updatedBy);
    }

    @Override
    public int getCreatedBy(int groupId, String permission) {
        if (groupId < 1 || permission == null) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("createdBy"), groupId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, String permission) {
        if (groupId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), groupId, permission);
    }

    @Override
    public String getCreatedDate(int groupId, String permission) {
        Timestamp createdAt = getCreatedAt(groupId, permission);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(int groupId, String permission) {
        if (groupId < 1 || permission == null) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), groupId, permission);
    }

    @Override
    public Timestamp getUpdatedAt(int groupId, String permission) {
        if (groupId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), groupId, permission);
    }

    @Override
    public String getUpdatedDate(int groupId, String permission) {
        Timestamp updatedAt = getUpdatedAt(groupId, permission);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    @Override
    public int loadExpired() {
        return database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("groupPermission_create", "p_groupId INT, p_permission VARCHAR(200), p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (groupId, permission, expiredAt, createdBy, updatedBy) VALUES (p_groupId, p_permission, p_expiredAt, p_createdBy, p_updatedBy);"),
        REMOVE_PERMISSION("groupPermission_remove", "p_groupId INT, p_permission VARCHAR(200)", "DELETE FROM [TABLE] WHERE groupId=p_groupId AND permission=p_permission;"),
        CLEAR_PERMISSION("groupPermission_clear", "p_groupId INT", "DELETE FROM [TABLE] WHERE groupId=p_groupId;"),
        GET_DATA("groupPermission_getData", "p_groupId INT, p_permission VARCHAR(200)", "SELECT * FROM [TABLE] WHERE groupId=p_groupId AND permission=p_permission;"),
        GET_ACTIVE_PERMISSION("groupPermission_getActive", "p_groupId INT",
                "SELECT permission FROM [TABLE] WHERE groupId=p_groupId AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP());"),
        UPDATE_EXPIRED_AT("groupPermission_updateExpiredAt", "p_groupId INT, p_permission VARCHAR(200), p_expiredAt DATETIME, p_updatedBy INT",
                "UPDATE [TABLE] SET expiredAt=p_expiredAt, updatedBy=p_updatedBy WHERE groupId=p_groupId AND permission=p_permission;"),
        UPDATE_EXPIRED("groupPermission_updateExpired", "",
                "DELETE FROM [TABLE] WHERE expiredAt IS NOT NULL AND expiredAt <= CURRENT_TIMESTAMP();");
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

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}

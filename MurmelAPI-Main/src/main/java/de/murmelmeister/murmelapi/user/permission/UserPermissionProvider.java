package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public final class UserPermissionProvider implements UserPermission {
    private static final String TABLE_NAME = "UserPermission";
    private final Database database;

    public UserPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT, Permission VARCHAR(200), PRIMARY KEY (UserID, Permission), " +
                                         "FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "ExpiredTime BIGINT, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int userId, String permission) {
        return database.exists(Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public void addPermission(int executorId, int userId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.CREATE.getName(), userId, permission, expired, executorId, executorId);
    }

    @Override
    public void removePermission(int userId, String permission) {
        database.callUpdate(Procedure.DELETE_PERMISSION.getName(), userId, permission);
    }

    @Override
    public void clearPermission(int userId) {
        database.callUpdate(Procedure.DELETE_USER.getName(), userId);
    }

    @Override
    public List<String> getPermissions(int userId) {
        return database.queryList(new LinkedList<>(), "Permission", String.class, Procedure.GET_BY_USER.getName(), userId);
    }

    @Override
    public long getExpiredTime(int userId, String permission) {
        return database.query(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public String getExpiredDate(int userId, String permission) {
        long time = getExpiredTime(userId, permission);
        return time == -1 ? "never" : MurmelAPI.getDateFormat().format(time);
    }

    @Override
    public String setExpiredTime(int executorId, int userId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.SET_EXPIRED_TIME.getName(), userId, permission, expired, executorId);
        return getExpiredDate(userId, permission);
    }

    @Override
    public boolean isExpired(int userId, String permission) {
        return database.query((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), userId, permission) == 1;
    }

    @Override
    public int getCreatedBy(int userId, String permission) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int userId, String permission) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public int getModifiedBy(int userId, String permission) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public Timestamp getModifiedAt(int userId, String permission) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, permission);
    }

    @Override
    public void loadExpired(User user) {
        List<UUID> userIds = user.getUniqueIds();
        for (int i = userIds.size() - 1; i >= 0; i--) {
            int userId = user.getId(userIds.get(i));
            List<String> permissions = getPermissions(userId);

            for (int j = permissions.size() - 1; j >= 0; j--) {
                String permission = permissions.get(j);
                if (isExpired(userId, permission))
                    removePermission(userId, permission);
            }
        }
    }

    private enum Procedure {
        CREATE("UserPermission_Create", "uid INT, perm VARCHAR(200), et BIGINT, created INT, modified INT",
                "INSERT INTO [TABLE] (UserID,Permission,ExpiredTime,CreatedBy,ModifiedBy) VALUES (uid,perm,et,created,modified);"),
        DELETE_PERMISSION("UserPermission_DeletePermission", "uid INT, perm VARCHAR(200)", "DELETE FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        DELETE_USER("UserPermission_DeleteUser", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_ALL("UserPermission_GetAll", "uid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        GET_BY_USER("UserPermission_GetByUser", "uid INT", "SELECT Permission FROM [TABLE] WHERE UserID=uid;"),
        SET_EXPIRED_TIME("UserPermission_UpdateExpiredTime", "uid INT, perm VARCHAR(200), et BIGINT, modified INT",
                "UPDATE [TABLE] SET ExpiredTime=et, ModifiedBy=modified WHERE UserID=uid AND Permission=perm;"),
        IS_EXPIRED("UserPermission_IsExpired", "uid INT, perm VARCHAR(200)",
                "SELECT IF(ExpiredTime = -1, 0, ExpiredTime <= CURRENT_TIMESTAMP()) AS Expired FROM [TABLE] WHERE UserID=uid AND Permission=perm;");
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

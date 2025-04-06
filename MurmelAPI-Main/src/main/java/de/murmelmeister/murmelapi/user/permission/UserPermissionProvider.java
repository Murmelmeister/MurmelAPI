package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class UserPermissionProvider implements UserPermission {
    private static final String TABLE_NAME = "UserPermission";
    private final Database database;

    public UserPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT, Permission VARCHAR(200), PRIMARY KEY (UserID, Permission), " +
                                         "FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "ExpiredAt DATETIME, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int userId, String permission) {
        return database.existsCallable(Procedure.GET_DATA.getName(), userId, permission);
    }

    @Override
    public void addPermission(int executorId, int userId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.CREATE.getName(), userId, permission, expired, executorId, executorId);
    }

    @Override
    public void removePermission(int userId, String permission) {
        database.updateCallable(Procedure.REMOVE_PERMISSION.getName(), userId, permission);
    }

    @Override
    public void clearPermission(int userId) {
        database.updateCallable(Procedure.CLEAR_PERMISSION.getName(), userId);
    }

    @Override
    public List<String> getPermissions(int userId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PERMISSION.getName(), new LinkedList<>(), resultSet -> resultSet.getString("Permission"), userId);
    }

    @Override
    public Timestamp getExpiredAt(int userId, String permission) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("ExpiredAt"), userId, permission);
    }

    @Override
    public String getExpiredDate(int userId, String permission) {
        Timestamp time = getExpiredAt(userId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void setExpiredAt(int executorId, int userId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.SET_EXPIRED_AT.getName(), userId, permission, expired, executorId);
    }

    @Override
    public int getCreatedBy(int userId, String permission) {
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), userId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int userId, String permission) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), userId, permission);
    }

    @Override
    public String getCreatedDate(int userId, String permission) {
        Timestamp time = getCreatedAt(userId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public int getModifiedBy(int userId, String permission) {
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), userId, permission);
    }

    @Override
    public Timestamp getModifiedAt(int userId, String permission) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), userId, permission);
    }

    @Override
    public String getModifiedDate(int userId, String permission) {
        Timestamp time = getModifiedAt(userId, permission);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void loadExpired() {
        database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("UserPermission_Create", "uid INT, perm VARCHAR(200), et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (UserID,Permission,ExpiredAt,CreatedBy,ModifiedBy) VALUES (uid,perm,et,created,modified);"),
        REMOVE_PERMISSION("UserPermission_Remove", "uid INT, perm VARCHAR(200)", "DELETE FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        CLEAR_PERMISSION("UserPermission_Clear", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_DATA("UserPermission_GetData", "uid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        GET_ACTIVE_PERMISSION("UserPermission_GetActive", "uid INT",
                "SELECT Permission FROM [TABLE] WHERE UserID=uid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP());"),
        SET_EXPIRED_AT("UserPermission_UpdateExpiredAt", "uid INT, perm VARCHAR(200), et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE UserID=uid AND Permission=perm;"),
        UPDATE_EXPIRED("UserPermission_UpdateExpired", "", "DELETE FROM [TABLE] WHERE ExpiredAt IS NOT NULL AND ExpiredAt <= CURRENT_TIMESTAMP();");
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

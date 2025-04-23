package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * UserPermissionProvider class to manage user permissions in the database.
 * This class implements the UserPermission interface and provides methods to interact with user permission data.
 */
public final class UserPermissionProvider implements UserPermission {
    private static final String TABLE_NAME = "user_permission";

    private final Database database;

    public UserPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "userId INT, permission VARCHAR(200), " +
                                         "PRIMARY KEY (userId, permission), " +
                                         "FOREIGN KEY (userId) REFERENCES users(id), " +
                                         "expiredAt DATETIME, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        database.update("CREATE INDEX IF NOT EXISTS idx_user_perm_userId_exp ON " + TABLE_NAME + " (userId, expiredAt)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsPermission(int userId, String permission) {
        return userId > 0 && permission != null && database.existsCallable(Procedure.GET_DATA.getName(), userId, permission);
    }

    @Override
    public int addPermission(int userId, String permission, long time, int createdBy) {
        if (userId < 1 || permission == null || createdBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.CREATE.getName(), userId, permission, expiredAt, createdBy, createdBy);
    }

    @Override
    public int removePermission(int userId, String permission) {
        if (userId < 1 || permission == null) return 0;
        return database.updateCallable(Procedure.REMOVE_PERMISSION.getName(), userId, permission);
    }

    @Override
    public int clearPermission(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.CLEAR_PERMISSION.getName(), userId);
    }

    @Override
    public List<String> getPermissions(int userId) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_ACTIVE_PERMISSION.getName(), result -> result.getString("permission"), userId);
    }

    @Override
    public Timestamp getExpiredAt(int userId, String permission) {
        if (userId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("expiredAt"), userId, permission);
    }

    @Override
    public String getExpiredDate(int userId, String permission) {
        Timestamp time = getExpiredAt(userId, permission);
        return time == null ? null : getDateFormat().format(time);
    }

    @Override
    public int setExpiredAt(int userId, String permission, long time, int updatedBy) {
        if (userId < 1 || permission == null || updatedBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.UPDATE_EXPIRED_AT.getName(), userId, permission, expiredAt, updatedBy);
    }

    @Override
    public int getCreatedBy(int userId, String permission) {
        if (userId < 1 || permission == null) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, result -> result.getInt("createdBy"), userId, permission);
    }

    @Override
    public Timestamp getCreatedAt(int userId, String permission) {
        if (userId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("createdAt"), userId, permission);
    }

    @Override
    public String getCreatedDate(int userId, String permission) {
        Timestamp time = getCreatedAt(userId, permission);
        return time == null ? null : getDateFormat().format(time);
    }

    @Override
    public int getUpdatedBy(int userId, String permission) {
        if (userId < 1 || permission == null) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, result -> result.getInt("updatedBy"), userId, permission);
    }

    @Override
    public Timestamp getUpdatedAt(int userId, String permission) {
        if (userId < 1 || permission == null) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("updatedAt"), userId, permission);
    }

    @Override
    public String getUpdatedDate(int userId, String permission) {
        Timestamp time = getUpdatedAt(userId, permission);
        return time == null ? null : getDateFormat().format(time);
    }

    @Override
    public int loadExpired() {
        return database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("userPermission_create", "p_userId INT, p_permission VARCHAR(200), p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (userId, permission, expiredAt, createdBy, updatedBy) VALUES (p_userId, p_permission, p_expiredAt, p_createdBy, p_updatedBy);"),
        REMOVE_PERMISSION("userPermission_remove", "p_userId INT, p_permission VARCHAR(200)", "DELETE FROM [TABLE] WHERE userId=p_userId AND permission=p_permission;"),
        CLEAR_PERMISSION("userPermission_clear", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId;"),
        GET_DATA("userPermission_getData", "p_userId INT, p_permission VARCHAR(200)", "SELECT * FROM [TABLE] WHERE userId=p_userId AND permission=p_permission;"),
        GET_ACTIVE_PERMISSION("userPermission_getActive", "p_userId INT",
                "SELECT Permission FROM [TABLE] WHERE userId=p_userId AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP());"),
        UPDATE_EXPIRED_AT("userPermission_updateExpiredAt", "p_userId INT, p_permission VARCHAR(200), p_expiredAt DATETIME, p_updatedBy INT",
                "UPDATE [TABLE] SET expiredAt=p_expiredAt, updatedBy=p_updatedBy WHERE userId=p_userId AND permission=p_permission;"),
        UPDATE_EXPIRED("userPermission_updateExpired", "",
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

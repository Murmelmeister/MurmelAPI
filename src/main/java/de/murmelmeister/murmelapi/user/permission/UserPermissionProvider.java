package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;

public final class UserPermissionProvider implements UserPermission {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public UserPermissionProvider() {
        String tableName = "UserPermission";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable(tableName, "UserID INT, CreatorID INT, Permission VARCHAR(1000), CreatedTime BIGINT, ExpiredTime BIGINT");
    }

    @Override
    public boolean existsPermission(int userId, String permission) {
        return Database.callExists(Procedure.USER_PERMISSION_PERMISSION.getName(), userId, permission);
    }

    @Override
    public void addPermission(int userId, int creatorId, String permission, long time) {
        if (existsPermission(userId, permission)) return;
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.USER_PERMISSION_ADD.getName(), userId, creatorId, permission, System.currentTimeMillis(), expired);
    }

    @Override
    public void removePermission(int userId, String permission) {
        Database.callUpdate(Procedure.USER_PERMISSION_REMOVE.getName(), userId, permission);
    }

    @Override
    public void clearPermission(int userId) {
        Database.callUpdate(Procedure.USER_PERMISSION_CLEAR.getName(), userId);
    }

    @Override
    public List<String> getPermissions(int userId) {
        return Database.callQueryList("Permission", String.class, Procedure.USER_PERMISSION_USER_ID.getName(), userId);
    }

    @Override
    public int getCreatorId(int userId, String permission) {
        return Database.callQuery(-2, "CreatorID", int.class, Procedure.USER_PERMISSION_PERMISSION.getName(), userId, permission);
    }

    @Override
    public long getCreatedTime(int userId, String permission) {
        return Database.callQuery(-1L, "CreatedTime", long.class, Procedure.USER_PERMISSION_PERMISSION.getName(), userId, permission);
    }

    @Override
    public String getCreatedDate(int userId, String permission) {
        return dateFormat.format(getCreatedTime(userId, permission));
    }

    @Override
    public long getExpiredTime(int userId, String permission) {
        return Database.callQuery(-2L, "ExpiredTime", long.class, Procedure.USER_PERMISSION_PERMISSION.getName(), userId, permission);
    }

    @Override
    public String getExpiredDate(int userId, String permission) {
        long time = getExpiredTime(userId, permission);
        return time == -1 ? "never" : dateFormat.format(time);
    }

    @Override
    public String setExpiredTime(int userId, String permission, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.callUpdate(Procedure.USER_PERMISSION_EXPIRED.getName(), userId, permission, expired);
        return getExpiredDate(userId, permission);
    }

    @Override
    public String addExpiredTime(int userId, String permission, long time) {
        long current = getExpiredTime(userId, permission);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.callUpdate(Procedure.USER_PERMISSION_EXPIRED.getName(), userId, permission, expired);
        return getExpiredDate(userId, permission);
    }

    @Override
    public String removeExpiredTime(int userId, String permission, long time) {
        long current = getExpiredTime(userId, permission);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.callUpdate(Procedure.USER_PERMISSION_EXPIRED.getName(), userId, permission, expired);
        return getExpiredDate(userId, permission);
    }

    @Override
    public void loadExpired(User user) {
        for (int userId : user.getIds())
            for (String permission : getPermissions(userId)) {
                long time = getExpiredTime(userId, permission);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removePermission(userId, permission);
            }
    }

    private enum Procedure {
        USER_PERMISSION_USER_ID("UserPermission_UserID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        USER_PERMISSION_PERMISSION("UserPermission_Permission", "uid INT, perm VARCHAR(1000)", "SELECT * FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        USER_PERMISSION_ADD("UserPermission_Add", "uid INT, creator INT, perm VARCHAR(1000), created BIGINT, expired BIGINT", "INSERT INTO [TABLE] VALUES (uid, creator, perm, created, expired);"),
        USER_PERMISSION_REMOVE("UserPermission_Remove", "uid INT, perm VARCHAR(1000)", "DELETE FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        USER_PERMISSION_CLEAR("UserPermission_Clear", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        USER_PERMISSION_EXPIRED("UserPermission_Expired", "uid INT, perm VARCHAR(1000), expired BIGINT", "UPDATE [TABLE] SET ExpiredTime=expired WHERE UserID=uid AND Permission=perm;");
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
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
            for (Procedure procedure : VALUES)
                Database.update(procedure.getQuery(tableName));
        }
    }
}

package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static de.murmelmeister.murmelapi.utils.StringUtil.checkArgumentSQL;

public final class UserPermissionProvider implements UserPermission {
    private static final String TABLE_NAME = "UserPermission";

    public UserPermissionProvider() throws SQLException {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() throws SQLException {
        Database.update("CREATE TABLE IF NOT EXISTS %s (UserID INT, CreatorID INT, Permission VARCHAR(1000), CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsPermission(int userId, String permission) throws SQLException {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission));
    }

    @Override
    public void addPermission(int userId, int creatorId, String permission, long time) throws SQLException {
        if (existsPermission(userId, permission)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), userId, creatorId, permission, System.currentTimeMillis(), expired);
    }

    @Override
    public void removePermission(int userId, String permission) throws SQLException {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_REMOVE.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission));
    }

    @Override
    public void clearPermission(int userId) throws SQLException {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_CLEAR.getName(), checkArgumentSQL(userId));
    }

    @Override
    public List<String> getPermissions(int userId) throws SQLException {
        return Database.getStringList(new ArrayList<>(), "Permission", "CALL %s('%s')", Procedure.PROCEDURE_USER_ID.getName(), checkArgumentSQL(userId));
    }

    @Override
    public int getCreatorId(int userId, String permission) throws SQLException {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission));
    }

    @Override
    public long getCreatedTime(int userId, String permission) throws SQLException {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission));
    }

    @Override
    public String getCreatedDate(int userId, String permission) throws SQLException {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(userId, permission));
    }

    @Override
    public long getExpiredTime(int userId, String permission) throws SQLException {
        return Database.getLong(-2, "ExpiredTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PERMISSION.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission));
    }

    @Override
    public String getExpiredDate(int userId, String permission) throws SQLException {
        long time = getExpiredTime(userId, permission);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public void setExpiredTime(int userId, String permission, long time) throws SQLException {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission), expired);
    }

    @Override
    public void addExpiredTime(int userId, String permission, long time) throws SQLException {
        var expired = getExpiredTime(userId, permission) + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission), expired);
    }

    @Override
    public void removeExpiredTime(int userId, String permission, long time) throws SQLException {
        var expired = getExpiredTime(userId, permission) - time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), checkArgumentSQL(userId), checkArgumentSQL(permission), expired);
    }

    @Override
    public void loadExpired(User user) throws SQLException {
        for (int userId : user.getIds())
            for (String permission : getPermissions(userId)) {
                var time = getExpiredTime(userId, permission);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removePermission(userId, permission);
            }
    }

    private enum Procedure {
        PROCEDURE_USER_ID("UserPermission_UserID", Database.getProcedureQuery("UserPermission_UserID", "uid INT", "SELECT * FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_PERMISSION("UserPermission_Permission", Database.getProcedureQuery("UserPermission_Permission", "uid INT, perm VARCHAR(1000)", "SELECT * FROM %s WHERE UserID=uid AND Permission=perm;", TABLE_NAME)),
        PROCEDURE_ADD("UserPermission_Add", Database.getProcedureQuery("UserPermission_Add", "uid INT, creator INT, perm VARCHAR(1000), created BIGINT(255), expired BIGINT(255)", "INSERT INTO %s VALUES (uid, creator, perm, created, expired);", TABLE_NAME)),
        PROCEDURE_REMOVE("UserPermission_Remove", Database.getProcedureQuery("UserPermission_Remove", "uid INT, perm VARCHAR(1000)", "DELETE FROM %s WHERE UserID=uid AND Permission=perm;", TABLE_NAME)),
        PROCEDURE_CLEAR("UserPermission_Clear", Database.getProcedureQuery("UserPermission_Clear", "uid INT", "DELETE FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_EXPIRED("UserPermission_Expired", Database.getProcedureQuery("UserPermission_Expired", "uid INT, perm VARCHAR(1000), expired BIGINT(255)", "UPDATE %s SET ExpiredTime=expired WHERE UserID=uid AND Permission=perm;", TABLE_NAME));
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

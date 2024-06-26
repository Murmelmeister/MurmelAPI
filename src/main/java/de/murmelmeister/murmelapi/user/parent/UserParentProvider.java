package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class UserParentProvider implements UserParent {
    private static final String TABLE_NAME = "UserParent";

    public UserParentProvider() {
        createTable();
        Procedure.loadAll();
    }

    private void createTable() {
        Database.update("CREATE TABLE IF NOT EXISTS %s (UserID INT, CreatorID INT, ParentID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255))", TABLE_NAME);
    }

    @Override
    public boolean existsParent(int userId, int parentId) {
        return Database.exists("CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), userId, parentId);
    }

    @Override
    public void addParent(int userId, int creatorId, int parentId, long time) {
        if (existsParent(userId, parentId)) return;
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s','%s','%s')", Procedure.PROCEDURE_ADD.getName(), userId, creatorId, parentId, System.currentTimeMillis(), expired);
    }

    @Override
    public void removeParent(int userId, int parentId) {
        Database.update("CALL %s('%s','%s')", Procedure.PROCEDURE_REMOVE.getName(), userId, parentId);
    }

    @Override
    public void clearParent(int userId) {
        Database.update("CALL %s('%s')", Procedure.PROCEDURE_CLEAR.getName(), userId);
    }

    @Override
    public int getParentId(int userId) {
        return Database.getInt(-1, "ParentID", "CALL %s('%s')", Procedure.PROCEDURE_USER_ID.getName(), userId);
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        return Database.getIntList("ParentID", "CALL %s('%s')", Procedure.PROCEDURE_USER_ID.getName(), userId);
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        return getParentIds(userId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public int getCreatorId(int userId, int parentId) {
        return Database.getInt(-2, "CreatorID", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), userId, parentId);
    }

    @Override
    public long getCreatedTime(int userId, int parentId) {
        return Database.getLong(-1, "CreatedTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), userId, parentId);
    }

    @Override
    public String getCreatedDate(int userId, int parentId) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(getCreatedTime(userId, parentId));
    }

    @Override
    public long getExpiredTime(int userId, int parentId) {
        return Database.getLong(-2, "ExpiredTime", "CALL %s('%s','%s')", Procedure.PROCEDURE_PARENT.getName(), userId, parentId);
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        var time = getExpiredTime(userId, parentId);
        return time == -1 ? "never" : new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(time);
    }

    @Override
    public String setExpiredTime(int userId, int parentId, long time) {
        var expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public String addExpiredTime(int userId, int parentId, long time) {
        var current = getExpiredTime(userId, parentId);
        var expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public String removeExpiredTime(int userId, int parentId, long time) {
        var current = getExpiredTime(userId, parentId);
        var expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.update("CALL %s('%s','%s','%s')", Procedure.PROCEDURE_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public void loadExpired(User user) {
        for (var userId : user.getIds())
            for (var parentId : getParentIds(userId)) {
                var time = getExpiredTime(userId, parentId);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removeParent(userId, parentId);
            }
    }

    private enum Procedure {
        PROCEDURE_USER_ID("UserParent_UserID", Database.getProcedureQuery("UserParent_UserID", "uid INT", "SELECT * FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_PARENT("UserParent_Parent", Database.getProcedureQuery("UserParent_Parent", "uid INT, pid INT", "SELECT * FROM %s WHERE UserID=uid AND ParentID=pid;", TABLE_NAME)),
        PROCEDURE_ADD("UserParent_Add", Database.getProcedureQuery("UserParent_Add", "uid INT, creator INT, pid INT, created BIGINT(255), expired BIGINT(255)", "INSERT INTO %s VALUES (uid, creator, pid, created, expired);", TABLE_NAME)),
        PROCEDURE_REMOVE("UserParent_Remove", Database.getProcedureQuery("UserParent_Remove", "uid INT, pid INT", "DELETE FROM %s WHERE UserID=uid AND ParentID=pid;", TABLE_NAME)),
        PROCEDURE_CLEAR("UserParent_Clear", Database.getProcedureQuery("UserParent_Clear", "uid INT", "DELETE FROM %s WHERE UserID=uid;", TABLE_NAME)),
        PROCEDURE_EXPIRED("UserParent_Expired", Database.getProcedureQuery("UserParent_Expired", "uid INT, pid INT, expired BIGINT(255)", "UPDATE %s SET ExpiredTime=expired WHERE UserID=uid AND ParentID=pid;", TABLE_NAME));
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

package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.Database;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

public final class UserParentProvider implements UserParent {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    public UserParentProvider() {
        String tableName = "UserParent";
        createTable(tableName);
        Procedure.loadAll(tableName);
    }

    private void createTable(String tableName) {
        Database.createTable("UserID INT, CreatorID INT, ParentID INT, CreatedTime BIGINT(255), ExpiredTime BIGINT(255)", tableName);
    }

    @Override
    public boolean existsParent(int userId, int parentId) {
        return Database.existsCall(Procedure.USER_PARENT_PARENT.getName(), userId, parentId);
    }

    @Override
    public void addParent(int userId, int creatorId, int parentId, long time) {
        if (existsParent(userId, parentId)) return;
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.USER_PARENT_ADD.getName(), userId, creatorId, parentId, System.currentTimeMillis(), expired);
    }

    @Override
    public void removeParent(int userId, int parentId) {
        Database.updateCall(Procedure.USER_PARENT_REMOVE.getName(), userId, parentId);
    }

    @Override
    public void clearParent(int userId) {
        Database.updateCall(Procedure.USER_PARENT_CLEAR.getName(), userId);
    }

    @Override
    public int getParentId(int userId) {
        return Database.getIntCall(-1, "ParentID", Procedure.USER_PARENT_USER_ID.getName(), userId);
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        return Database.getIntListCall("ParentID", Procedure.USER_PARENT_USER_ID.getName(), userId);
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        return getParentIds(userId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public int getCreatorId(int userId, int parentId) {
        return Database.getIntCall(-2, "CreatorID", Procedure.USER_PARENT_PARENT.getName(), userId, parentId);
    }

    @Override
    public long getCreatedTime(int userId, int parentId) {
        return Database.getLongCall(-1, "CreatedTime", Procedure.USER_PARENT_PARENT.getName(), userId, parentId);
    }

    @Override
    public String getCreatedDate(int userId, int parentId) {
        return dateFormat.format(getCreatedTime(userId, parentId));
    }

    @Override
    public long getExpiredTime(int userId, int parentId) {
        return Database.getLongCall(-2, "ExpiredTime", Procedure.USER_PARENT_PARENT.getName(), userId, parentId);
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        long time = getExpiredTime(userId, parentId);
        return time == -1 ? "never" : dateFormat.format(time);
    }

    @Override
    public String setExpiredTime(int userId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        Database.updateCall(Procedure.USER_PARENT_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public String addExpiredTime(int userId, int parentId, long time) {
        long current = getExpiredTime(userId, parentId);
        long expired = current == -1 ? System.currentTimeMillis() + time : current + time;
        Database.updateCall(Procedure.USER_PARENT_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public String removeExpiredTime(int userId, int parentId, long time) {
        long current = getExpiredTime(userId, parentId);
        long expired = current == -1 ? System.currentTimeMillis() : current - time;
        Database.updateCall(Procedure.USER_PARENT_EXPIRED.getName(), userId, parentId, expired);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public void loadExpired(User user) {
        for (int userId : user.getIds())
            for (int parentId : getParentIds(userId)) {
                long time = getExpiredTime(userId, parentId);
                if (time == -1) continue;
                if (time <= System.currentTimeMillis()) removeParent(userId, parentId);
            }
    }

    private enum Procedure {
        USER_PARENT_USER_ID("UserParent_UserID", "uid INT", "SELECT * FROM [TABLE] WHERE UserID=uid;"),
        USER_PARENT_PARENT("UserParent_Parent", "uid INT, pid INT", "SELECT * FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        USER_PARENT_ADD("UserParent_Add", "uid INT, creator INT, pid INT, created BIGINT(255), expired BIGINT(255)", "INSERT INTO [TABLE] VALUES (uid, creator, pid, created, expired);"),
        USER_PARENT_REMOVE("UserParent_Remove", "uid INT, pid INT", "DELETE FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        USER_PARENT_CLEAR("UserParent_Clear", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        USER_PARENT_EXPIRED("UserParent_Expired", "uid INT, pid INT, expired BIGINT(255)", "UPDATE [TABLE] SET ExpiredTime=expired WHERE UserID=uid AND ParentID=pid;");
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

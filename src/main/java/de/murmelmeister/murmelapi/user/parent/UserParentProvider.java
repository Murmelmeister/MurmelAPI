package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * UserParentProvider is a class that provides methods to manage user-parent relationships in the database.
 * It implements the UserParent interface and uses the Database class to interact with the database.
 */
public final class UserParentProvider implements UserParent {
    private static final String TABLE_NAME = "user_parent";

    private final Database database;

    public UserParentProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "userId INT, parentId INT, " +
                                         "PRIMARY KEY (userId, parentId), " +
                                         "FOREIGN KEY (userId) REFERENCES users(id), " +
                                         "FOREIGN KEY (parentId) REFERENCES groups(id)," +
                                         "expiredAt DATETIME, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        database.update("CREATE INDEX IF NOT EXISTS idx_user_parent_userId_exp ON " + TABLE_NAME + " (userId, expiredAt)");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int userId, int parentId) {
        return userId > 0 && parentId > 0 && database.existsCallable(Procedure.GET_DATA.getName(), userId, parentId);
    }

    @Override
    public int addParent(int userId, int parentId, long time, int createdBy) {
        if (userId < 1 || parentId < 1 || createdBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.CREATE.getName(), userId, parentId, expiredAt, createdBy, createdBy);
    }

    @Override
    public int removeParent(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return 0;
        return database.updateCallable(Procedure.REMOVE_PARENT.getName(), userId, parentId);
    }

    @Override
    public int clearParent(int userId) {
        if (userId < 1) return 0;
        return database.updateCallable(Procedure.CLEAR_PARENT.getName(), userId);
    }

    @Override
    public int clearOtherParent(int parentId) {
        if (parentId < 1) return 0;
        return database.updateCallable(Procedure.CLEAR_OTHER_PARENT.getName(), parentId);
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), result -> result.getInt("parentId"), userId);
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        if (userId < 1) return null;
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), result -> group.getGroupName(result.getInt("parentId")), userId);
    }

    @Override
    public int getHighestPriority(Group group, int userId) {
        if (userId < 1) return -1;
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), result -> group.getPriority(result.getInt("parentId")), userId)
                .stream()
                .max(Comparator.naturalOrder())
                .orElse(-1);
    }

    @Override
    public Timestamp getExpiredAt(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("expiredAt"), userId, parentId);
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        Timestamp expiredAt = getExpiredAt(userId, parentId);
        return expiredAt == null ? null : getDateFormat().format(expiredAt);
    }

    @Override
    public int setExpiredAt(int userId, int parentId, long time, int updatedBy) {
        if (userId < 1 || parentId < 1 || updatedBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.UPDATE_EXPIRED_AT.getName(), userId, parentId, expiredAt, updatedBy);
    }

    @Override
    public int getCreatedBy(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, result -> result.getInt("createdBy"), userId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("createdAt"), userId, parentId);
    }

    @Override
    public String getCreatedDate(int userId, int parentId) {
        Timestamp createdAt = getCreatedAt(userId, parentId);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, result -> result.getInt("updatedBy"), userId, parentId);
    }

    @Override
    public Timestamp getUpdatedAt(int userId, int parentId) {
        if (userId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, result -> result.getTimestamp("updatedAt"), userId, parentId);
    }

    @Override
    public String getUpdatedDate(int userId, int parentId) {
        Timestamp updatedAt = getUpdatedAt(userId, parentId);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    @Override
    public int loadExpired() {
        return database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("userParent_create", "p_userId INT, p_parentId INT, p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (userId, parentId, expiredAt, createdBy, updatedBy) VALUES (p_userId, p_parentId, p_expiredAt, p_createdBy, p_updatedBy);"),
        REMOVE_PARENT("userParent_remove", "p_userId INT, p_parentId INT", "DELETE FROM [TABLE] WHERE userId=p_userId AND parentId=p_parentId;"),
        CLEAR_PARENT("userParent_clear", "p_userId INT", "DELETE FROM [TABLE] WHERE userId=p_userId;"),
        CLEAR_OTHER_PARENT("userParent_clearOther", "p_parentId INT",
                "DELETE FROM [TABLE] WHERE parentId=p_parentId;"),
        GET_DATA("userParent_getData", "p_userId INT, p_parentId INT", "SELECT * FROM [TABLE] WHERE userId=p_userId AND parentId=p_parentId;"),
        GET_ACTIVE_PARENT("userParent_getActive", "p_userId INT",
                "SELECT parentId FROM [TABLE] WHERE userId=p_userId AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP());"),
        UPDATE_EXPIRED_AT("userParent_updateExpiredAt", "p_userId INT, p_parentId INT, p_expiredAt DATETIME, p_updatedBy INT",
                "UPDATE [TABLE] SET expiredAt=p_expiredAt, updatedBy=p_updatedBy WHERE userId=p_userId AND parentId=p_parentId;"),
        UPDATE_EXPIRED("userParent_updateExpired", "",
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

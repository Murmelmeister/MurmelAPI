package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

/**
 * GroupParentProvider is a class that provides methods to manage group parents in the database.
 * It implements the GroupParent interface and uses the Database class to interact with the database.
 */
public final class GroupParentProvider implements GroupParent {
    private static final String TABLE_NAME = "group_parent";

    private final Database database;

    public GroupParentProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "groupId INT, parentId INT, " +
                                         "PRIMARY KEY (groupId, parentId), " +
                                         "FOREIGN KEY (groupId) REFERENCES groups(id), " +
                                         "FOREIGN KEY (parentId) REFERENCES groups(id), " +
                                         "expiredAt DATETIME, " +
                                         "createdBy INT, FOREIGN KEY (createdBy) REFERENCES users(id), " +
                                         "createdAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "updatedBy INT, FOREIGN KEY (updatedBy) REFERENCES users(id), " +
                                         "updatedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return groupId > 0 && parentId > 0 && database.existsCallable(Procedure.GET_DATA.getName(), groupId, parentId);
    }

    @Override
    public int addParent(int groupId, int parentId, long time, int createdBy) {
        if (groupId < 1 || parentId < 1 || createdBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.CREATE.getName(), groupId, parentId, expiredAt, createdBy, createdBy);
    }

    @Override
    public int removeParent(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return 0;
        return database.updateCallable(Procedure.REMOVE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public int clearParent(int groupId) {
        if (groupId < 1) return 0;
        return database.updateCallable(Procedure.CLEAR_PARENT.getName(), groupId);
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        if (groupId < 1) return null;
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), result -> result.getInt("parentId"), groupId);
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        if (groupId < 1) return null;
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), result -> group.getGroupName(result.getInt("parentId")), groupId);
    }

    @Override
    public Timestamp getExpiredAt(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("expiredAt"), groupId, parentId);
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        Timestamp expiredAt = getExpiredAt(groupId, parentId);
        return expiredAt == null ? null : getDateFormat().format(expiredAt);
    }

    @Override
    public int setExpiredAt(int groupId, int parentId, long time, int updatedBy) {
        if (groupId < 1 || parentId < 1 || updatedBy == -2) return 0;
        Timestamp expiredAt = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.updateCallable(Procedure.UPDATE_EXPIRED_AT.getName(), groupId, parentId, expiredAt, updatedBy);
    }

    @Override
    public int getCreatedBy(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("createdBy"), groupId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("createdAt"), groupId, parentId);
    }

    @Override
    public String getCreatedDate(int groupId, int parentId) {
        Timestamp createdAt = getCreatedAt(groupId, parentId);
        return createdAt == null ? null : getDateFormat().format(createdAt);
    }

    @Override
    public int getUpdatedBy(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return -2;
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("updatedBy"), groupId, parentId);
    }

    @Override
    public Timestamp getUpdatedAt(int groupId, int parentId) {
        if (groupId < 1 || parentId < 1) return null;
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("updatedAt"), groupId, parentId);
    }

    @Override
    public String getUpdatedDate(int groupId, int parentId) {
        Timestamp updatedAt = getUpdatedAt(groupId, parentId);
        return updatedAt == null ? null : getDateFormat().format(updatedAt);
    }

    @Override
    public int loadExpired() {
        return database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("groupParent_create", "p_groupId INT, p_parentId INT, p_expiredAt DATETIME, p_createdBy INT, p_updatedBy INT",
                "INSERT INTO [TABLE] (groupId, parentId, expiredAt, createdBy, updatedBy) VALUES (p_groupId, p_parentId, p_expiredAt, p_createdBy, p_updatedBy);"),
        REMOVE_PARENT("groupParent_remove", "p_groupId INT, p_parentId INT", "DELETE FROM [TABLE] WHERE groupId=p_groupId AND parentId=p_parentId;"),
        CLEAR_PARENT("groupParent_clear", "p_groupId INT", "DELETE FROM [TABLE] WHERE groupId=p_groupId;"),
        GET_DATA("groupParent_getData", "p_groupId INT, p_parentId INT", "SELECT * FROM [TABLE] WHERE groupId=p_groupId AND parentId=p_parentId;"),
        GET_ACTIVE_PARENT("groupParent_getActiveParent", "p_groupId INT",
                "SELECT parentId FROM [TABLE] WHERE groupId=p_groupId AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP());"),
        UPDATE_EXPIRED_AT("groupParent_updateExpiredAt", "p_groupId INT, p_parentId INT, p_expiredAt DATETIME, p_updatedBy INT",
                "UPDATE [TABLE] SET expiredAt=p_expiredAt, updatedBy=p_updatedBy WHERE groupId=p_groupId AND parentId=p_parentId;"),
        UPDATE_EXPIRED("groupParent_updateExpired", "",
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

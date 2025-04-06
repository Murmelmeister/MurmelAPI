package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class UserParentProvider implements UserParent {
    private static final String TABLE_NAME = "UserParent";
    private final Database database;

    public UserParentProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT, ParentID INT, PRIMARY KEY (UserID, ParentID), " +
                                         "FOREIGN KEY (UserID) REFERENCES Users(ID), " +
                                         "FOREIGN KEY (ParentID) REFERENCES Groups(ID), " +
                                         "ExpiredAt DATETIME, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int userId, int parentId) {
        return database.existsCallable(Procedure.GET_DATA.getName(), userId, parentId);
    }

    @Override
    public void addParent(int executorId, int userId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.CREATE.getName(), userId, parentId, expired, executorId, executorId);
    }

    @Override
    public void removeParent(int userId, int parentId) {
        database.updateCallable(Procedure.REMOVE_PARENT.getName(), userId, parentId);
    }

    @Override
    public void clearParent(int userId) {
        database.updateCallable(Procedure.CLEAR_PARENT.getName(), userId);
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), new LinkedList<>(), resultSet -> resultSet.getInt("ParentID"), userId);
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), new LinkedList<>(), resultSet -> group.getName(resultSet.getInt("ParentID")), userId);
    }

    @Override
    public int getHighestPriority(Group group, int userId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), new LinkedList<>(), resultSet -> group.getPriority(resultSet.getInt("ParentID")), userId)
                .stream().max(Comparator.naturalOrder()).orElse(-1);
    }

    @Override
    public Timestamp getExpiredAt(int userId, int parentId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("ExpiredAt"), userId, parentId);
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        Timestamp time = getExpiredAt(userId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void setExpiredAt(int executorId, int userId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.SET_EXPIRED_AT.getName(), userId, parentId, expired, executorId);
    }

    @Override
    public int getCreatedBy(int userId, int parentId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), userId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int userId, int parentId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), userId, parentId);
    }

    @Override
    public String getCreatedDate(int userId, int parentId) {
        Timestamp time = getCreatedAt(userId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public int getModifiedBy(int userId, int parentId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), userId, parentId);
    }

    @Override
    public Timestamp getModifiedAt(int userId, int parentId) {
        return database.queryCallable(Procedure.GET_DATA.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), userId, parentId);
    }

    @Override
    public String getModifiedDate(int userId, int parentId) {
        Timestamp time = getModifiedAt(userId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void loadExpired() {
        database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("UserParent_Create", "uid INT, pid INT, et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (UserID,ParentID,ExpiredAt,CreatedBy,ModifiedBy) VALUES (uid,pid,et,created,modified);"),
        REMOVE_PARENT("UserParent_Remove", "uid INT, pid INT", "DELETE FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        CLEAR_PARENT("UserParent_Clear", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_DATA("UserParent_GetData", "uid INT, pid INT", "SELECT * FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        GET_ACTIVE_PARENT("UserParent_GetActive", "uid INT",
                "SELECT ParentID FROM [TABLE] WHERE UserID=uid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP());"),
        SET_EXPIRED_AT("UserParent_SetExpiredAt", "uid INT, pid INT, et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE UserID=uid AND ParentID=pid;"),
        UPDATE_EXPIRED("UserParent_UpdateExpired", "", "DELETE FROM [TABLE] WHERE ExpiredAt IS NOT NULL AND ExpiredAt <= CURRENT_TIMESTAMP();");
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

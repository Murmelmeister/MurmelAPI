package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

import static de.murmelmeister.murmelapi.MurmelAPI.getDateFormat;

public final class GroupParentProvider implements GroupParent {
    private static final String TABLE_NAME = "GroupParent";
    private final Database database;

    public GroupParentProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "GroupID INT, ParentID INT, PRIMARY KEY (GroupID, ParentID), " +
                                         "FOREIGN KEY (GroupID) REFERENCES Groups(ID), " +
                                         "FOREIGN KEY (ParentID) REFERENCES Groups(ID), " +
                                         "ExpiredAt DATETIME, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return database.existsCallable(Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public void addParent(int executorId, int groupId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.CREATE.getName(), groupId, parentId, expired, executorId, executorId);
    }

    @Override
    public void removeParent(int groupId, int parentId) {
        database.updateCallable(Procedure.REMOVE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public void clearParent(int groupId) {
        database.updateCallable(Procedure.CLEAR_PARENT.getName(), groupId);
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), new LinkedList<>(), resultSet -> resultSet.getInt("ParentID"), groupId);
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        return database.queryListCallable(Procedure.GET_ACTIVE_PARENT.getName(), new LinkedList<>(), resultSet -> group.getName(resultSet.getInt("ParentID")), groupId);
    }

    @Override
    public Timestamp getExpiredAt(int groupId, int parentId) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("ExpiredAt"), groupId, parentId);
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        Timestamp time = getExpiredAt(groupId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void setExpiredAt(int executorId, int groupId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        database.updateCallable(Procedure.SET_EXPIRED_AT.getName(), groupId, parentId, expired, executorId);
    }

    @Override
    public int getCreatedBy(int groupId, int parentId) {
        return database.queryCallable(Procedure.GET_ALL.getName(), -2, resultSet -> resultSet.getInt("CreatedBy"), groupId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, int parentId) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("CreatedAt"), groupId, parentId);
    }

    @Override
    public String getCreatedDate(int groupId, int parentId) {
        Timestamp time = getCreatedAt(groupId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public int getModifiedBy(int groupId, int parentId) {
        return database.queryCallable(Procedure.GET_ALL.getName(), -2, resultSet -> resultSet.getInt("ModifiedBy"), groupId, parentId);
    }

    @Override
    public Timestamp getModifiedAt(int groupId, int parentId) {
        return database.queryCallable(Procedure.GET_ALL.getName(), null, resultSet -> resultSet.getTimestamp("ModifiedAt"), groupId, parentId);
    }

    @Override
    public String getModifiedDate(int groupId, int parentId) {
        Timestamp time = getModifiedAt(groupId, parentId);
        return time == null ? "never" : getDateFormat().format(time);
    }

    @Override
    public void loadExpired() {
        database.updateCallable(Procedure.UPDATE_EXPIRED.getName());
    }

    private enum Procedure {
        CREATE("GroupParent_Create", "gid INT, pid INT, et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,ParentID,ExpiredAt,CreatedBy,ModifiedBy) VALUES (gid,pid,et,created,modified);"),
        REMOVE_PARENT("GroupParent_Remove", "gid INT, pid INT", "DELETE FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        CLEAR_PARENT("GroupParent_Clear", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GET_ALL("GroupParent_GetAll", "gid INT, pid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        GET_ACTIVE_PARENT("GroupParent_GetActive", "gid INT",
                "SELECT ParentID FROM [TABLE] WHERE GroupID=gid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP());"),
        SET_EXPIRED_AT("GroupParent_SetExpiredAt", "gid INT, pid INT, et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE GroupID=gid AND ParentID=pid;"),
        UPDATE_EXPIRED("GroupParent_UpdateExpired", "", "DELETE FROM [TABLE] WHERE ExpiredAt IS NOT NULL AND ExpiredAt <= CURRENT_TIMESTAMP();");
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

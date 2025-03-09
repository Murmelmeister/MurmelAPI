package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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
                                         "ExpiredTime BIGINT, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return database.exists(Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public void addParent(int executorId, int groupId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.CREATE.getName(), groupId, parentId, expired, executorId, executorId);
    }

    @Override
    public void removeParent(int groupId, int parentId) {
        database.callUpdate(Procedure.DELETE_PARENT.getName(), groupId, parentId);
    }

    @Override
    public void clearParent(int groupId) {
        database.callUpdate(Procedure.DELETE_GROUP.getName(), groupId);
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        return database.queryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_BY_USER.getName(), groupId);
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        return getParentIds(groupId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public long getExpiredTime(int groupId, int parentId) {
        return database.query(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        long time = getExpiredTime(groupId, parentId);
        return time == -1 ? "never" : MurmelAPI.getDateFormat().format(time);
    }

    @Override
    public String setExpiredTime(int executorId, int groupId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.SET_EXPIRED_TIME.getName(), groupId, parentId, expired, executorId);
        return getExpiredDate(groupId, parentId);
    }

    @Override
    public boolean isExpired(int groupId, int parentId) {
        return database.query((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), groupId, parentId) == 1;
    }

    @Override
    public int getCreatedBy(int groupId, int parentId) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int groupId, int parentId) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public int getModifiedBy(int groupId, int parentId) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public Timestamp getModifiedAt(int groupId, int parentId) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    @Override
    public void loadExpired(Group group) {
        List<Integer> groupIds = group.getUniqueIds();
        for (int i = groupIds.size() - 1; i >= 0; i--) {
            int groupId = groupIds.get(i);
            List<Integer> parentIds = getParentIds(groupId);

            for (int j = parentIds.size() - 1; j >= 0; j--) {
                int parentId = parentIds.get(j);
                if (isExpired(groupId, parentId))
                    removeParent(groupId, parentId);
            }
        }
    }

    private enum Procedure {
        CREATE("GroupParent_Create", "gid INT, pid INT, et BIGINT, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,ParentID,ExpiredTime,CreatedBy,ModifiedBy) VALUES (gid,pid,et,created,modified);"),
        DELETE_PARENT("GroupParent_DeleteParent", "gid INT, pid INT", "DELETE FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        DELETE_GROUP("GroupParent_DeleteGroup", "gid INT", "DELETE FROM [TABLE] WHERE GroupID=gid;"),
        GET_ALL("GroupParent_GetAll", "gid INT, pid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        GET_BY_USER("GroupParent_GetByUser", "gid INT", "SELECT ParentID FROM [TABLE] WHERE GroupID=gid;"),
        SET_EXPIRED_TIME("GroupParent_SetExpiredTime", "gid INT, pid INT, et BIGINT, modified INT",
                "UPDATE [TABLE] SET ExpiredTime=et, ModifiedBy=modified WHERE GroupID=gid AND ParentID=pid;"),
        IS_EXPIRED("GroupParent_IsExpired", "gid INT, pid INT",
                "SELECT IF(ExpiredTime = -1, 0, ExpiredTime <= CURRENT_TIMESTAMP()) AS Expired FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;");
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

package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
                                         "ExpiredTime BIGINT, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    @Override
    public boolean existsParent(int userId, int parentId) {
        return database.exists(Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public void addParent(int executorId, int userId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.CREATE.getName(), userId, parentId, expired, executorId, executorId);
    }

    @Override
    public void removeParent(int userId, int parentId) {
        database.callUpdate(Procedure.DELETE_PARENT.getName(), userId, parentId);
    }

    @Override
    public void clearParent(int userId) {
        database.callUpdate(Procedure.DELETE_USER.getName(), userId);
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        return database.queryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_BY_USER.getName(), userId);
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        return getParentIds(userId).parallelStream().map(group::getName).collect(Collectors.toList());
    }

    @Override
    public int getHighestPriority(Group group, int userId) {
        return getParentIds(userId)
                .parallelStream()
                .map(group::getPriority)
                .max(Comparator.naturalOrder())
                .orElse(-1);
    }

    @Override
    public long getExpiredTime(int userId, int parentId) {
        return database.query(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        long time = getExpiredTime(userId, parentId);
        return time == -1 ? "never" : MurmelAPI.getDateFormat().format(time);
    }

    @Override
    public String setExpiredTime(int executorId, int userId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        database.callUpdate(Procedure.SET_EXPIRED_TIME.getName(), userId, parentId, expired, executorId);
        return getExpiredDate(userId, parentId);
    }

    @Override
    public boolean isExpired(int userId, int parentId) {
        return database.query((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), userId, parentId) == 1;
    }

    @Override
    public int getCreatedBy(int userId, int parentId) {
        return database.query(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public Timestamp getCreatedAt(int userId, int parentId) {
        return database.query(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public int getModifiedBy(int userId, int parentId) {
        return database.query(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public Timestamp getModifiedAt(int userId, int parentId) {
        return database.query(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    @Override
    public void loadExpired(User user) {
        List<UUID> userIds = user.getUniqueIds();
        for (int i = userIds.size() - 1; i >= 0; i--) {
            int userId = user.getId(userIds.get(i));
            List<Integer> parentIds = getParentIds(userId);

            for (int j = parentIds.size() - 1; j >= 0; j--) {
                int parentId = parentIds.get(j);
                if (isExpired(userId, parentId))
                    removeParent(userId, parentId);
            }
        }
    }

    private enum Procedure {
        CREATE("UserParent_Create", "uid INT, pid INT, et BIGINT, created INT, modified INT",
                "INSERT INTO [TABLE] (UserID,ParentID,ExpiredTime,CreatedBy,ModifiedBy) VALUES (uid,pid,et,created,modified);"),
        DELETE_PARENT("UserParent_DeleteParent", "uid INT, pid INT", "DELETE FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        DELETE_USER("UserParent_DeleteUser", "uid INT", "DELETE FROM [TABLE] WHERE UserID=uid;"),
        GET_ALL("UserParent_GetAll", "uid INT, pid INT", "SELECT * FROM [TABLE] WHERE UserID=uid AND ParentID=pid;"),
        GET_BY_USER("UserParent_GetByUser", "uid INT", "SELECT ParentID FROM [TABLE] WHERE UserID=uid;"),
        SET_EXPIRED_TIME("UserParent_SetExpiredTime", "uid INT, pid INT, et BIGINT, modified INT",
                "UPDATE [TABLE] SET ExpiredTime=et, ModifiedBy=modified WHERE UserID=uid AND ParentID=pid;"),
        IS_EXPIRED("UserParent_IsExpired", "uid INT, pid INT",
                "SELECT IF(ExpiredTime = -1, 0, ExpiredTime <= CURRENT_TIMESTAMP()) AS Expired FROM [TABLE] WHERE UserID=uid AND ParentID=pid;");
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

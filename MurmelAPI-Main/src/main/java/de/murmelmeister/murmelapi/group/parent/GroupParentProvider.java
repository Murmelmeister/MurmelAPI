package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.MurmelAPI;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    // === Asynchrone API-Methoden ===

    public CompletableFuture<Boolean> existsParentAsync(int groupId, int parentId) {
        return database.asyncExists(Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Void> addParentAsync(int executorId, int groupId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        return database.asyncUpdate(Procedure.CREATE.getName(), groupId, parentId, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removeParentAsync(int groupId, int parentId) {
        return database.asyncUpdate(Procedure.DELETE_PARENT.getName(), groupId, parentId);
    }

    public CompletableFuture<Void> clearParentAsync(int groupId) {
        return database.asyncUpdate(Procedure.DELETE_GROUP.getName(), groupId);
    }

    public CompletableFuture<List<Integer>> getParentIdsAsync(int groupId) {
        return database.asyncQueryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_BY_USER.getName(), groupId);
    }

    public CompletableFuture<List<String>> getParentNamesAsync(Group group, int groupId) {
        return getParentIdsAsync(groupId).thenApply(ids ->
                ids.parallelStream().map(group::getName).collect(Collectors.toList())
        );
    }

    public CompletableFuture<Long> getExpiredTimeAsync(int groupId, int parentId) {
        return database.asyncQuery(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<String> getExpiredDateAsync(int groupId, int parentId) {
        return getExpiredTimeAsync(groupId, parentId)
                .thenApply(time -> time == -1 ? "never" : MurmelAPI.getDateFormat().format(time));
    }

    public CompletableFuture<String> setExpiredTimeAsync(int executorId, int groupId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        return database.asyncUpdate(Procedure.SET_EXPIRED_TIME.getName(), groupId, parentId, expired, executorId)
                .thenCompose(v -> getExpiredDateAsync(groupId, parentId));
    }

    public CompletableFuture<Boolean> isExpiredAsync(int groupId, int parentId) {
        return database.asyncQuery((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), groupId, parentId)
                .thenApply(b -> b == 1);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int groupId, int parentId) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int groupId, int parentId) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int groupId, int parentId) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int groupId, int parentId) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Void> loadExpiredAsync(Group group) {
        return CompletableFuture.runAsync(() -> {
            List<Integer> groupIds = group.getUniqueIds();
            for (int i = groupIds.size() - 1; i >= 0; i--) {
                int groupId = groupIds.get(i);
                List<Integer> parentIds = getParentIdsAsync(groupId).join();
                for (int j = parentIds.size() - 1; j >= 0; j--) {
                    int parentId = parentIds.get(j);
                    if (isExpiredAsync(groupId, parentId).join())
                        removeParentAsync(groupId, parentId).join();
                }
            }
        });
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsParent(int groupId, int parentId) {
        return existsParentAsync(groupId, parentId).join();
    }

    @Override
    public void addParent(int executorId, int groupId, int parentId, long time) {
        addParentAsync(executorId, groupId, parentId, time).join();
    }

    @Override
    public void removeParent(int groupId, int parentId) {
        removeParentAsync(groupId, parentId).join();
    }

    @Override
    public void clearParent(int groupId) {
        clearParentAsync(groupId).join();
    }

    @Override
    public List<Integer> getParentIds(int groupId) {
        return getParentIdsAsync(groupId).join();
    }

    @Override
    public List<String> getParentNames(Group group, int groupId) {
        return getParentNamesAsync(group, groupId).join();
    }

    @Override
    public long getExpiredTime(int groupId, int parentId) {
        return getExpiredTimeAsync(groupId, parentId).join();
    }

    @Override
    public String getExpiredDate(int groupId, int parentId) {
        return getExpiredDateAsync(groupId, parentId).join();
    }

    @Override
    public String setExpiredTime(int executorId, int groupId, int parentId, long time) {
        return setExpiredTimeAsync(executorId, groupId, parentId, time).join();
    }

    @Override
    public boolean isExpired(int groupId, int parentId) {
        return isExpiredAsync(groupId, parentId).join();
    }

    @Override
    public int getCreatedBy(int groupId, int parentId) {
        return getCreatedByAsync(groupId, parentId).join();
    }

    @Override
    public Timestamp getCreatedAt(int groupId, int parentId) {
        return getCreatedAtAsync(groupId, parentId).join();
    }

    @Override
    public int getModifiedBy(int groupId, int parentId) {
        return getModifiedByAsync(groupId, parentId).join();
    }

    @Override
    public Timestamp getModifiedAt(int groupId, int parentId) {
        return getModifiedAtAsync(groupId, parentId).join();
    }

    @Override
    public void loadExpired(Group group) {
        loadExpiredAsync(group).join();
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

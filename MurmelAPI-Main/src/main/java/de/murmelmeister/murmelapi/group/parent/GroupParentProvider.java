package de.murmelmeister.murmelapi.group.parent;

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
                                         "ExpiredAt DATETIME, " +
                                         "Archived TINYINT(1) DEFAULT 0, " +
                                         "ArchivedAt DATETIME, " +
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
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.CREATE.getName(), groupId, parentId, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removeParentAsync(int executorId, int groupId, int parentId) {
        return database.asyncUpdate(Procedure.REMOVE_PARENT.getName(), groupId, parentId, executorId);
    }

    public CompletableFuture<Void> clearParentAsync(int executorId, int groupId) {
        return database.asyncUpdate(Procedure.CLEAR_PARENT.getName(), groupId, executorId);
    }

    public CompletableFuture<List<Integer>> getParentIdsAsync(int groupId) {
        return database.asyncQueryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_ACTIVE_PARENT.getName(), groupId);
    }

    public CompletableFuture<List<String>> getParentNamesAsync(Group group, int groupId) {
        return getParentIdsAsync(groupId).thenApply(ids ->
                ids.parallelStream().map(group::getName).collect(Collectors.toList())
        );
    }

    public CompletableFuture<Timestamp> getExpiredAtAsync(int groupId, int parentId) {
        return database.asyncQuery(null, "ExpiredAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, parentId);
    }

    public CompletableFuture<Void> setExpiredAtAsync(int executorId, int groupId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.SET_EXPIRED_AT.getName(), groupId, parentId, expired, executorId);
    }

    public CompletableFuture<Boolean> isExpiredAsync(int groupId, int parentId) {
        return database.asyncQuery((byte) 0, "Archived", byte.class, Procedure.IS_EXPIRED.getName(), groupId, parentId)
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

    public CompletableFuture<Void> loadExpiredAsync() {
        /*return CompletableFuture.runAsync(() -> {
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
        });*/
        return database.asyncUpdate(Procedure.UPDATE_ARCHIVED.getName());
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
    public void removeParent(int executorId, int groupId, int parentId) {
        removeParentAsync(executorId, groupId, parentId).join();
    }

    @Override
    public void clearParent(int executorId, int groupId) {
        clearParentAsync(executorId, groupId).join();
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
    public Timestamp getExpiredAt(int groupId, int parentId) {
        return getExpiredAtAsync(groupId, parentId).join();
    }

    @Override
    public void setExpiredAt(int executorId, int groupId, int parentId, long time) {
        setExpiredAtAsync(executorId, groupId, parentId, time).join();
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
    public void loadExpired() {
        loadExpiredAsync().join();
    }

    private enum Procedure {
        CREATE("GroupParent_Create", "gid INT, pid INT, et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,ParentID,ExpiredAt,CreatedBy,ModifiedBy) VALUES (gid,pid,et,created,modified);"),
        REMOVE_PARENT("GroupParent_Remove", "gid INT, pid INT, modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE GroupID=gid AND ParentID=pid AND Archived=0;"),
        CLEAR_PARENT("GroupParent_Clear", "gid INT, modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE GroupID=gid AND Archived=0;"),
        GET_ALL("GroupParent_GetAll", "gid INT, pid INT", "SELECT * FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        GET_ACTIVE_PARENT("GroupParent_GetActive", "gid INT",
                "SELECT ParentID FROM [TABLE] WHERE GroupID=gid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP()) AND Archived=0;"),
        SET_EXPIRED_AT("GroupParent_SetExpiredAt", "gid INT, pid INT, et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE GroupID=gid AND ParentID=pid;"),
        IS_EXPIRED("GroupParent_IsExpired", "gid INT, pid INT",
                "SELECT Archived FROM [TABLE] WHERE GroupID=gid AND ParentID=pid;"),
        UPDATE_ARCHIVED("GroupParent_UpdateArchived", "",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=-1 WHERE Archived=0 AND ExpiredAt IS NOT NULL AND ExpiredAt <= CURRENT_TIMESTAMP();");
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

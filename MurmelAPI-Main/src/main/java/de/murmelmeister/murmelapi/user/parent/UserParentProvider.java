package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
                                         "ExpiredAt DATETIME, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    // === Asynchrone API-Methoden ===

    public CompletableFuture<Boolean> existsParentAsync(int userId, int parentId) {
        return database.asyncExists(Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Void> addParentAsync(int executorId, int userId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.CREATE.getName(), userId, parentId, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removeParentAsync(int userId, int parentId) {
        return database.asyncUpdate(Procedure.REMOVE_PARENT.getName(), userId, parentId);
    }

    public CompletableFuture<Void> clearParentAsync(int userId) {
        return database.asyncUpdate(Procedure.CLEAR_PARENT.getName(), userId);
    }

    public CompletableFuture<List<Integer>> getParentIdsAsync(int userId) {
        return database.asyncQueryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_ACTIVE_PARENT.getName(), userId);
    }

    public CompletableFuture<List<String>> getParentNamesAsync(Group group, int userId) {
        return getParentIdsAsync(userId).thenApply(ids ->
                ids.parallelStream()
                        .map(group::getName)
                        .collect(Collectors.toList())
        );
    }

    public CompletableFuture<Integer> getHighestPriorityAsync(Group group, int userId) {
        return getParentIdsAsync(userId).thenApply(ids ->
                ids.parallelStream()
                        .map(group::getPriority)
                        .max(Comparator.naturalOrder())
                        .orElse(-1)
        );
    }

    public CompletableFuture<Timestamp> getExpiredAtAsync(int userId, int parentId) {
        return database.asyncQuery(null, "ExpiredAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Void> setExpiredAtAsync(int executorId, int userId, int parentId, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.SET_EXPIRED_AT.getName(), userId, parentId, expired, executorId);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int userId, int parentId) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int userId, int parentId) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int userId, int parentId) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int userId, int parentId) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, parentId);
    }

    public CompletableFuture<Void> loadExpiredAsync() {
        /*return CompletableFuture.runAsync(() -> {
            List<UUID> userIds = user.getUniqueIds();
            for (int i = userIds.size() - 1; i >= 0; i--) {
                int userId = user.getId(userIds.get(i));
                List<Integer> parentIds = getParentIdsAsync(userId).join();
                for (int j = parentIds.size() - 1; j >= 0; j--) {
                    int parentId = parentIds.get(j);
                    if (isExpiredAsync(userId, parentId).join())
                        removeParentAsync(userId, parentId).join();
                }
            }
        });*/
        return database.asyncUpdate(Procedure.UPDATE_EXPIRED.getName());
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsParent(int userId, int parentId) {
        return existsParentAsync(userId, parentId).join();
    }

    @Override
    public void addParent(int executorId, int userId, int parentId, long time) {
        addParentAsync(executorId, userId, parentId, time).join();
    }

    @Override
    public void removeParent(int userId, int parentId) {
        removeParentAsync(userId, parentId).join();
    }

    @Override
    public void clearParent(int userId) {
        clearParentAsync(userId).join();
    }

    @Override
    public List<Integer> getParentIds(int userId) {
        return getParentIdsAsync(userId).join();
    }

    @Override
    public List<String> getParentNames(Group group, int userId) {
        return getParentNamesAsync(group, userId).join();
    }

    @Override
    public int getHighestPriority(Group group, int userId) {
        return getHighestPriorityAsync(group, userId).join();
    }

    @Override
    public Timestamp getExpiredAt(int userId, int parentId) {
        return getExpiredAtAsync(userId, parentId).join();
    }

    @Override
    public void setExpiredAt(int executorId, int userId, int parentId, long time) {
        setExpiredAtAsync(executorId, userId, parentId, time).join();
    }

    @Override
    public int getCreatedBy(int userId, int parentId) {
        return getCreatedByAsync(userId, parentId).join();
    }

    @Override
    public Timestamp getCreatedAt(int userId, int parentId) {
        return getCreatedAtAsync(userId, parentId).join();
    }

    @Override
    public int getModifiedBy(int userId, int parentId) {
        return getModifiedByAsync(userId, parentId).join();
    }

    @Override
    public Timestamp getModifiedAt(int userId, int parentId) {
        return getModifiedAtAsync(userId, parentId).join();
    }

    @Override
    public void loadExpired() {
        loadExpiredAsync().join();
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

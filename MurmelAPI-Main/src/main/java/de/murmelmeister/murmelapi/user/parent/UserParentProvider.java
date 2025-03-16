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
                                         "ExpiredTime BIGINT, " +
                                         "CreatedBy INT, FOREIGN KEY (CreatedBy) REFERENCES Users(ID), " +
                                         "CreatedAt DATETIME DEFAULT CURRENT_TIMESTAMP(), " +
                                         "ModifiedBy INT, FOREIGN KEY (ModifiedBy) REFERENCES Users(ID), " +
                                         "ModifiedAt DATETIME DEFAULT CURRENT_TIMESTAMP() ON UPDATE CURRENT_TIMESTAMP()");
        Procedure.loadAll(database);
    }

    // === Asynchrone API-Methoden ===

    public CompletableFuture<Boolean> existsParentAsync(int userId, int parentId) {
        return database.asyncExists(Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<Void> addParentAsync(int executorId, int userId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        return database.asyncUpdate(Procedure.CREATE.getName(), userId, parentId, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removeParentAsync(int userId, int parentId) {
        return database.asyncUpdate(Procedure.DELETE_PARENT.getName(), userId, parentId);
    }

    public CompletableFuture<Void> clearParentAsync(int userId) {
        return database.asyncUpdate(Procedure.DELETE_USER.getName(), userId);
    }

    public CompletableFuture<List<Integer>> getParentIdsAsync(int userId) {
        return database.asyncQueryList(new LinkedList<>(), "ParentID", int.class, Procedure.GET_BY_USER.getName(), userId);
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

    public CompletableFuture<Long> getExpiredTimeAsync(int userId, int parentId) {
        return database.asyncQuery(-2L, "ExpiredTime", long.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<String> getExpiredDateAsync(int userId, int parentId) {
        return getExpiredTimeAsync(userId, parentId)
                .thenApply(time -> time == -1 ? "never" : MurmelAPI.getDateFormat().format(time));
    }

    public CompletableFuture<String> setExpiredTimeAsync(int executorId, int userId, int parentId, long time) {
        long expired = time == -1 ? time : System.currentTimeMillis() + time;
        return database.asyncUpdate(Procedure.SET_EXPIRED_TIME.getName(), userId, parentId, expired, executorId)
                .thenCompose(v -> getExpiredDateAsync(userId, parentId));
    }

    public CompletableFuture<Boolean> isExpiredAsync(int userId, int parentId) {
        return database.asyncQuery((byte) 0, "Expired", byte.class, Procedure.IS_EXPIRED.getName(), userId, parentId)
                .thenApply(b -> b == 1);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int userId, int parentId) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int userId, int parentId) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int userId, int parentId) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int userId, int parentId) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), userId, parentId);
    }

    public CompletableFuture<Void> loadExpiredAsync(User user) {
        return CompletableFuture.runAsync(() -> {
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
        });
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
    public long getExpiredTime(int userId, int parentId) {
        return getExpiredTimeAsync(userId, parentId).join();
    }

    @Override
    public String getExpiredDate(int userId, int parentId) {
        return getExpiredDateAsync(userId, parentId).join();
    }

    @Override
    public String setExpiredTime(int executorId, int userId, int parentId, long time) {
        return setExpiredTimeAsync(executorId, userId, parentId, time).join();
    }

    @Override
    public boolean isExpired(int userId, int parentId) {
        return isExpiredAsync(userId, parentId).join();
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
    public void loadExpired(User user) {
        loadExpiredAsync(user).join();
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

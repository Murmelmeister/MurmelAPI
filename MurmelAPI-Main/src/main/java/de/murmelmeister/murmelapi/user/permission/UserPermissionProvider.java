package de.murmelmeister.murmelapi.user.permission;

import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class UserPermissionProvider implements UserPermission {
    private static final String TABLE_NAME = "UserPermission";
    private final Database database;

    public UserPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "UserID INT, Permission VARCHAR(200), PRIMARY KEY (UserID, Permission), " +
                                         "FOREIGN KEY (UserID) REFERENCES Users(ID), " +
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

    public CompletableFuture<Boolean> existsPermissionAsync(int userId, String permission) {
        return database.asyncExists(Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Void> addPermissionAsync(int executorId, int userId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.CREATE.getName(), userId, permission, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removePermissionAsync(int executorId, int userId, String permission) {
        return database.asyncUpdate(Procedure.REMOVE_PERMISSION.getName(), userId, permission, executorId);
    }

    public CompletableFuture<Void> clearPermissionAsync(int executorId, int userId) {
        return database.asyncUpdate(Procedure.CLEAR_PERMISSION.getName(), userId, executorId);
    }

    public CompletableFuture<List<String>> getPermissionsAsync(int userId) {
        return database.asyncQueryList(new LinkedList<>(), "Permission", String.class, Procedure.GET_ACTIVE_PERMISSION.getName(), userId);
    }

    public CompletableFuture<Timestamp> getExpiredAtAsync(int userId, String permission) {
        return database.asyncQuery(null, "ExpiredAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Void> setExpiredAtAsync(int executorId, int userId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.SET_EXPIRED_AT.getName(), userId, permission, expired, executorId);
    }

    public CompletableFuture<Boolean> isExpiredAsync(int userId, String permission) {
        return database.asyncQuery((byte) 0, "Archived", byte.class, Procedure.IS_EXPIRED.getName(), userId, permission)
                .thenApply(b -> b == 1);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int userId, String permission) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int userId, String permission) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int userId, String permission) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int userId, String permission) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_DATA.getName(), userId, permission);
    }

    public CompletableFuture<Void> loadExpiredAsync() {
        /*return CompletableFuture.runAsync(() -> {
            List<UUID> userIds = user.getUniqueIds();
            for (int i = userIds.size() - 1; i >= 0; i--) {
                int userId = user.getId(userIds.get(i));
                List<String> permissions = getPermissionsAsync(userId).join();
                for (int j = permissions.size() - 1; j >= 0; j--) {
                    String perm = permissions.get(j);
                    if (isExpiredAsync(userId, perm).join()) {
                        removePermissionAsync(userId, perm).join();
                    }
                }
            }
        });*/
        return database.asyncUpdate(Procedure.UPDATE_ARCHIVED.getName());
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsPermission(int userId, String permission) {
        return existsPermissionAsync(userId, permission).join();
    }

    @Override
    public void addPermission(int executorId, int userId, String permission, long time) {
        addPermissionAsync(executorId, userId, permission, time).join();
    }

    @Override
    public void removePermission(int executorId, int userId, String permission) {
        removePermissionAsync(executorId, userId, permission).join();
    }

    @Override
    public void clearPermission(int executorId, int userId) {
        clearPermissionAsync(executorId, userId).join();
    }

    @Override
    public List<String> getPermissions(int userId) {
        return getPermissionsAsync(userId).join();
    }

    @Override
    public Timestamp getExpiredAt(int userId, String permission) {
        return getExpiredAtAsync(userId, permission).join();
    }

    @Override
    public void setExpiredAt(int executorId, int userId, String permission, long time) {
        setExpiredAtAsync(executorId, userId, permission, time).join();
    }

    @Override
    public boolean isExpired(int userId, String permission) {
        return isExpiredAsync(userId, permission).join();
    }

    @Override
    public int getCreatedBy(int userId, String permission) {
        return getCreatedByAsync(userId, permission).join();
    }

    @Override
    public Timestamp getCreatedAt(int userId, String permission) {
        return getCreatedAtAsync(userId, permission).join();
    }

    @Override
    public int getModifiedBy(int userId, String permission) {
        return getModifiedByAsync(userId, permission).join();
    }

    @Override
    public Timestamp getModifiedAt(int userId, String permission) {
        return getModifiedAtAsync(userId, permission).join();
    }

    @Override
    public void loadExpired() {
        loadExpiredAsync().join();
    }

    private enum Procedure {
        CREATE("UserPermission_Create", "uid INT, perm VARCHAR(200), et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (UserID,Permission,ExpiredAt,CreatedBy,ModifiedBy) VALUES (uid,perm,et,created,modified);"),
        REMOVE_PERMISSION("UserPermission_Remove", "uid INT, perm VARCHAR(200), modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE UserID=uid AND Permission=perm AND Archived=0;"),
        CLEAR_PERMISSION("UserPermission_Clear", "uid INT, modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE UserID=uid AND Archived=0;"),
        GET_DATA("UserPermission_GetData", "uid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        GET_ACTIVE_PERMISSION("UserPermission_GetActive", "uid INT",
                "SELECT Permission FROM [TABLE] WHERE UserID=uid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP()) AND Archived=0;"),
        SET_EXPIRED_AT("UserPermission_UpdateExpiredAt", "uid INT, perm VARCHAR(200), et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE UserID=uid AND Permission=perm;"),
        IS_EXPIRED("UserPermission_IsExpired", "uid INT, perm VARCHAR(200)",
                "SELECT Archived FROM [TABLE] WHERE UserID=uid AND Permission=perm;"),
        UPDATE_ARCHIVED("UserPermission_UpdateArchived", "",
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

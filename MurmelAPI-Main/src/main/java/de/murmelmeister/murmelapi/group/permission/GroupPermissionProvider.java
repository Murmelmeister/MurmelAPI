package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.database.Database;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class GroupPermissionProvider implements GroupPermission {
    private static final String TABLE_NAME = "GroupPermission";
    private final Database database;

    public GroupPermissionProvider(Database database) {
        this.database = database;
    }

    public static void setup(Database database) {
        database.createTable(TABLE_NAME, "GroupID INT, Permission VARCHAR(200), PRIMARY KEY (GroupID, Permission), " +
                                         "FOREIGN KEY (GroupID) REFERENCES Groups(ID), " +
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

    public CompletableFuture<Boolean> existsPermissionAsync(int groupId, String permission) {
        return database.asyncExists(Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Void> addPermissionAsync(int executorId, int groupId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.CREATE.getName(), groupId, permission, expired, executorId, executorId);
    }

    public CompletableFuture<Void> removePermissionAsync(int executorId, int groupId, String permission) {
        return database.asyncUpdate(Procedure.REMOVE_PERMISSION.getName(), groupId, permission, executorId);
    }

    public CompletableFuture<Void> clearPermissionAsync(int executorId, int groupId) {
        return database.asyncUpdate(Procedure.CLEAR_PERMISSION.getName(), groupId, executorId);
    }

    public CompletableFuture<List<String>> getPermissionsAsync(int groupId) {
        return database.asyncQueryList(new LinkedList<>(), "Permission", String.class, Procedure.GET_ACTIVE_PERMISSION.getName(), groupId);
    }

    public CompletableFuture<Timestamp> getExpiredAtAsync(int groupId, String permission) {
        return database.asyncQuery(null, "ExpiredAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Void> setExpiredAtAsync(int executorId, int groupId, String permission, long time) {
        Timestamp expired = time == -1 ? null : new Timestamp(System.currentTimeMillis() + time);
        return database.asyncUpdate(Procedure.SET_EXPIRED_AT.getName(), groupId, permission, expired, executorId);
    }

    public CompletableFuture<Boolean> isExpiredAsync(int groupId, String permission) {
        return database.asyncQuery((byte) 0, "Archived", byte.class, Procedure.IS_EXPIRED.getName(), groupId, permission)
                .thenApply(b -> b == 1);
    }

    public CompletableFuture<Integer> getCreatedByAsync(int groupId, String permission) {
        return database.asyncQuery(-2, "CreatedBy", int.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Timestamp> getCreatedAtAsync(int groupId, String permission) {
        return database.asyncQuery(null, "CreatedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Integer> getModifiedByAsync(int groupId, String permission) {
        return database.asyncQuery(-2, "ModifiedBy", int.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Timestamp> getModifiedAtAsync(int groupId, String permission) {
        return database.asyncQuery(null, "ModifiedAt", Timestamp.class, Procedure.GET_ALL.getName(), groupId, permission);
    }

    public CompletableFuture<Void> loadExpiredAsync() {
        /*return CompletableFuture.runAsync(() -> {
            List<Integer> groupIds = group.getUniqueIds();
            for (int i = groupIds.size() - 1; i >= 0; i--) {
                int groupId = groupIds.get(i);
                List<String> perms = getPermissionsAsync(groupId).join();
                for (int j = perms.size() - 1; j >= 0; j--) {
                    String perm = perms.get(j);
                    long time = getExpiredTimeAsync(groupId, perm).join();
                    if (time != -1 && time <= System.currentTimeMillis()) {
                        removePermissionAsync(groupId, perm).join();
                    }
                }
            }
        });*/
        return database.asyncUpdate(Procedure.UPDATE_ARCHIVED.getName());
    }

    // TODO: Fix fix this method
    public CompletableFuture<List<String>> getAllPermissionsAsync(GroupParent groupParent, int groupId) {
        return CompletableFuture.supplyAsync(() -> {
            Set<String> permissions = new LinkedHashSet<>();
            Stack<Integer> stack = new Stack<>();
            stack.push(groupId);

            while (!stack.isEmpty()) {
                int currentGroupId = stack.pop();
                permissions.addAll(getPermissionsAsync(currentGroupId).join());
                List<Integer> parentIds = groupParent.getParentIds(currentGroupId);
                for (int i = parentIds.size() - 1; i >= 0; i--) {
                    stack.push(parentIds.get(i));
                }
            }
            return new LinkedList<>(permissions);
        });
    }

    // === Synchrone Wrapper (Interface-Implementierung) ===

    @Override
    public boolean existsPermission(int groupId, String permission) {
        return existsPermissionAsync(groupId, permission).join();
    }

    @Override
    public void addPermission(int executorId, int groupId, String permission, long time) {
        addPermissionAsync(executorId, groupId, permission, time).join();
    }

    @Override
    public void removePermission(int executorId, int groupId, String permission) {
        removePermissionAsync(executorId, groupId, permission).join();
    }

    @Override
    public void clearPermission(int executorId, int groupId) {
        clearPermissionAsync(executorId, groupId).join();
    }

    @Override
    public List<String> getPermissions(int groupId) {
        return getPermissionsAsync(groupId).join();
    }

    @Override
    public List<String> getAllPermissions(GroupParent groupParent, int groupId) {
        return getAllPermissionsAsync(groupParent, groupId).join();
    }

    @Override
    public Timestamp getExpiredAt(int groupId, String permission) {
        return getExpiredAtAsync(groupId, permission).join();
    }

    @Override
    public void setExpiredAt(int executorId, int groupId, String permission, long time) {
        setExpiredAtAsync(executorId, groupId, permission, time).join();
    }

    @Override
    public boolean isExpired(int groupId, String permission) {
        return isExpiredAsync(groupId, permission).join();
    }

    @Override
    public int getCreatedBy(int groupId, String permission) {
        return getCreatedByAsync(groupId, permission).join();
    }

    @Override
    public Timestamp getCreatedAt(int groupId, String permission) {
        return getCreatedAtAsync(groupId, permission).join();
    }

    @Override
    public int getModifiedBy(int groupId, String permission) {
        return getModifiedByAsync(groupId, permission).join();
    }

    @Override
    public Timestamp getModifiedAt(int groupId, String permission) {
        return getModifiedAtAsync(groupId, permission).join();
    }

    @Override
    public void loadExpired() {
        loadExpiredAsync().join();
    }

    private enum Procedure {
        CREATE("GroupPermission_Create", "gid INT, perm VARCHAR(200), et DATETIME, created INT, modified INT",
                "INSERT INTO [TABLE] (GroupID,Permission,ExpiredAt,CreatedBy,ModifiedBy) VALUES (gid,perm,et,created,modified);"),
        REMOVE_PERMISSION("GroupPermission_Remove", "gid INT, perm VARCHAR(200), modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE GroupID=gid AND Permission=perm AND Archived=0;"),
        CLEAR_PERMISSION("GroupPermission_Clear", "gid INT, modified INT",
                "UPDATE [TABLE] SET Archived=1, ArchivedAt=CURRENT_TIMESTAMP(), ModifiedBy=modified WHERE [TABLE] WHERE GroupID=gid AND Archived=0;"),
        GET_ALL("GroupPermission_GetAll", "gid INT, perm VARCHAR(200)", "SELECT * FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        GET_ACTIVE_PERMISSION("GroupPermission_GetActive", "gid INT",
                "SELECT Permission FROM [TABLE] WHERE GroupID=gid AND (ExpiredAt IS NULL OR ExpiredAt > CURRENT_TIMESTAMP()) AND Archived=0;"),
        SET_EXPIRED_AT("GroupPermission_SetExpiredAt", "gid INT, perm VARCHAR(200), et DATETIME, modified INT",
                "UPDATE [TABLE] SET ExpiredAt=et, ModifiedBy=modified WHERE GroupID=gid AND Permission=perm;"),
        IS_EXPIRED("GroupPermission_IsExpired", "gid INT, perm VARCHAR(200)",
                "SELECT Archived FROM [TABLE] WHERE GroupID=gid AND Permission=perm;"),
        UPDATE_ARCHIVED("GroupPermission_UpdateArchived", "",
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

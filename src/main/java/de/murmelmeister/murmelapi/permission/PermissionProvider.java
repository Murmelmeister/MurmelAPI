package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.utils.CacheManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * The PermissionProvider class provides methods to manage and check permissions for users and groups.
 * It implements the Permission interface.
 */
public record PermissionProvider(Database database, Group group, User user) implements Permission {
    private static final CacheManager<Integer, Set<String>> CACHE = new CacheManager<>();
    private static final long CACHE_TTL = 15; // 15 minutes

    public static void setup(Database database) {
        Procedure.loadAll(database);
    }

    @Override
    public CompletableFuture<Void> preloadAsync(int userId) {
        return CompletableFuture.runAsync(() -> refresh(userId), database.getExecutor());
    }

    @Override
    public void invalidate(int userId) {
        CACHE.remove(userId);
    }

    @Override
    public List<String> getPermissions(int userId) {
        Set<String> perms = CACHE.get(userId);
        if (perms == null)
            perms = refresh(userId);
        return new LinkedList<>(perms);
    }

    @Override
    public boolean hasPermission(int userId, String permission) {
        Collection<String> permissions = getPermissions(userId);
        if (permissions.isEmpty()) return false;
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;

        int idx = permission.lastIndexOf('.');
        while (idx > 0) {
            String prefix = permission.substring(0, idx) + ".*";
            if (permissions.contains(prefix)) return true;
            idx = permission.lastIndexOf('.', idx - 1);
        }
        return false;
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return hasPermission(user.getId(uuid), permission);
    }

    @Override
    public int loadExpired() {
        return user.loadExpired() + group.loadExpired();
    }

    private Set<String> refresh(int userId) {
        List<String> list = database.queryListCallable(Procedure.GET_USER_PERMISSION.getName(),
                resultSet -> resultSet.getString("permission"), userId);
        Set<String> permissions = new LinkedHashSet<>(list);
        CACHE.put(userId, permissions, CACHE_TTL, TimeUnit.MINUTES);
        return permissions;
    }

    private enum Procedure {
        GET_USER_PERMISSION("getUserPermission", "p_userId INT", """
                     WITH RECURSIVE grp(grp_id) AS (
                         SELECT parentId AS grp_id
                         FROM   user_parent
                         WHERE  userId = p_userId
                           AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP())
                         UNION ALL
                         SELECT gp.parentId
                         FROM   group_parent gp
                         JOIN   grp g ON g.grp_id = gp.groupId
                         WHERE  gp.expiredAt IS NULL OR gp.expiredAt > CURRENT_TIMESTAMP()
                     ),
                    \s
                     perms AS (
                         SELECT permission
                         FROM   user_permission
                         WHERE  userId = p_userId
                           AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP())
                         UNION
                         SELECT permission
                         FROM   group_permission
                         WHERE  groupId IN (SELECT grp_id FROM grp)
                           AND (expiredAt IS NULL OR expiredAt > CURRENT_TIMESTAMP())
                     )
                    \s
                     SELECT DISTINCT permission
                     FROM   perms
                     ORDER BY permission;
                \s"""),
        ;
        private static final Procedure[] VALUES = values();

        private final String name;
        private final String query;

        Procedure(String name, String input, String query) {
            this.name = name;
            this.query = Database.getProcedureQuery(name, input, query);
        }

        public String getName() {
            return name;
        }

        public String getQuery() {
            return query;
        }

        public static void loadAll(Database database) {
            for (Procedure procedure : VALUES)
                database.update(procedure.getQuery());
        }
    }
}

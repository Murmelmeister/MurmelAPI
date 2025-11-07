package de.murmelmeister.murmelapi.permission;

import com.github.benmanes.caffeine.cache.LoadingCache;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.utils.CacheUtil;
import de.murmelmeister.murmelapi.utils.update.RefreshEvent;
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshType;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;

import java.time.Duration;
import java.util.*;

import static de.murmelmeister.murmelapi.MurmelAPI.CONSOLE_USER_ID;

/**
 * The PermissionProvider class provides methods to manage and check permissions for users and groups.
 * It implements the Permission interface.
 */
public final class PermissionProvider implements Permission, RefreshListener, AutoCloseable {
    private final Database database;
    private final UserProvider userProvider;
    private final GroupParentProvider groupParentProvider;
    private final GroupPermissionProvider groupPermissionProvider;
    private final UserParentProvider userParentProvider;
    private final UserPermissionProvider userPermissionProvider;
    private final LoadingCache<Integer, Set<String>> cache;

    public PermissionProvider(Database database, UserProvider userProvider,
                              GroupParentProvider groupParentProvider, GroupPermissionProvider groupPermissionProvider,
                              UserParentProvider userParentProvider, UserPermissionProvider userPermissionProvider,
                              long cacheCapcity, Duration refreshInterval) {
        this.database = database;
        this.userProvider = userProvider;
        this.groupParentProvider = groupParentProvider;
        this.groupPermissionProvider = groupPermissionProvider;
        this.userParentProvider = userParentProvider;
        this.userPermissionProvider = userPermissionProvider;
        this.cache = CacheUtil.buildCacheRefresh(this::loadAllFromDatabase, cacheCapcity, refreshInterval);
        RefreshUtil.register(this);
    }

    private Set<String> loadAllFromDatabase(int userId) {
        return new LinkedHashSet<>(database.queryListCallable("CALL getUserPermission(?)",
                resultSet -> resultSet.getString("permission"),
                stmt -> stmt.setInt(1, userId)));
    }

    public static void setup(Database database) {
        database.update(Database.getProcedureQuery("getUserPermission", "p_user_id INT", """
                     WITH RECURSIVE grp(grp_id) AS (
                         SELECT parent_id AS grp_id
                         FROM   user_parent
                         WHERE  user_id = p_user_id
                           AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP())
                         UNION ALL
                         SELECT gp.parent_id
                         FROM   group_parent gp
                         JOIN   grp g ON g.grp_id = gp.group_id
                         WHERE  gp.expires_at IS NULL OR gp.expires_at > CURRENT_TIMESTAMP()
                     ),
                    \s
                     perms AS (
                         SELECT permission
                         FROM   user_permission
                         WHERE  user_id = p_user_id
                           AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP())
                         UNION
                         SELECT permission
                         FROM   group_permission
                         WHERE  group_id IN (SELECT grp_id FROM grp)
                           AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP())
                     )
                    \s
                     SELECT DISTINCT permission
                     FROM   perms
                     ORDER BY permission;
                \s"""));
    }

    @Override
    public Set<String> getPermissions(int userId) {
        return cache.get(userId);
    }

    @Override
    public boolean hasPermission(User user, String permission) {
        if (user.id() < CONSOLE_USER_ID || permission == null || permission.isEmpty())
            return false; // Invalid permission
        if (user.systemUser()) return true; // Special case for server-wide permissions
        Set<String> permissions = getPermissions(user.id());
        if (permissions.isEmpty()) return false;
        if (permissions.contains("-" + permission)) return false;

        for (String negative : permissions) {
            if (negative.startsWith("-") && negative.endsWith(".*")) {
                String prefix = negative.substring(1, negative.length() - 1); // "-minecraft.command.*" -> "minecraft.command."
                if (permission.startsWith(prefix))
                    return false; // Negative permission matches
            }
        }

        if (permissions.contains("*")) return true;
        if (permissions.contains(permission)) return true;

        for (String perm : permissions) {
            if (perm.endsWith(".*")) {
                String prefix = perm.substring(0, perm.length() - 1); // "minecraft.command.*" -> "minecraft.command."
                if (permission.startsWith(prefix))
                    return true; // Permission matches with wildcard
            }
        }
        return false;
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        User user = userProvider.findByMojangId(uuid);
        if (user == null) return false; // User not found
        return hasPermission(user, permission);
    }

    @Override
    public int loadExpired() {
        int groupParentExpired = groupParentProvider.loadExpired();
        int groupPermissionExpired = groupPermissionProvider.loadExpired();
        int userParentExpired = userParentProvider.loadExpired();
        int userPermissionExpired = userPermissionProvider.loadExpired();
        return groupParentExpired + groupPermissionExpired + userParentExpired + userPermissionExpired;
    }

    @Override
    public void onRefresh(RefreshEvent<?> event) {
        String cacheName = event.type();
        // Let the cache refresh by single and all events (Not really optimal, but works for now)
        if (RefreshType.USER_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.GROUP_PERMISSIONS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.USER_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.GROUP_PARENTS.getName().equalsIgnoreCase(cacheName)
                || RefreshType.SINGLE_USER_PERMISSION.getName().equalsIgnoreCase(cacheName)
                || RefreshType.SINGLE_GROUP_PERMISSION.getName().equalsIgnoreCase(cacheName)
                || RefreshType.SINGLE_USER_PARENT.getName().equalsIgnoreCase(cacheName)
                || RefreshType.SINGLE_GROUP_PARENT.getName().equalsIgnoreCase(cacheName)
                || RefreshType.ALL.getName().equalsIgnoreCase(cacheName)) {
            cache.invalidateAll();
            List<Integer> userIds = userProvider.findAll().stream().map(User::id).toList();
            userIds.forEach(id -> cache.put(id, loadAllFromDatabase(id)));
        }
    }

    @Override
    public void close() {
        RefreshUtil.unregister(this);
        cache.invalidateAll();
    }

    @Override
    public void closeCache() {
        close();
    }
}

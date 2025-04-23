package de.murmelmeister.murmelapi.permission;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.util.*;

/**
 * The PermissionProvider class provides methods to manage and check permissions for users and groups.
 * It implements the Permission interface.
 */
public record PermissionProvider(Database database, Group group, User user) implements Permission {
    // TODO: All permissions of user -> cached

    public static void setup(Database database) {
        Procedure.loadAll(database);
    }

    @Override
    public List<String> getPermissions(int userId) {
        return database.queryListCallable(Procedure.GET_USER_PERMISSION.getName(),
                resultSet -> resultSet.getString("permission"), userId);
    }

    @Override
    public boolean hasPermission(int userId, String permission) {
        Set<String> permissions = new LinkedHashSet<>(getPermissions(userId));
        if (permissions.contains("-" + permission)) return false;
        if (permissions.contains("*")) return true;
        // TODO: With wildcard
        return permissions.contains(permission);
    }

    @Override
    public boolean hasPermission(UUID uuid, String permission) {
        return hasPermission(user.getId(uuid), permission);
    }

    @Override
    public int loadExpired() {
        int userRows = user.loadExpired();
        int groupRows = group.loadExpired();
        return userRows + groupRows;
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

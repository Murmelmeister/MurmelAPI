package de.murmelmeister.murmelapi.group.permission;

import java.util.List;

/**
 * GroupPermission is a class that provides methods to manage group permissions in the database.
 * It implements the GroupPermissionProvider interface and uses the Database class to interact with the database.
 */
public sealed interface GroupPermissionProvider permits GroupPermissionProviderImpl {
    void closeCache();

    void refreshCache();

    GroupPermission getPermission(int groupId, String permission);

    List<GroupPermission> getPermissions(int groupId);

    GroupPermission add(int groupId, String permission, long duration, int createdBy);

    int remove(int groupId, String permission);

    int clear(int groupId);

    GroupPermission update(int groupId, String permission, long duration, int changedBy);

    int loadExpired();
}

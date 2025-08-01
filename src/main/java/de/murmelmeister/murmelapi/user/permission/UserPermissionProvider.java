package de.murmelmeister.murmelapi.user.permission;

import java.util.List;

/**
 * This interface defines methods for managing user permissions.
 * It allows checking, adding, removing, and retrieving permissions for users.
 * It also provides methods to manage permission expiration and track who created or updated a permission.
 */
public interface UserPermissionProvider {
    void closeCache();

    void refreshCache();

    UserPermission getPermission(int userId, String permission);

    List<UserPermission> getPermissions(int userId);

    UserPermission add(int userId, String permission, long duration, int createdBy);

    int remove(int userId, String permission);

    int clear(int userId);

    UserPermission update(int userId, String permission, long duration, int changedBy);

    int loadExpired();
}

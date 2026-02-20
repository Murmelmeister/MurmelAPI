package de.murmelmeister.murmelapi.user.permission;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * This interface defines methods for managing user permissions.
 * It allows checking, adding, removing, and retrieving permissions for users.
 * It also provides methods to manage permission expiration and track who created or updated a permission.
 */
public interface UserPermissionProvider {
    void refreshCache();

    @Nullable UserPermission getPermission(int userId, @NotNull String permission);

    @NotNull @Unmodifiable
    List<UserPermission> getPermissions(int userId);

    @Nullable UserPermission add(int userId, @NotNull String permission, long duration, int createdBy);

    int remove(int userId, @NotNull String permission);

    int clear(int userId);

    @Nullable UserPermission update(int userId, @NotNull String permission, long duration, int changedBy);

    int loadExpired();
}

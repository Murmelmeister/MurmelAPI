package de.murmelmeister.murmelapi.permission;

import java.util.List;
import java.util.UUID;

/**
 * Permission interface to manage permissions.
 */
public sealed interface Permission permits PermissionProvider {
    /**
     * Obtains the permissions of a user.
     *
     * @param userId The id of the user.
     * @return The permissions of the user.
     */
    List<String> getPermissions(int userId);

    /**
     * Checks if a user has a permission.
     *
     * @param uuid       The id of the player.
     * @param permission The permission.
     * @return True if the user has the permission, otherwise false.
     */
    boolean hasPermission(UUID uuid, String permission);
}

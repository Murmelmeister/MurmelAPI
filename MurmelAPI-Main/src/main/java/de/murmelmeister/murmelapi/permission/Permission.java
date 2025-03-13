package de.murmelmeister.murmelapi.permission;

import java.util.List;
import java.util.UUID;

/**
 * Permission interface to manage permissions.
 */
public sealed interface Permission permits PermissionProvider {
    /**
     * Retrieves a list of permissions for a user based on their user ID.
     * This includes both the user's direct permissions and those inherited
     * from parent entities.
     *
     * @param userId The unique identifier of the user whose permissions are to be retrieved.
     * @return A list of all permissions applicable to the specified user.
     */
    List<String> getPermissions(int userId);

    /**
     * Reloads or refreshes any expired entities or permissions within the system.
     * This method is intended to invoke expiration-related updates for underlying
     * group and user components.
     */
    void loadExpired();

    /**
     * Checks if a user identified by their unique UUID has a specific permission.
     *
     * @param uuid       The universally unique identifier (UUID) of the user.
     * @param permission The permission string to check for the user.
     * @return {@code true} if the user has the specified permission, otherwise {@code false}.
     */
    boolean hasPermission(UUID uuid, String permission);

    /**
     * Checks if a user identified by their unique user ID has a specific permission.
     *
     * @param userId     The unique identifier of the user to check permissions for.
     * @param permission The specific permission string to verify for the user.
     * @return {@code true} if the user has the specified permission, otherwise {@code false}.
     */
    boolean hasPermission(int userId, String permission);
}

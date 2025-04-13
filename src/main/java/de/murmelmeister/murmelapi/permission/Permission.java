package de.murmelmeister.murmelapi.permission;

import java.util.List;
import java.util.UUID;

/**
 * The Permission interface provides methods to manage and check user permissions.
 * It is designed to be implemented by classes that handle permission management.
 */
public sealed interface Permission permits PermissionProvider {
    /**
     * Retrieves all effective permissions for the specified user.
     * <p>
     * This method starts by gathering the user's individual permissions from their permission provider.
     * It then iterates over each parent ID provided by the user's parent provider, fetching and merging
     * all permissions inherited from the group's permission provider for each parent.
     * The use of a LinkedHashSet ensures that duplicate permissions are removed while preserving the insertion order.
     *
     * @param userId The ID of the user whose permissions are to be retrieved.
     * @return A List of unique permission strings for the user.
     */
    List<String> getPermissions(int userId);

    /**
     * Checks whether the specified user has a particular permission.
     * <p>
     * The method retrieves all effective permissions for the user. If a negative permission (prefixed by "-")
     * is present, the permission is considered explicitly denied and the method returns false.
     * If a wildcard ("*") is present, the user is granted the permission.
     * <strong>Note:</strong> Wildcard processing is currently incomplete (see TO-DO).
     *
     * @param userId     The ID of the user.
     * @param permission The permission string to check.
     * @return {@code true} if the user has the permission; {@code false} otherwise.
     */
    boolean hasPermission(int userId, String permission);

    /**
     * Checks whether the user identified by the specified UUID has the given permission.
     * <p>
     * This method obtains the user ID corresponding to the UUID and delegates the permission check
     * to the {@link #hasPermission(int, String)} method.
     *
     * @param uuid       The UUID of the user.
     * @param permission The permission string to check.
     * @return {@code true} if the user has the permission; {@code false} otherwise.
     */
    boolean hasPermission(UUID uuid, String permission);

    /**
     * Loads expired permissions for both user and group records.
     * <p>
     * The method calls the corresponding loadExpired methods of the user and group providers and
     * returns the total number of rows affected.
     *
     * @return The total number of rows affected by removing expired permissions.
     */
    int loadExpired();
}

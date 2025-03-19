package de.murmelmeister.murmelapi.user.permission;

import java.sql.Timestamp;
import java.util.List;

/**
 * User permission interface to manage user permissions.
 */
public sealed interface UserPermission permits UserPermissionProvider {
    /**
     * Checks whether a specific permission exists for a given user.
     *
     * @param userId     The unique identifier of the user.
     * @param permission The permission to check for the user.
     * @return {@code true} if the permission exists for the user, otherwise {@code false}.
     */
    boolean existsPermission(int userId, String permission);

    /**
     * Adds a specific permission to a user with an optional expiration time.
     *
     * @param executorId The unique identifier for the log entry of the operation.
     * @param userId     The unique identifier of the user to whom the permission will be added.
     * @param permission The permission string to be added to the user.
     * @param time       The expiration time for the permission in milliseconds. Use -1 for no expiration.
     */
    void addPermission(int executorId, int userId, String permission, long time);

    /**
     * Removes a specific permission from a user.
     *
     * @param userId     The unique identifier of the user from whom the permission will be removed.
     * @param permission The permission string to be removed from the user.
     */
    void removePermission(int userId, String permission);

    /**
     * Clears all permissions associated with a specific user.
     *
     * @param userId The unique identifier of the user whose permissions will be cleared.
     */
    void clearPermission(int userId);

    /**
     * Retrieves a list of permissions associated with a specific user.
     *
     * @param userId The unique identifier of the user whose permissions are being retrieved.
     * @return A list of permission strings associated with the specified user.
     */
    List<String> getPermissions(int userId);

    /**
     * Retrieves the expiration time of a specific permission for a given user.
     *
     * @param userId     The unique identifier of the user.
     * @param permission The permission for which the expiration time is being retrieved.
     * @return The expiration time in milliseconds since the epoch for the specified permission and user.
     * Returns -1 if the permission has no expiration time.
     */
    Timestamp getExpiredTime(int userId, String permission);

    /**
     * Retrieves the expiration date of a specific permission for a given user.
     * The expiration date is returned as a formatted string representation.
     *
     * @param userId     The unique identifier of the user.
     * @param permission The permission for which the expiration date is being retrieved.
     * @return A string representation of the expiration date for the specified user and permission.
     * Returns an empty string if the permission has no expiration date.
     */
    String getExpiredDate(int userId, String permission);

    /**
     * Sets the expiration time for a specific permission associated with a user.
     * Updates the expiration to the specified time in milliseconds.
     *
     * @param executorId The unique identifier for the log entry of the operation.
     * @param userId     The unique identifier of the user.
     * @param permission The permission for which the expiration time will be set.
     * @param time       The expiration time in milliseconds since the epoch. Use -1 for no expiration.
     * @return A string indicating the status of the operation, such as success or an error message.
     */
    String setExpiredTime(int executorId, int userId, String permission, long time);

    /**
     * Checks if a specific permission for a given user has expired.
     *
     * @param userId     The unique identifier of the user.
     * @param permission The permission whose expiration status is being checked.
     * @return {@code true} if the permission has expired, otherwise {@code false}.
     */
    boolean isExpired(int userId, String permission);

    /**
     * Retrieves the identifier of the user who created the specified permission for a given user.
     *
     * @param userId     The unique identifier of the user for whom the permission was created.
     * @param permission The permission whose creator's identifier is to be retrieved.
     * @return The unique identifier of the user who created the specified permission.
     */
    int getCreatedBy(int userId, String permission);

    /**
     * Retrieves the creation timestamp of a specific permission for a given user.
     *
     * @param userId     The unique identifier of the user.
     * @param permission The permission for which the creation timestamp is being retrieved.
     * @return The timestamp indicating when the specified permission was created for the user.
     */
    Timestamp getCreatedAt(int userId, String permission);

    /**
     * Retrieves the identifier of the executor who last modified a specific permission associated with a user.
     *
     * @param userId     The unique identifier of the user whose permission modification details are being retrieved.
     * @param permission The permission for which the modification details are being retrieved.
     * @return The unique identifier of the executor who last modified the specified permission for the given user.
     */
    int getModifiedBy(int userId, String permission);

    /**
     * Retrieves the timestamp indicating the last modification time of a specific permission
     * associated with a given user.
     *
     * @param userId     The unique identifier of the user whose permission modification timestamp is needed.
     * @param permission The permission for which the last modification time will be retrieved.
     * @return The timestamp representing the last modification time of the specified permission
     * for the given user. Returns {@code null} if no modification timestamp is found.
     */
    Timestamp getModifiedAt(int userId, String permission);

    /**
     * Loads all expired permissions for the specified user and performs operations related to the expired permissions.
     */
    void loadExpired();
}

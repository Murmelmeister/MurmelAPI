package de.murmelmeister.murmelapi.user.permission;

import java.sql.Timestamp;
import java.util.List;

/**
 * This interface defines methods for managing user permissions.
 * It allows checking, adding, removing, and retrieving permissions for users.
 * It also provides methods to manage permission expiration and track who created or updated a permission.
 */
public sealed interface UserPermission permits UserPermissionProvider {
    /**
     * Checks if the specified permission exists for the given user.
     *
     * @param userId     The id of the user.
     * @param permission The permission string to check.
     * @return {@code true} if the user id is greater than 0, the permission is not null and a matching record exists; {@code false} otherwise.
     */
    boolean existsPermission(int userId, String permission);

    /**
     * Adds a permission record for the specified user with an optional expiry time.
     * If the provided time is -1, the permission does not expire.
     *
     * @param userId     The id of the user.
     * @param permission The permission to add.
     * @param time       The duration (in milliseconds) after which the permission expires, or -1 for no expiration.
     * @param createdBy  The id of the user creating the permission.
     * @return The number of rows affected by the insertion, or 0 if the input parameters are invalid.
     */
    int addPermission(int userId, String permission, long time, int createdBy);

    /**
     * Removes the specified permission for the given user.
     *
     * @param userId     The id of the user.
     * @param permission The permission to remove.
     * @return The number of rows affected by the deletion, or 0 if the input parameters are invalid.
     */
    int removePermission(int userId, String permission);

    /**
     * Clears all permissions for the specified user.
     *
     * @param userId The id of the user whose permissions should be cleared.
     * @return The number of rows affected by the deletion, or 0 if the user id is invalid.
     */
    int clearPermission(int userId);

    /**
     * Retrieves all active permissions for the specified user.
     * Active permissions are those that do not have an expiration time or have an expiration time in the future.
     *
     * @param userId The id of the user.
     * @return A list of permission strings, or {@code null} if the user id is invalid.
     */
    List<String> getPermissions(int userId);

    /**
     * Retrieves the expiration timestamp for the specified permission of the given user.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose expiration time is to be retrieved.
     * @return The expiration timestamp, or {@code null} if the user id is invalid or permission is null.
     */
    Timestamp getExpiredAt(int userId, String permission);

    /**
     * Returns a formatted date string representing the expiration time for the specified permission of the user.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose expiration date is to be formatted.
     * @return The formatted expiration date, or {@code null} if no expiration date is available.
     */
    String getExpiredDate(int userId, String permission);

    /**
     * Updates the expiration timestamp for the specified permission of the given user.
     * If the provided time is -1, the permission is set to never expire.
     *
     * @param userId     The id of the user.
     * @param permission The permission to update.
     * @param time       The duration (in milliseconds) after which the permission should expire, or -1 for no expiration.
     * @param updatedBy  The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if the input parameters are invalid.
     */
    int setExpiredAt(int userId, String permission, long time, int updatedBy);

    /**
     * Retrieves the id of the user who created the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission to check.
     * @return The creator's user id, or -2 if the input parameters are invalid.
     */
    int getCreatedBy(int userId, String permission);

    /**
     * Retrieves the creation timestamp for the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose creation time is to be retrieved.
     * @return The creation timestamp, or {@code null} if the input parameters are invalid.
     */
    Timestamp getCreatedAt(int userId, String permission);

    /**
     * Returns a formatted date string representing the creation date of the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose creation date is to be formatted.
     * @return The formatted creation date, or {@code null} if the creation timestamp is unavailable.
     */
    String getCreatedDate(int userId, String permission);

    /**
     * Retrieves the id of the user who last updated the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose updater is to be retrieved.
     * @return The updater's user id, or -2 if the input parameters are invalid.
     */
    int getUpdatedBy(int userId, String permission);

    /**
     * Retrieves the last update timestamp for the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose update time is to be retrieved.
     * @return The update timestamp, or {@code null} if the input parameters are invalid.
     */
    Timestamp getUpdatedAt(int userId, String permission);

    /**
     * Returns a formatted date string representing the last update date of the specified permission record.
     *
     * @param userId     The id of the user.
     * @param permission The permission whose update date is to be formatted.
     * @return The formatted update date, or {@code null} if the update timestamp is unavailable.
     */
    String getUpdatedDate(int userId, String permission);

    /**
     * Removes all permission records that have expired.
     *
     * @return The number of rows affected by the removal of expired permissions.
     */
    int loadExpired();
}

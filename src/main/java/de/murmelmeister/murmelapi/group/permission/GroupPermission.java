package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.parent.GroupParent;

import java.sql.Timestamp;
import java.util.List;

/**
 * GroupPermission is a class that provides methods to manage group permissions in the database.
 * It implements the GroupPermissionProvider interface and uses the Database class to interact with the database.
 */
public sealed interface GroupPermission permits GroupPermissionProvider {
    /**
     * Checks if the specified permission exists for the given group.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission string to check.
     * @return {@code true} if groupId is greater than 0, permission is not null and a corresponding record exists;
     * {@code false} otherwise.
     */
    boolean existsPermission(int groupId, String permission);

    /**
     * Adds a permission record for the specified group with an optional expiration time.
     * If the provided time is -1, the permission does not expire.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to add.
     * @param time       The duration in milliseconds until expiration, or -1 for no expiration.
     * @param createdBy  The ID of the user who creates the permission.
     * @return The number of rows affected by the insertion, or 0 if input parameters are invalid.
     */
    int addPermission(int groupId, String permission, long time, int createdBy);

    /**
     * Removes the specified permission for the given group.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to remove.
     * @return The number of rows affected by the deletion, or 0 if input parameters are invalid.
     */
    int removePermission(int groupId, String permission);

    /**
     * Clears all permission records for the specified group.
     *
     * @param groupId The ID of the group.
     * @return The number of rows affected by the deletion, or 0 if the group ID is invalid.
     */
    int clearPermission(int groupId);

    /**
     * Retrieves a list of active permissions for the specified group.
     * Active permissions are those that do not have an expiration time or have an expiration time in the future.
     *
     * @param groupId The ID of the group.
     * @return A List of permission strings.
     */
    List<String> getPermissions(int groupId);

    /**
     * Recursively retrieves all permissions for the specified group, including permissions from parent groups.
     * This method aggregates permissions from the group and all of its parents (as provided by the GroupParent interface).
     *
     * @param groupParent The GroupParent instance used to retrieve parent relationships.
     * @param groupId     The ID of the group.
     * @return A List of unique permission strings aggregated from the group and its hierarchy.
     */
    List<String> getAllPermissions(GroupParent groupParent, int groupId);

    /**
     * Retrieves the expiration timestamp for the specified permission of the given group.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission whose expiration time is to be retrieved.
     * @return The expiration timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getExpiredAt(int groupId, String permission);

    /**
     * Returns a formatted date string representing the expiration time of the specified permission.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission whose expiration date is to be formatted.
     * @return A formatted expiration date string, or {@code null} if no expiration time is set.
     */
    String getExpiredDate(int groupId, String permission);

    /**
     * Updates the expiration timestamp for the specified permission of the given group.
     * If the provided time is -1, the expiration is cleared (set to null).
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to update.
     * @param time       The duration in milliseconds until expiration, or -1 to remove expiration.
     * @param updatedBy  The ID of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setExpiredAt(int groupId, String permission, long time, int updatedBy);

    /**
     * Retrieves the user ID of the creator of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The creator's user ID, or -2 if input parameters are invalid.
     */
    int getCreatedBy(int groupId, String permission);

    /**
     * Retrieves the creation timestamp of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The creation timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getCreatedAt(int groupId, String permission);

    /**
     * Returns a formatted date string representing the creation date of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The formatted creation date string, or {@code null} if creation timestamp is unavailable.
     */
    String getCreatedDate(int groupId, String permission);

    /**
     * Retrieves the user ID of the last updater of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The updater's user ID, or -2 if input parameters are invalid.
     */
    int getUpdatedBy(int groupId, String permission);

    /**
     * Retrieves the last update timestamp of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The update timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getUpdatedAt(int groupId, String permission);

    /**
     * Returns a formatted date string representing the last update date of the specified permission record.
     *
     * @param groupId    The ID of the group.
     * @param permission The permission to query.
     * @return The formatted update date string, or {@code null} if update timestamp is unavailable.
     */
    String getUpdatedDate(int groupId, String permission);

    /**
     * Removes all expired permission records from the database.
     * A record is considered expired if its expiration timestamp is not null and is less than or equal to the current time.
     *
     * @return The number of rows affected by the deletion of expired permissions.
     */
    int loadExpired();
}

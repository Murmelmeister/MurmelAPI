package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.parent.GroupParent;

import java.sql.Timestamp;
import java.util.List;

/**
 * Group permission interface to manage group permissions.
 */
public sealed interface GroupPermission permits GroupPermissionProvider {
    /**
     * Checks if a specific permission exists for a given group.
     *
     * @param groupId    The ID of the group to check the permission for.
     * @param permission The name of the permission to verify.
     * @return {@code true} if the permission exists for the group, otherwise {@code false}.
     */
    boolean existsPermission(int groupId, String permission);

    /**
     * Adds a specific permission to a group along with an expiration time.
     *
     * @param executorId The ID of the executor initiating the permission addition.
     * @param groupId    The ID of the group to which the permission will be added.
     * @param permission The permission to add to the group.
     * @param time       The expiration time in milliseconds. If set to -1, the permission will not expire.
     */
    void addPermission(int executorId, int groupId, String permission, long time);

    /**
     * Removes a specific permission from a group.
     *
     * @param groupId    The ID of the group from which the permission is to be removed.
     * @param permission The permission to be removed from the group.
     */
    void removePermission(int groupId, String permission);

    /**
     * Removes all permissions associated with the specified group.
     *
     * @param groupId The unique identifier of the group whose permissions are to be cleared.
     */
    void clearPermission(int groupId);

    /**
     * Retrieves a list of permissions associated with a specified group ID.
     *
     * @param groupId The ID of the group whose permissions are to be retrieved.
     * @return A list of permissions associated with the given group ID.
     */
    List<String> getPermissions(int groupId);

    /**
     * Retrieves all permissions associated with a given group, including inherited permissions
     * from its parent group if applicable.
     *
     * @param groupParent The parent group entity, which may provide inherited permissions to the group.
     * @param groupId     The unique identifier of the group for which permissions are being retrieved.
     * @return A list of permissions associated with the group, including inherited permissions if present.
     */
    List<String> getAllPermissions(GroupParent groupParent, int groupId);

    /**
     * Retrieves the expiration time for a specific permission associated with a given group.
     *
     * @param groupId    The ID of the group for which the permission expiration time is being retrieved.
     * @param permission The permission whose expiration time is being retrieved.
     * @return A {@code Timestamp} object representing the expiration date and time of the association.
     */
    Timestamp getExpiredAt(int groupId, String permission);

    /**
     * Retrieves the expiration date of a specific permission associated with a given group.
     *
     * @param groupId    The ID of the group whose permission expiration date is being queried.
     * @param permission The name of the permission for which the expiration date is being retrieved.
     * @return A {@code String} representing the expiration date of the specified permission,
     * or {@code null} if no expiration record exists.
     */
    String getExpiredDate(int groupId, String permission);

    /**
     * Sets the expiration time for a specific permission associated with a group.
     * The expiration time can be specified to indicate when the permission should expire.
     *
     * @param executorId The ID of the user or process making the change.
     * @param groupId    The ID of the group associated with the permission.
     * @param permission The name of the permission whose expiration time is being set.
     * @param time       The expiration time in milliseconds from the current time; use -1 for no expiration.
     */
    void setExpiredAt(int executorId, int groupId, String permission, long time);

    /**
     * Retrieves the ID of the user who created the specified permission for the given group.
     *
     * @param groupId    The ID of the group for which the permission was created.
     * @param permission The name of the permission to retrieve the creator information for.
     * @return The ID of the user who created the permission, or -2 if no such permission exists.
     */
    int getCreatedBy(int groupId, String permission);

    /**
     * Retrieves the timestamp of when the given permission was created for the specified group.
     *
     * @param groupId    The ID of the group for which the permission was created.
     * @param permission The specific permission whose creation timestamp is to be retrieved.
     * @return A {@code Timestamp} representing the creation time of the permission, or {@code null}
     * if the permission does not exist or no creation time is available.
     */
    Timestamp getCreatedAt(int groupId, String permission);

    /**
     * Retrieves the creation date of a specific permission associated with a given group.
     *
     * @param groupId    The ID of the group whose permission creation date is being queried.
     * @param permission The name of the permission for which the creation date is being retrieved.
     * @return A {@code String} representing the creation date of the specified permission,
     * or {@code null} if no creation record exists.
     */
    String getCreatedDate(int groupId, String permission);

    /**
     * Retrieves the user ID of the user who last modified the specified permission for a given group.
     *
     * @param groupId    The ID of the group whose permission modification details are being queried.
     * @param permission The name of the permission for which the modifying user is being retrieved.
     * @return The ID of the user who last modified the specified permission. If no modification was found, returns -2 as a fallback.
     */
    int getModifiedBy(int groupId, String permission);

    /**
     * Retrieves the timestamp of the last modification made to a specific permission
     * associated with a given group.
     *
     * @param groupId    The unique identifier of the group the permission belongs to.
     * @param permission The name of the permission whose modification timestamp is to be retrieved.
     * @return A {@code Timestamp} object representing the last modification time of the specified
     * permission, or {@code null} if no modification record exists.
     */
    Timestamp getModifiedAt(int groupId, String permission);

    /**
     * Retrieves the modification date of a specific permission associated with a given group.
     *
     * @param groupId    The ID of the group whose permission modification date is being queried.
     * @param permission The name of the permission for which the modification date is being retrieved.
     * @return A {@code String} representing the last modification date of the specified permission,
     * or {@code null} if no modification record exists.
     */
    String getModifiedDate(int groupId, String permission);

    /**
     * Loads all expired permissions or entities associated with a given group
     * into the system for further processing or cleanup.
     */
    void loadExpired();
}

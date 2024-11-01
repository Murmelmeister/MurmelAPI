package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;

import java.util.List;

/**
 * Group permission interface to manage group permissions.
 */
public sealed interface GroupPermission permits GroupPermissionProvider {
    /**
     * Checks if a permission exists.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return True if the permission exists, otherwise false.
     */
    boolean existsPermission(int groupId, String permission);

    /**
     * Adds a permission to a group.
     *
     * @param groupId    The id of the group.
     * @param creatorId  The id of the creator.
     * @param permission The permission.
     * @param time       The time the permission was added.
     */
    void addPermission(int groupId, int creatorId, String permission, long time);

    /**
     * Removes a permission from a group.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     */
    void removePermission(int groupId, String permission);

    /**
     * Clears all permissions of a group.
     *
     * @param groupId The id of the group.
     */
    void clearPermission(int groupId);

    /**
     * Obtains all permissions of a group.
     *
     * @param groupId The id of the group.
     * @return A list of all permissions of the group.
     */
    List<String> getPermissions(int groupId);

    /**
     * Obtains all permissions of a group.
     *
     * @param groupParent The group parent.
     * @param groupId     The id of the group.
     * @return A list of all permissions of the group.
     */
    List<String> getAllPermissions(GroupParent groupParent, int groupId);

    /**
     * Obtains the creator id of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The creator id of the permission.
     */
    int getCreatorId(int groupId, String permission);

    /**
     * Obtains the created time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The created time of the permission.
     */
    long getCreatedTime(int groupId, String permission);

    /**
     * Obtains the created date of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The created date of the permission.
     */
    String getCreatedDate(int groupId, String permission);

    /**
     * Obtains the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The expired time of the permission.
     */
    long getExpiredTime(int groupId, String permission);

    /**
     * Obtains the expired date of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The expired date of the permission.
     */
    String getExpiredDate(int groupId, String permission);

    /**
     * Sets the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time the permission will expire.
     * @return The expired date of the permission.
     */
    String setExpiredTime(int groupId, String permission, long time);

    /**
     * Adds time to the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time to add to the expired time.
     * @return The expired date of the permission.
     */
    String addExpiredTime(int groupId, String permission, long time);

    /**
     * Removes time from the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time to remove from the expired time.
     * @return The expired date of the permission.
     */
    String removeExpiredTime(int groupId, String permission, long time);

    /**
     * Loads all expired permissions of a group.
     *
     * @param group The group.
     */
    void loadExpired(Group group);
}

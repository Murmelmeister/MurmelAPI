package de.murmelmeister.murmelapi.group.permission;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.parent.GroupParent;

import java.sql.SQLException;
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
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsPermission(int groupId, String permission) throws SQLException;

    /**
     * Adds a permission to a group.
     *
     * @param groupId    The id of the group.
     * @param creatorId  The id of the creator.
     * @param permission The permission.
     * @param time       The time the permission was added.
     * @throws SQLException If an SQL error occurs.
     */
    void addPermission(int groupId, int creatorId, String permission, long time) throws SQLException;

    /**
     * Removes a permission from a group.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @throws SQLException If an SQL error occurs.
     */
    void removePermission(int groupId, String permission) throws SQLException;

    /**
     * Clears all permissions of a group.
     *
     * @param groupId The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void clearPermission(int groupId) throws SQLException;

    /**
     * Obtains all permissions of a group.
     *
     * @param groupId The id of the group.
     * @return A list of all permissions of the group.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getPermissions(int groupId) throws SQLException;

    /**
     * Obtains all permissions of a group.
     *
     * @param groupParent The group parent.
     * @param groupId     The id of the group.
     * @return A list of all permissions of the group.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getAllPermissions(GroupParent groupParent, int groupId) throws SQLException;

    /**
     * Obtains the creator id of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The creator id of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int groupId, String permission) throws SQLException;

    /**
     * Obtains the created time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The created time of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int groupId, String permission) throws SQLException;

    /**
     * Obtains the created date of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The created date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int groupId, String permission) throws SQLException;

    /**
     * Obtains the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The expired time of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    long getExpiredTime(int groupId, String permission) throws SQLException;

    /**
     * Obtains the expired date of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @return The expired date of the permission.
     * @throws SQLException If an SQL error occurs.
     */
    String getExpiredDate(int groupId, String permission) throws SQLException;

    /**
     * Sets the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time the permission will expire.
     * @throws SQLException If an SQL error occurs.
     */
    void setExpiredTime(int groupId, String permission, long time) throws SQLException;

    /**
     * Adds time to the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time to add to the expired time.
     * @throws SQLException If an SQL error occurs.
     */
    String addExpiredTime(int groupId, String permission, long time) throws SQLException;

    /**
     * Removes time from the expired time of a permission.
     *
     * @param groupId    The id of the group.
     * @param permission The permission.
     * @param time       The time to remove from the expired time.
     * @throws SQLException If an SQL error occurs.
     */
    String removeExpiredTime(int groupId, String permission, long time) throws SQLException;

    /**
     * Loads all expired permissions of a group.
     *
     * @param group The group.
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired(Group group) throws SQLException;
}

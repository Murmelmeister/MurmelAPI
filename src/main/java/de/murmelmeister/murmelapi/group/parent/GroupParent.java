package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.sql.SQLException;
import java.util.List;

/**
 * Group parent interface to manage group parents.
 */
public sealed interface GroupParent permits GroupParentProvider {
    /**
     * Checks if a parent exists.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return True if the parent exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsParent(int groupId, int parentId) throws SQLException;

    /**
     * Adds a parent to a group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The id of the creator.
     * @param parentId  The id of the parent.
     * @param time      The time the parent was added.
     * @throws SQLException If an SQL error occurs.
     */
    void addParent(int groupId, int creatorId, int parentId, long time) throws SQLException;

    /**
     * Removes a parent from a group.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    void removeParent(int groupId, int parentId) throws SQLException;

    /**
     * Clears all parents from a group.
     *
     * @param groupId The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void clearParent(int groupId) throws SQLException;

    /**
     * Obtains all parent ids of a group.
     *
     * @param groupId The id of the group.
     * @return A list of all parent ids of the group.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getParentIds(int groupId) throws SQLException;

    /**
     * Obtains all parent names of a group.
     *
     * @param group   The group.
     * @param groupId The id of the group.
     * @return A list of all parent names of the group.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getParentNames(Group group, int groupId) throws SQLException;

    /**
     * Obtains the creator id of a parent.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The creator id of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int groupId, int parentId) throws SQLException;

    /**
     * Obtains the time the parent was created.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The time the parent was created.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int groupId, int parentId) throws SQLException;

    /**
     * Obtains the date the parent was created.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The date the parent was created.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int groupId, int parentId) throws SQLException;

    /**
     * Obtains the time the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The time the parent will expire.
     * @throws SQLException If an SQL error occurs.
     */
    long getExpiredTime(int groupId, int parentId) throws SQLException;

    /**
     * Obtains the date the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @return The date the parent will expire.
     * @throws SQLException If an SQL error occurs.
     */
    String getExpiredDate(int groupId, int parentId) throws SQLException;

    /**
     * Sets the time the parent will expire.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time the parent will expire.
     * @throws SQLException If an SQL error occurs.
     */
    void setExpiredTime(int groupId, int parentId, long time) throws SQLException;

    /**
     * Adds time to the parent expiration.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time to add to the expiration.
     * @return An error message if the time is negative, otherwise an empty string.
     * @throws SQLException If an SQL error occurs.
     */
    String addExpiredTime(int groupId, int parentId, long time) throws SQLException;

    /**
     * Removes time from the parent expiration.
     *
     * @param groupId  The id of the group.
     * @param parentId The id of the parent.
     * @param time     The time to remove from the expiration.
     * @return An error message if the time is negative, otherwise an empty string.
     * @throws SQLException If an SQL error occurs.
     */
    String removeExpiredTime(int groupId, int parentId, long time) throws SQLException;

    /**
     * Loads all expired parents of a group.
     *
     * @param group The group.
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired(Group group) throws SQLException;
}

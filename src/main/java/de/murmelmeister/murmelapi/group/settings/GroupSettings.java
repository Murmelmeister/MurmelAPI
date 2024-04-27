package de.murmelmeister.murmelapi.group.settings;

import java.sql.SQLException;
import java.util.List;

/**
 * Group settings interface to manage group settings.
 */
public sealed interface GroupSettings permits GroupSettingsProvider {
    /**
     * Checks if a group exists.
     *
     * @param groupId The id of the group.
     * @return True if the group exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsGroup(int groupId) throws SQLException;

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param groupId   The id of the group.
     * @param creatorId The creator id of the group.
     * @param sortId    The sort id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void createGroup(int groupId, int creatorId, int sortId) throws SQLException;

    /**
     * Deletes a group.
     *
     * @param groupId The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteGroup(int groupId) throws SQLException;

    /**
     * Obtains the creator id of a group.
     *
     * @param groupId The id of the group.
     * @return The creator id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int groupId) throws SQLException;

    /**
     * Obtains the created time of a group.
     *
     * @param groupId The id of the group.
     * @return The created time of the group.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int groupId) throws SQLException;

    /**
     * Obtains the created date of a group.
     *
     * @param groupId The id of the group.
     * @return The created date of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int groupId) throws SQLException;

    /**
     * Obtains the sort id of a group.
     *
     * @param groupId The id of the group.
     * @return The sort id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    int getSortId(int groupId) throws SQLException;

    /**
     * Sets the sort id of a group.
     *
     * @param groupId The id of the group.
     * @param sortId  The sort id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void setSortId(int groupId, int sortId) throws SQLException;

    /**
     * Obtains a list of all sort ids of the groups.
     *
     * @return A list of all sort ids of the groups.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getSortIds() throws SQLException;
}

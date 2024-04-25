package de.murmelmeister.murmelapi.group.settings;

import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;
import java.util.UUID;

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
     * @throws SQLException If an SQL error occurs.
     */
    void createGroup(int groupId, int creatorId) throws SQLException;

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
     * Obtains the creator id of a group.
     *
     * @param user    The user.
     * @param groupId The id of the group.
     * @return The creator id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    UUID getCreatorId(User user, int groupId) throws SQLException;

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
}

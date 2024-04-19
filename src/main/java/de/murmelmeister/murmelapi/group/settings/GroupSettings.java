package de.murmelmeister.murmelapi.group.settings;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Group settings interface to manage group settings.
 */
public sealed interface GroupSettings permits GroupSettingsProvider {
    /**
     * Checks if a group exists.
     *
     * @param id The id of the group.
     * @return True if the group exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsGroup(int id) throws SQLException;

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param id The id of the group.
     * @param creator The creator of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void createGroup(int id, UUID creator) throws SQLException;

    /**
     * Deletes a group.
     *
     * @param id The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteGroup(int id) throws SQLException;

    /**
     * Gets the creator id of a group.
     *
     * @param id The id of the group.
     * @return The creator id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    UUID getCreatorId(int id) throws SQLException;

    /**
     * Gets the created time of a group.
     *
     * @param id The id of the group.
     * @return The created time of the group.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int id) throws SQLException;

    /**
     * Gets the created date of a group.
     *
     * @param id The id of the group.
     * @return The created date of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int id) throws SQLException;
}

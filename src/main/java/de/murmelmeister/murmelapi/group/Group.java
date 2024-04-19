package de.murmelmeister.murmelapi.group;

import java.sql.SQLException;
import java.util.List;

/**
 * Group interface to manage groups.
 */
public sealed interface Group permits GroupProvider {
    /**
     * Checks if a group exists.
     *
     * @param id The id of the group.
     * @return True if the group exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsGroup(int id) throws SQLException;

    /**
     * Checks if a group exists.
     *
     * @param name The name of the group.
     * @return True if the group exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsGroup(String name) throws SQLException;

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param name The name of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void createNewGroup(String name) throws SQLException;

    /**
     * Deletes a group.
     *
     * @param id The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteGroup(int id) throws SQLException;

    /**
     * Gets the unique id of a group.
     *
     * @param name The name of the group.
     * @return The unique id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    int getUniqueId(String name) throws SQLException;

    /**
     * Gets the name of a group.
     *
     * @param id The id of the group.
     * @return The name of the group.
     * @throws SQLException If an SQL error occurs.
     */
    String getName(int id) throws SQLException;

    /**
     * Renames a group.
     *
     * @param id      The id of the group.
     * @param newName The new name of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void rename(int id, String newName) throws SQLException;

    /**
     * Renames a group.
     *
     * @param oldName The old name of the group.
     * @param newName The new name of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void rename(String oldName, String newName) throws SQLException;

    /**
     * Gets a list of all unique ids of the groups.
     *
     * @return A list of all unique ids of the groups.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getUniqueIds() throws SQLException;

    /**
     * Gets a list of all names of the groups.
     *
     * @return A list of all names of the groups.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getNames() throws SQLException;
}

package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;

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
     * @param name      The name of the group.
     * @param creatorId The creator id of the group.
     * @param sortId    The sort id of the group.
     * @param teamId    The team id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void createNewGroup(String name, int creatorId, int sortId, String teamId) throws SQLException;

    /**
     * Deletes a group.
     *
     * @param id The id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteGroup(int id) throws SQLException;

    /**
     * Obtains the unique id of a group.
     *
     * @param name The name of the group.
     * @return The unique id of the group.
     * @throws SQLException If an SQL error occurs.
     */
    int getUniqueId(String name) throws SQLException;

    /**
     * Obtains the name of a group.
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
     * Obtains a list of all unique ids of the groups.
     *
     * @return A list of all unique ids of the groups.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getUniqueIds() throws SQLException;

    /**
     * Obtains a list of all names of the groups.
     *
     * @return A list of all names of the groups.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getNames() throws SQLException;

    /**
     * Loads all expired things.
     *
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired() throws SQLException;

    /**
     * Obtains the default group.
     * The default group is the group with the id 1.
     *
     * @throws SQLException If an SQL error occurs.
     */
    int getDefaultGroup() throws SQLException;

    /**
     * Obtains the settings of a group.
     *
     * @return The settings of the group.
     */
    GroupSettings getSettings();

    /**
     * Obtains the color settings of a group.
     *
     * @return The color settings of the group.
     */
    GroupColorSettings getColorSettings();

    /**
     * Obtains the parent of a group.
     *
     * @return The parent of the group.
     */
    GroupParent getParent();

    /**
     * Obtains the permission of a group.
     *
     * @return The permission of the group.
     */
    GroupPermission getPermission();
}

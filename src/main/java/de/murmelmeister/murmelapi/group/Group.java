package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.settings.GroupColorSettings;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;

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
     */
    boolean existsGroup(int id);

    /**
     * Checks if a group exists.
     *
     * @param name The name of the group.
     * @return True if the group exists, otherwise false.
     */
    boolean existsGroup(String name);

    /**
     * Creates a new group and checks if the group already exists.
     * If the group already exists, the method will return without creating a new group.
     *
     * @param name      The name of the group.
     * @param creatorId The creator id of the group.
     * @param sortId    The sort id of the group.
     * @param teamId    The team id of the group.
     */
    void createNewGroup(String name, int creatorId, int sortId, String teamId);

    /**
     * Deletes a group.
     *
     * @param id The id of the group.
     */
    void deleteGroup(int id);

    /**
     * Obtains the unique id of a group.
     *
     * @param name The name of the group.
     * @return The unique id of the group.
     */
    int getUniqueId(String name);

    /**
     * Obtains the name of a group.
     *
     * @param id The id of the group.
     * @return The name of the group.
     */
    String getName(int id);

    /**
     * Renames a group.
     *
     * @param id      The id of the group.
     * @param newName The new name of the group.
     */
    void rename(int id, String newName);

    /**
     * Renames a group.
     *
     * @param oldName The old name of the group.
     * @param newName The new name of the group.
     */
    void rename(String oldName, String newName);

    /**
     * Obtains a list of all unique ids of the groups.
     *
     * @return A list of all unique ids of the groups.
     */
    List<Integer> getUniqueIds();

    /**
     * Obtains a list of all names of the groups.
     *
     * @return A list of all names of the groups.
     */
    List<String> getNames();

    /**
     * Loads all expired things.
     */
    void loadExpired();

    /**
     * Obtains the default group.
     * The default group is the group with the id 1.
     */
    int getDefaultGroup();

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

package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.color.GroupColor;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a group in the MurmelAPI.
 * <p>
 * This interface is used to define a group that can be managed by the MurmelAPI.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link GroupProvider} interface extends this interface to provide additional functionality.
 * </p>
 */
public sealed interface Group permits GroupProvider {
    /**
     * Checks whether a group exists with the specified id.
     *
     * @param id The id of the group to check.
     * @return {@code true} if the id is greater than 0 and a corresponding group record exists; {@code false} otherwise.
     */
    boolean existsGroup(int id);

    /**
     * Checks whether a group exists with the specified group name.
     *
     * @param groupName The group name to check.
     * @return {@code true} if groupName is not null and a corresponding group record exists; {@code false} otherwise.
     */
    boolean existsGroup(String groupName);

    /**
     * Creates a new group with the specified parameters.
     *
     * @param groupName The name of the group.
     * @param priority  The priority level of the group.
     * @param teamId    A string representing part of the team sort order (combined with groupName).
     * @param createdBy The id of the user who creates the group.
     * @return The number of rows affected by the insertion, or 0 if input parameters are invalid.
     */
    int createGroup(String groupName, int priority, String teamId, int createdBy);

    /**
     * Deletes the group with the specified id.
     *
     * @param id The id of the group to delete.
     * @return The number of rows affected by the deletion, or 0 if the id is invalid.
     */
    int deleteGroup(int id);

    /**
     * Retrieves the id of a group by its group name.
     *
     * @param groupName The name of the group.
     * @return The group id if found; returns -1 if groupName is null or the record doesn't exist.
     */
    int getId(String groupName);

    /**
     * Retrieves the group name for the group with the specified id.
     *
     * @param id The id of the group.
     * @return The group name as a String, or null if the id is invalid.
     */
    String getGroupName(int id);

    /**
     * Renames the group with the specified id.
     *
     * @param id        The id of the group to rename.
     * @param newName   The new name for the group.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int rename(int id, String newName, int updatedBy);

    /**
     * Retrieves a list of all group ids.
     *
     * @return A List of integers representing the group ids.
     */
    List<Integer> getIds();

    /**
     * Retrieves a list of all group names.
     *
     * @return A List of strings representing the group names.
     */
    List<String> getGroupNames();

    /**
     * Retrieves the priority of the group with the specified id.
     *
     * @param id The group id.
     * @return The priority as an integer, or 0 if the id is invalid.
     */
    int getPriority(int id);

    /**
     * Updates the priority of the group with the specified id.
     *
     * @param id        The group id.
     * @param priority  The new priority value.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setPriority(int id, int priority, int updatedBy);

    /**
     * Retrieves the team sort value for the group with the specified id.
     *
     * @param id The group id.
     * @return The teamSort value as a String, or null if the id is invalid.
     */
    String getTeamSort(int id);

    /**
     * Updates the team sort value for the group with the specified id.
     *
     * @param id        The group id.
     * @param teamSort  The new team sort value.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setTeamSort(int id, String teamSort, int updatedBy);

    /**
     * Retrieves the id of the user who created the group.
     *
     * @param id The group id.
     * @return The creator's user id, or -2 if the group id is invalid.
     */
    int getCreatedBy(int id);

    /**
     * Retrieves the creation timestamp of the group.
     *
     * @param id The group id.
     * @return A Timestamp representing when the group was created, or {@code null} if the id is invalid.
     */
    Timestamp getCreatedAt(int id);

    /**
     * Returns a formatted date string representing the creation date of the group.
     *
     * @param id The group id.
     * @return A formatted date string, or {@code null} if the creation timestamp is unavailable.
     */
    String getCreatedDate(int id);

    /**
     * Retrieves the id of the user who last updated the group.
     *
     * @param id The group id.
     * @return The updater's user id, or -2 if the id is invalid.
     */
    int getUpdatedBy(int id);

    /**
     * Retrieves the last update timestamp of the group.
     *
     * @param id The group id.
     * @return A Timestamp representing the last update time, or {@code null} if the id is invalid.
     */
    Timestamp getUpdatedAt(int id);

    /**
     * Returns a formatted date string representing the last update date of the group.
     *
     * @param id The group id.
     * @return A formatted date string, or {@code null} if the last update timestamp is unavailable.
     */
    String getUpdatedDate(int id);

    /**
     * Creates a default group with the specified group name if it does not already exist.
     * A default group is created using predefined default values for priority, team sort, and group colors.
     *
     * @param groupName The name of the default group to create.
     */
    void createDefaultGroup(String groupName);

    /**
     * Returns the GroupColor instance associated with this GroupProvider.
     * If the instance is not already initialized, a new GroupColorProvider is created.
     *
     * @return The GroupColor instance.
     */
    GroupColor getColor();
}

package de.murmelmeister.murmelapi.group;

import de.murmelmeister.murmelapi.group.color.GroupColor;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;

import java.sql.Timestamp;
import java.util.List;

/**
 * Group interface to manage groups.
 */
public sealed interface Group permits GroupProvider {
    /**
     * Checks if a group with the specified group ID exists.
     *
     * @param groupId The unique identifier of the group to check.
     * @return True if the group exists, false otherwise.
     */
    boolean existsGroup(int groupId);

    /**
     * Checks if a group with the specified group name exists.
     *
     * @param groupName The name of the group to check.
     * @return True if the group exists, false otherwise.
     */
    boolean existsGroup(String groupName);

    /**
     * Creates a new group with the specified details.
     *
     * @param groupName The name of the new group to be created.
     * @param createdBy The ID of the user creating the group.
     * @param priority  The priority level of the group.
     * @param teamId    The ID of the team to which the group belongs.
     */
    void createNewGroup(String groupName, int createdBy, int priority, String teamId);

    /**
     * Deletes a group with the specified group ID.
     *
     * @param executorId The unique identifier of the user performing the deletion.
     * @param groupId    The unique identifier of the group to be deleted.
     */
    void deleteGroup(int executorId, int groupId);

    /**
     * Retrieves the unique identifier for the specified group name.
     *
     * @param groupName The name of the group whose unique identifier is to be retrieved.
     * @return The unique identifier associated with the specified group name.
     */
    int getUniqueId(String groupName);

    /**
     * Retrieves the name of the group associated with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose name is to be retrieved.
     * @return The name of the group corresponding to the provided group ID.
     */
    String getName(int groupId);

    /**
     * Renames the specified group to a new name.
     *
     * @param executorId The unique identifier of the user performing the rename operation.
     * @param groupId    The unique identifier of the group to be renamed.
     * @param newName    The new name for the group.
     */
    void rename(int executorId, int groupId, String newName);

    /**
     * Retrieves a list of unique identifiers for the groups.
     *
     * @return A list of integers representing the unique identifiers for the groups.
     */
    List<Integer> getUniqueIds();

    /**
     * Retrieves a list of names associated with the groups.
     *
     * @return A list of strings representing the names of the groups.
     */
    List<String> getNames();

    /**
     * Retrieves the priority level assigned to a group specified by its unique identifier.
     *
     * @param groupId The unique identifier of the group whose priority is to be retrieved.
     * @return The priority level of the specified group as an integer.
     */
    int getPriority(int groupId);

    /**
     * Updates the priority level of a specified group.
     *
     * @param executorId The unique identifier of the user performing the update.
     * @param groupId    The unique identifier of the group whose priority is to be set.
     * @param priority   The new priority level to assign to the group.
     */
    void setPriority(int executorId, int groupId, int priority);

    /**
     * Retrieves the team sort value associated with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose team sort value is to be retrieved.
     * @return The team sort value of the specified group as a string.
     */
    String getTeamSort(int groupId);

    /**
     * Sets the team sort order for the specified group.
     *
     * @param executorId The unique identifier of the user performing the operation.
     * @param groupId    The unique identifier of the group whose team sort is being set.
     * @param teamSort   The new team sort value to assign to the group.
     */
    void setTeamSort(int executorId, int groupId, String teamSort);

    /**
     * Retrieves the unique identifier of the user who created the specified group.
     *
     * @param groupId The unique identifier of the group whose creator's ID is to be retrieved.
     * @return The unique identifier of the user who created the specified group.
     */
    int getCreatedBy(int groupId);

    /**
     * Retrieves the creation timestamp of the group specified by its unique ID.
     *
     * @param groupId The unique identifier of the group whose creation timestamp is to be retrieved.
     * @return The creation timestamp of the group as a {@code Timestamp}.
     */
    Timestamp getCreatedAt(int groupId);

    /**
     * Retrieves the unique identifier of the user who last modified the group
     * specified by its unique identifier.
     *
     * @param groupId The unique identifier of the group whose modifier is to be retrieved.
     * @return The unique identifier of the user who last modified the specified group.
     */
    int getModifiedBy(int groupId);

    /**
     * Retrieves the timestamp indicating when the specified group was last modified.
     *
     * @param groupId The unique identifier of the group whose modification timestamp is to be retrieved.
     * @return A {@code Timestamp} object representing the last modification time of the specified group.
     */
    Timestamp getModifiedAt(int groupId);

    /**
     * Creates a default group with the specified name.
     *
     * @param groupName The name of the group to be created.
     */
    void createDefaultGroup(String groupName);

    /**
     * Loads expired group data for processing or further operations.
     * This method fetches and handles groups that have surpassed their expiration criteria.
     * Typically used to manage or clean up expired group entries.
     */
    void loadExpired();

    /**
     * Retrieves the color information associated with a group.
     *
     * @return An instance of {@code GroupColor} representing the color configuration of the group.
     */
    GroupColor getColor();

    /**
     * Retrieves the parent information associated with the group.
     *
     * @return An instance of {@code GroupParent} representing the parent configuration of the group.
     */
    GroupParent getParent();

    /**
     * Retrieves the permission details associated with the group.
     *
     * @return An instance of {@code GroupPermission} representing the permission configuration of the group.
     */
    GroupPermission getPermission();
}

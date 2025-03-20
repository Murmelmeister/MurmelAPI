package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;

/**
 * Group parent interface to manage group parents.
 */
public sealed interface GroupParent permits GroupParentProvider {
    /**
     * Checks whether a parent exists for a specified group.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent to check.
     * @return True if the specified parent exists for the group; otherwise, false.
     */
    boolean existsParent(int groupId, int parentId);

    /**
     * Adds a parent relationship for a group. If the specified parent already exists
     * for the group, the method will not perform any action.
     *
     * @param executorId The unique identifier of the user performing this action.
     * @param groupId    The unique identifier of the group to which the parent will be added.
     * @param parentId   The unique identifier of the parent to be added.
     * @param time       The duration in milliseconds until the parent relationship expires.
     *                   Use -1 for no expiration.
     */
    void addParent(int executorId, int groupId, int parentId, long time);

    /**
     * Removes the association between a specified parent and a group.
     *
     * @param executorId The unique identifier of the user performing this action.
     * @param groupId  The unique identifier of the group whose parent should be removed.
     * @param parentId The unique identifier of the parent to be removed.
     */
    void removeParent(int executorId, int groupId, int parentId);

    /**
     * Removes all parent associations for a specified group.
     *
     * @param executorId The unique identifier of the user performing this action.
     * @param groupId The unique identifier of the group whose parent associations should be cleared.
     */
    void clearParent(int executorId, int groupId);

    /**
     * Retrieves the list of IDs of all parent associations for the specified group.
     *
     * @param groupId The unique identifier of the group whose parent IDs are being retrieved.
     * @return A list of integers representing the IDs of the parents associated with the specified group.
     */
    List<Integer> getParentIds(int groupId);

    /**
     * Retrieves the names of all parent entities associated with the specified group.
     *
     * @param group   The instance of the {@code Group} interface representing the group.
     * @param groupId The unique identifier of the group whose parent names are to be retrieved.
     * @return A list of strings representing the names of the parent entities associated with the group.
     */
    List<String> getParentNames(Group group, int groupId);

    /**
     * Retrieves the expiration time of the association between the specified group and parent.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return A {@code Timestamp} object representing the expiration date and time of the association.
     */
    Timestamp getExpiredAt(int groupId, int parentId);

    /**
     * Updates the expiration time for the association between the specified group and parent.
     * If the expiration is successfully updated, returns a confirmation status or message.
     *
     * @param executorId The unique identifier of the user performing this action.
     * @param groupId    The unique identifier of the group whose parent association is being updated.
     * @param parentId   The unique identifier of the parent whose association expiration is being updated.
     * @param time       The new expiration time in milliseconds since the epoch. Use -1 for no expiration.
     */
    void setExpiredAt(int executorId, int groupId, int parentId, long time);

    /**
     * Determines if the association between the specified group and parent is expired.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return True if the association is expired; otherwise, false.
     */
    boolean isExpired(int groupId, int parentId);

    /**
     * Retrieves the ID of the creator associated with the specified group and parent relationship.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return An integer representing the ID of the creator of the group-parent relationship.
     */
    int getCreatedBy(int groupId, int parentId);

    /**
     * Retrieves the creation timestamp of the association between the specified group and parent.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return A {@code Timestamp} object representing the creation date and time of the association.
     */
    Timestamp getCreatedAt(int groupId, int parentId);

    /**
     * Retrieves the unique identifier of the user who last modified the association
     * between the specified group and parent.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return An integer representing the ID of the user who last modified the association.
     */
    int getModifiedBy(int groupId, int parentId);

    /**
     * Retrieves the last modified timestamp of the association between the specified group and parent.
     *
     * @param groupId  The unique identifier of the group.
     * @param parentId The unique identifier of the parent.
     * @return A {@code Timestamp} representing the last modified time of the association.
     * If the association does not exist or has not been modified, it may return null or
     * a specific value to indicate such a case.
     */
    Timestamp getModifiedAt(int groupId, int parentId);

    /**
     * Loads all expired parent associations for the specified group. This operation is
     * performed on behalf of a specific user, and it handles any necessary processing related
     * to expired associations within the given group.
     */
    void loadExpired();
}

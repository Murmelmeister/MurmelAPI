package de.murmelmeister.murmelapi.group.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;

/**
 * GroupParent is an interface that provides methods to manage group parents in the database.
 * It defines methods for checking existence, adding, removing, and retrieving parent groups.
 * It also provides methods for managing expiration dates and user information related to group parents.
 */
public sealed interface GroupParent permits GroupParentProvider {
    /**
     * Checks if a parent relationship exists between the specified group and parent.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return {@code true} if both IDs are greater than 0 and a corresponding relationship exists; {@code false} otherwise.
     */
    boolean existsParent(int groupId, int parentId);

    /**
     * Adds a parent relationship between the specified group and parent.
     * An expiration time can be set for the relationship; if the provided time is -1, then no expiration is set.
     *
     * @param groupId   The ID of the child group.
     * @param parentId  The ID of the parent group.
     * @param time      The duration in milliseconds until expiration, or -1 for no expiration.
     * @param createdBy The ID of the user creating the relationship.
     * @return The number of rows affected by the insertion, or 0 if input parameters are invalid.
     */
    int addParent(int groupId, int parentId, long time, int createdBy);

    /**
     * Removes the specified parent relationship between the given group and parent.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The number of rows affected by the deletion, or 0 if input parameters are invalid.
     */
    int removeParent(int groupId, int parentId);

    /**
     * Clears all parent relationships for the specified group.
     *
     * @param groupId The ID of the child group.
     * @return The number of rows affected by the deletion, or 0 if the group ID is invalid.
     */
    int clearParent(int groupId);

    /**
     * Clears all parent relationships for groups other than the specified parent.
     *
     * @param parentId The ID of the parent group whose relationships should not be cleared.
     * @return The number of rows affected by the operation, or 0 if the provided parent ID is invalid.
     */
    int clearOtherParent(int parentId);

    /**
     * Retrieves a list of active parent IDs for the specified group.
     * A parent relationship is considered active if it has no expiration or its expiration time is in the future.
     *
     * @param groupId The ID of the child group.
     * @return A List of parent IDs, or {@code null} if the group ID is invalid.
     */
    List<Integer> getParentIds(int groupId);

    /**
     * Retrieves a list of active parent group names for the specified group.
     * The provided Group instance is used to convert parent IDs to group names.
     *
     * @param group   A Group instance used to retrieve group names.
     * @param groupId The ID of the child group.
     * @return A List of parent group names, or {@code null} if the group ID is invalid.
     */
    List<String> getParentNames(Group group, int groupId);

    /**
     * Retrieves the expiration timestamp for the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The expiration timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getExpiredAt(int groupId, int parentId);

    /**
     * Returns a formatted date string for the expiration time of the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return A formatted expiration date string, or {@code null} if no expiration is set.
     */
    String getExpiredDate(int groupId, int parentId);

    /**
     * Updates the expiration timestamp for the specified parent relationship.
     * If the provided time is -1, the expiration timestamp is set to {@code null}.
     *
     * @param groupId   The ID of the child group.
     * @param parentId  The ID of the parent group.
     * @param time      The duration in milliseconds until expiration, or -1 for no expiration.
     * @param updatedBy The ID of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setExpiredAt(int groupId, int parentId, long time, int updatedBy);

    /**
     * Retrieves the ID of the user who created the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The creator's user ID, or -2 if input parameters are invalid.
     */
    int getCreatedBy(int groupId, int parentId);

    /**
     * Retrieves the creation timestamp for the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The creation timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getCreatedAt(int groupId, int parentId);

    /**
     * Returns a formatted date string representing the creation date of the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The formatted creation date string, or {@code null} if unavailable.
     */
    String getCreatedDate(int groupId, int parentId);

    /**
     * Retrieves the ID of the user who last updated the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The updater's user ID, or -2 if input parameters are invalid.
     */
    int getUpdatedBy(int groupId, int parentId);

    /**
     * Retrieves the last update timestamp for the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The update timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getUpdatedAt(int groupId, int parentId);

    /**
     * Returns a formatted date string representing the last update date of the specified parent relationship.
     *
     * @param groupId  The ID of the child group.
     * @param parentId The ID of the parent group.
     * @return The formatted update date string, or {@code null} if the update timestamp is unavailable.
     */
    String getUpdatedDate(int groupId, int parentId);

    /**
     * Removes all expired parent relationships from the database.
     * Expired relationships are those with a non-null expiration timestamp that is in the past.
     *
     * @return The number of rows affected by the removal of expired relationships.
     */
    int loadExpired();
}

package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;

import java.sql.Timestamp;
import java.util.List;

/**
 * Represents a user-parent relationship in the MurmelAPI.
 * <p>
 * This interface is used to define the operations that can be performed on user-parent relationships.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link UserParentProvider} interface extends this interface to provide additional functionality.
 * </p>
 */
public sealed interface UserParent permits UserParentProvider {
    /**
     * Checks if a parent relationship exists for the specified user and parent.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return {@code true} if both ids are greater than 0 and a matching record exists; {@code false} otherwise.
     */
    boolean existsParent(int userId, int parentId);

    /**
     * Adds a parent relationship for the specified user.
     * The relationship may have an expiration time; if the provided time is -1, the relationship never expires.
     *
     * @param userId    The id of the child user.
     * @param parentId  The id of the parent group.
     * @param time      The duration in milliseconds until expiration, or -1 for no expiration.
     * @param createdBy The id of the user creating the parent relationship.
     * @return The number of rows affected by the insertion, or 0 if input parameters are invalid.
     */
    int addParent(int userId, int parentId, long time, int createdBy);

    /**
     * Removes the specified parent relationship for the given user.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The number of rows affected by the deletion, or 0 if input parameters are invalid.
     */
    int removeParent(int userId, int parentId);

    /**
     * Clears all parent relationships for the specified user.
     *
     * @param userId The id of the child user.
     * @return The number of rows affected by the deletion, or 0 if the user id is invalid.
     */
    int clearParent(int userId);

    /**
     * Clears all relationships associated with a specific parent group, excluding other relevant parent relationships.
     *
     * @param parentId The id of the parent group for which relationships should be cleared.
     * @return The number of rows affected by the operation, or 0 if the parent id is invalid.
     */
    int clearOtherParent(int parentId);

    /**
     * Retrieves a list of active parent ids for the specified user.
     * Only parent relationships that have not expired are returned.
     *
     * @param userId The id of the child user.
     * @return A list of parent ids, or {@code null} if the user id is invalid.
     */
    List<Integer> getParentIds(int userId);

    /**
     * Retrieves a list of parent group names for the specified user.
     * The provided Group instance is used to resolve parent ids to group names.
     *
     * @param group  The Group instance used to obtain group names.
     * @param userId The id of the child user.
     * @return A list of parent group names, or {@code null} if the user id is invalid.
     */
    List<String> getParentNames(Group group, int userId);

    /**
     * Retrieves the highest priority among the active parent groups for the specified user.
     *
     * @param group  The Group instance used to retrieve group priority.
     * @param userId The id of the child user.
     * @return The highest priority value, or -1 if no active parent groups exist.
     */
    int getHighestPriority(Group group, int userId);

    /**
     * Retrieves the expiration timestamp for the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The expiration timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getExpiredAt(int userId, int parentId);

    /**
     * Returns a formatted date string representing the expiration time of the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return A formatted expiration date string, or {@code null} if no expiration time is set.
     */
    String getExpiredDate(int userId, int parentId);

    /**
     * Updates the expiration timestamp for the specified parent relationship.
     * If the provided time is -1, the expiration timestamp is set to {@code null} (i.e. no expiration).
     *
     * @param userId    The id of the child user.
     * @param parentId  The id of the parent group.
     * @param time      The duration in milliseconds until expiration, or -1 for no expiration.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if input parameters are invalid.
     */
    int setExpiredAt(int userId, int parentId, long time, int updatedBy);

    /**
     * Retrieves the id of the user who created the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The creator's user id, or -2 if input parameters are invalid.
     */
    int getCreatedBy(int userId, int parentId);

    /**
     * Retrieves the creation timestamp of the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The creation timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getCreatedAt(int userId, int parentId);

    /**
     * Returns a formatted date string representing the creation date of the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The formatted creation date, or {@code null} if the creation timestamp is unavailable.
     */
    String getCreatedDate(int userId, int parentId);

    /**
     * Retrieves the id of the user who last updated the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The updater's user id, or -2 if input parameters are invalid.
     */
    int getUpdatedBy(int userId, int parentId);

    /**
     * Retrieves the last update timestamp of the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The last update timestamp, or {@code null} if input parameters are invalid.
     */
    Timestamp getUpdatedAt(int userId, int parentId);

    /**
     * Returns a formatted date string representing the last update date of the specified parent relationship.
     *
     * @param userId   The id of the child user.
     * @param parentId The id of the parent group.
     * @return The formatted update date, or {@code null} if the update timestamp is unavailable.
     */
    String getUpdatedDate(int userId, int parentId);

    /**
     * Removes all expired parent relationships from the database.
     *
     * @return The number of rows affected by the removal of expired relationships.
     */
    int loadExpired();
}

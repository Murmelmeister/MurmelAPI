package de.murmelmeister.murmelapi.user.parent;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.user.User;

import java.sql.SQLException;
import java.util.List;

/**
 * User parent interface to manage user parents.
 */
public sealed interface UserParent permits UserParentProvider {
    /**
     * Checks if a parent exists.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return True if the parent exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsParent(int userId, int parentId) throws SQLException;

    /**
     * Adds a parent to a user.
     *
     * @param userId    The id of the user.
     * @param creatorId The id of the creator.
     * @param parentId  The id of the parent.
     * @param time      The time the parent was added.
     * @throws SQLException If an SQL error occurs.
     */
    void addParent(int userId, int creatorId, int parentId, long time) throws SQLException;

    /**
     * Removes a parent from a user.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    void removeParent(int userId, int parentId) throws SQLException;

    /**
     * Clears all parents from a user.
     *
     * @param userId The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void clearParent(int userId) throws SQLException;

    /**
     * Obtains the parent id of a user.
     *
     * @param userId The id of the user.
     * @return The parent id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    int getParentId(int userId) throws SQLException;

    /**
     * Obtains all parent ids of a user.
     *
     * @param userId The id of the user.
     * @return A list of all parent ids of the user.
     * @throws SQLException If an SQL error occurs.
     */
    List<Integer> getParentIds(int userId) throws SQLException;

    /**
     * Obtains all parent names of a user.
     *
     * @param group  The group.
     * @param userId The id of the user.
     * @return A list of all parent names of the user.
     * @throws SQLException If an SQL error occurs.
     */
    List<String> getParentNames(Group group, int userId) throws SQLException;

    /**
     * Obtains the creator id of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The creator id of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    int getCreatorId(int userId, int parentId) throws SQLException;

    /**
     * Obtains the created time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The created time of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    long getCreatedTime(int userId, int parentId) throws SQLException;

    /**
     * Obtains the created date of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The created date of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    String getCreatedDate(int userId, int parentId) throws SQLException;

    /**
     * Obtains the expired time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The expired time of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    long getExpiredTime(int userId, int parentId) throws SQLException;

    /**
     * Obtains the expired date of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @return The expired date of the parent.
     * @throws SQLException If an SQL error occurs.
     */
    String getExpiredDate(int userId, int parentId) throws SQLException;

    /**
     * Sets the expired time of a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time the parent expires.
     * @throws SQLException If an SQL error occurs.
     */
    void setExpiredTime(int userId, int parentId, long time) throws SQLException;

    /**
     * Adds an expired time to a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time to add to the expired time.
     * @return An error message if the time is negative, otherwise an empty string.
     * @throws SQLException If an SQL error occurs.
     */
    String addExpiredTime(int userId, int parentId, long time) throws SQLException;

    /**
     * Removes an expired time from a parent.
     *
     * @param userId   The id of the user.
     * @param parentId The id of the parent.
     * @param time     The time to remove from the expired time.
     * @return An error message if the time is negative, otherwise an empty string.
     * @throws SQLException If an SQL error occurs.
     */
    String removeExpiredTime(int userId, int parentId, long time) throws SQLException;

    /**
     * Loads all expired parents of a user.
     *
     * @param user The user.
     * @throws SQLException If an SQL error occurs.
     */
    void loadExpired(User user) throws SQLException;
}

package de.murmelmeister.murmelapi.group.color;

import java.sql.Timestamp;

/**
 * Group color interface to manage group color.
 */
public sealed interface GroupColor permits GroupColorProvider {
    /**
     * Checks if a group with the specified group ID exists in the database.
     *
     * @param groupId The unique identifier of the group to be checked
     * @return True if the group exists, false otherwise
     */
    boolean existsGroup(int groupId);

    /**
     * Creates a new group entry in the database with the provided user and group IDs.
     * If the group already exists, the method returns without taking any action.
     *
     * @param executorId The unique identifier of the user initiating the group creation
     * @param groupId    The unique identifier of the group to be created
     */
    void createGroup(int executorId, int groupId);

    /**
     * Creates a new group with detailed customization options for chat, tab, and team settings.
     * If the group with the specified groupId already exists, no action is taken.
     *
     * @param executorId The unique identifier of the user initiating the group creation.
     * @param groupId    The unique identifier of the group to be created.
     * @param chatPrefix The prefix to be used for chat messages.
     * @param chatSuffix The suffix to be used for chat messages.
     * @param chatColor  The color code to be applied to chat messages.
     * @param tabPrefix  The prefix to be displayed in the tab list.
     * @param tabSuffix  The suffix to be displayed in the tab list.
     * @param tabColor   The color code to be applied to the tab list.
     * @param teamPrefix The prefix to be used for team names.
     * @param teamSuffix The suffix to be used for team names.
     * @param teamColor  The color code to be applied to team names.
     */
    void createGroup(int executorId, int groupId, String chatPrefix, String chatSuffix, String chatColor, String tabPrefix, String tabSuffix, String tabColor, String teamPrefix, String teamSuffix, String teamColor);

    /**
     * Deletes the group with the specified group ID from the database.
     * This operation is performed by the user with the given user ID.
     *
     * @param groupId    The unique identifier of the group to be deleted.
     */
    void deleteGroup(int groupId);

    /**
     * Retrieves the prefix associated with the specified group and group color type.
     *
     * @param groupId The unique identifier of the group.
     * @param type    The type of group color customization (e.g., Chat, Tab, or Team).
     * @return The prefix string associated with the specified group and type.
     */
    String getPrefix(int groupId, GroupColorType type);

    /**
     * Retrieves the suffix associated with the specified group and group color type.
     *
     * @param groupId The unique identifier of the group.
     * @param type    The type of group color customization (e.g., Chat, Tab, or Team).
     * @return The suffix string associated with the specified group and type.
     */
    String getSuffix(int groupId, GroupColorType type);

    /**
     * Retrieves the color associated with the specified group and group color type.
     *
     * @param groupId The unique identifier of the group.
     * @param type    The type of group color customization (e.g., Chat, Tab, or Team).
     * @return The color string associated with the specified group and type.
     */
    String getColor(int groupId, GroupColorType type);

    /**
     * Sets the prefix associated with the specified group and group color type.
     *
     * @param executorId The unique identifier of the user initiating the operation.
     * @param groupId    The unique identifier of the group whose prefix is being set.
     * @param type       The type of group color customization (e.g., Chat, Tab, or Team).
     * @param prefix     The prefix string to be set for the specified group and type.
     */
    void setPrefix(int executorId, int groupId, GroupColorType type, String prefix);

    /**
     * Sets the suffix associated with the specified group and group color type.
     *
     * @param executorId The unique identifier of the user initiating the operation.
     * @param groupId    The unique identifier of the group whose suffix is being set.
     * @param type       The type of group color customization (e.g., Chat, Tab, or Team).
     * @param suffix     The suffix string to be set for the specified group and type.
     */
    void setSuffix(int executorId, int groupId, GroupColorType type, String suffix);

    /**
     * Sets the color associated with the specified group and group color type.
     *
     * @param executorId The unique identifier of the user initiating the operation.
     * @param groupId    The unique identifier of the group whose color is being set.
     * @param type       The type of group color customization (e.g., Chat, Tab, or Team).
     * @param color      The color string to be set for the specified group and type.
     */
    void setColor(int executorId, int groupId, GroupColorType type, String color);

    /**
     * Retrieves the ID of the user who created the specified group.
     *
     * @param groupId The unique identifier of the group whose creator ID is to be retrieved.
     * @return The unique identifier of the user who created the group.
     */
    int getCreatedBy(int groupId);

    /**
     * Retrieves the creation timestamp of the group with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose creation timestamp is to be retrieved.
     * @return The creation timestamp of the specified group, or null if the group does not exist.
     */
    Timestamp getCreatedAt(int groupId);

    /**
     * Retrieves the creation date of the group with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose creation date is to be retrieved.
     * @return A string representing the creation date of the specified group.
     */
    String getCreatedDate(int groupId);

    /**
     * Retrieves the ID of the user who last modified the group with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose last modifier is to be retrieved.
     * @return The unique identifier of the user who last modified the group.
     */
    int getModifiedBy(int groupId);

    /**
     * Retrieves the timestamp at which the specified group was last modified.
     *
     * @param groupId The unique identifier of the group whose modification timestamp is to be retrieved.
     * @return A {@code Timestamp} object representing the last modification time of the specified group.
     */
    Timestamp getModifiedAt(int groupId);

    /**
     * Retrieves the modification date of the group with the specified group ID.
     *
     * @param groupId The unique identifier of the group whose modification date is to be retrieved.
     * @return A string representing the modification date of the specified group.
     */
    String getModifiedDate(int groupId);
}

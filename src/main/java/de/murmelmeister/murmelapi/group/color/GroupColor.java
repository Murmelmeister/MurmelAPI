package de.murmelmeister.murmelapi.group.color;

import java.sql.Timestamp;

/**
 * Represents a color associated with a group in the MurmelAPI.
 * <p>
 * This interface is used to define a color that can be managed by the MurmelAPI.
 * It is a marker interface and does not contain any methods.
 * </p>
 * <p>
 * The {@link GroupColorProvider} interface extends this interface to provide additional functionality.
 * </p>
 */
public sealed interface GroupColor permits GroupColorProvider {
    /**
     * Checks if a group color record exists for the specified group.
     *
     * @param groupId The id of the group to check.
     * @return {@code true} if the group id is greater than 0 and a record exists; {@code false} otherwise.
     */
    boolean existsGroup(int groupId);

    /**
     * Creates a new group color record with the provided color settings.
     *
     * @param groupId          The id of the group.
     * @param chatPrefix       The chat prefix.
     * @param chatSuffix       The chat suffix.
     * @param chatColor        The chat color.
     * @param chatMessageColor The chat message color.
     * @param tabPrefix        The tab prefix.
     * @param tabSuffix        The tab suffix.
     * @param tabColor         The tab color.
     * @param teamPrefix       The team prefix.
     * @param teamSuffix       The team suffix.
     * @param teamColor        The team color.
     * @param createdBy        The id of the user creating this record.
     * @return The number of rows affected by the insertion, or 0 if the group id is invalid or the creator is invalid.
     */
    int createGroup(int groupId, String chatPrefix, String chatSuffix, String chatColor, String chatMessageColor,
                    String tabPrefix, String tabSuffix, String tabColor,
                    String teamPrefix, String teamSuffix, String teamColor,
                    int createdBy);

    /**
     * Creates a new group color record with default color values.
     * This method delegates to createGroup by passing default values and a default teamColor of "7".
     *
     * @param groupId   The id of the group.
     * @param createdBy The id of the user creating this record.
     * @return The number of rows affected by the insertion.
     */
    int createGroup(int groupId, int createdBy);

    /**
     * Deletes the group color record for the specified group.
     *
     * @param groupId The id of the group whose record should be deleted.
     * @return The number of rows affected by the deletion, or 0 if the group id is invalid.
     */
    int deleteGroup(int groupId);

    /**
     * Retrieves the prefix value for a given group and color type.
     *
     * @param groupId The id of the group.
     * @param type    The GroupColorType specifying which context (e.g., Chat, Tab, Team) to retrieve.
     * @return The prefix as a String, or {@code null} if the group id is invalid.
     */
    String getPrefix(int groupId, GroupColorType type);

    /**
     * Retrieves the suffix value for a given group and color type.
     *
     * @param groupId The id of the group.
     * @param type    The GroupColorType specifying which context (e.g., Chat, Tab, Team) to retrieve.
     * @return The suffix as a String, or {@code null} if the group id is invalid.
     */
    String getSuffix(int groupId, GroupColorType type);

    /**
     * Retrieves the color value for a given group and color type.
     *
     * @param groupId The id of the group.
     * @param type    The GroupColorType specifying which context (e.g., Chat, ChatMessage, Tab, Team) to retrieve.
     * @return The color as a String, or {@code null} if the group id is invalid.
     */
    String getColor(int groupId, GroupColorType type);

    /**
     * Sets a new prefix for the specified group and color type.
     *
     * @param groupId   The id of the group.
     * @param type      The GroupColorType specifying which context (e.g., Chat, Tab, Team) to update.
     * @param prefix    The new prefix value.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if the group id or updater is invalid.
     */
    int setPrefix(int groupId, GroupColorType type, String prefix, int updatedBy);

    /**
     * Sets a new suffix for the specified group and color type.
     *
     * @param groupId   The id of the group.
     * @param type      The GroupColorType specifying which context (e.g., Chat, Tab, Team) to update.
     * @param suffix    The new suffix value.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if the group id or updater is invalid.
     */
    int setSuffix(int groupId, GroupColorType type, String suffix, int updatedBy);

    /**
     * Sets a new color for the specified group and color type.
     *
     * @param groupId   The id of the group.
     * @param type      The GroupColorType specifying which context (e.g., Chat, ChatMessage, Tab, Team) to update.
     * @param color     The new color value.
     * @param updatedBy The id of the user performing the update.
     * @return The number of rows affected by the update, or 0 if the group id or updater is invalid.
     */
    int setColor(int groupId, GroupColorType type, String color, int updatedBy);

    /**
     * Retrieves the id of the user who created the group color record.
     *
     * @param groupId The id of the group.
     * @return The user id of the creator, or -2 if the group id is invalid.
     */
    int getCreatedBy(int groupId);

    /**
     * Retrieves the timestamp when the group color record was created.
     *
     * @param groupId The id of the group.
     * @return The creation timestamp, or {@code null} if the group id is invalid.
     */
    Timestamp getCreatedAt(int groupId);

    /**
     * Returns a formatted date string representing the creation date of the group color record.
     *
     * @param groupId The id of the group.
     * @return A formatted date string, or {@code null} if the creation timestamp is not available.
     */
    String getCreatedDate(int groupId);

    /**
     * Retrieves the id of the user who last updated the group color record.
     *
     * @param groupId The id of the group.
     * @return The user id of the updater, or -2 if the group id is invalid.
     */
    int getUpdatedBy(int groupId);

    /**
     * Retrieves the timestamp when the group color record was last updated.
     *
     * @param groupId The id of the group.
     * @return The last updated timestamp, or {@code null} if the group id is invalid.
     */
    Timestamp getUpdatedAt(int groupId);

    /**
     * Returns a formatted date string representing the last update date of the group color record.
     *
     * @param groupId The id of the group.
     * @return A formatted date string, or {@code null} if the last updated timestamp is not available.
     */
    String getUpdatedDate(int groupId);
}

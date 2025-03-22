package de.murmelmeister.murmelapi.punishment;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

/**
 * Represents a user who has been punished or is subject to a punishment.
 */
public sealed interface PunishmentUser permits PunishmentUserProvider {
    /**
     * Checks if a record or punishment exists for a specific type and user.
     *
     * @param userId The ID of the user to check for the existence of the record.
     * @param typeId The ID of the punishment type to check.
     * @return true if the record exists, false otherwise.
     */
    boolean exists(int userId, int typeId);

    /**
     * Applies a punishment to a user.
     *
     * @param userId      The ID of the user being punished
     * @param typeId      The ID representing the type of punishment
     * @param executorId  The ID of the executor performing the punishment
     * @param inetAddress The network address associated with the user
     * @param reasonId    The ID representing the reason for the punishment
     */
    void punish(int userId, int typeId, int executorId, InetAddress inetAddress, int reasonId);

    /**
     * Removes the punishment for the specified user and type.
     *
     * @param userId The identifier of the user
     * @param typeId The identifier of the punishment type
     */
    void unpunished(int userId, int typeId);

    /**
     * Retrieves a list of user IDs associated with the specified punishment type.
     *
     * @param typeId The ID of the punishment type
     * @return A list of user IDs associated with the specified punishment type
     */
    List<Integer> getUsers(int typeId);

    /**
     * Retrieves the log identifier related to a specific type and user.
     *
     * @param userId The unique identifier of the user.
     * @param typeId The type identifier associated with the user.
     * @return The UUID representing the log identifier for the given type and user.
     */
    UUID getLogId(int userId, int typeId);

    /**
     * Retrieves the reason ID associated with the specified type and user ID.
     *
     * @param userId The ID of the user for whom the reason ID is requested.
     * @param typeId The ID representing the type of punishment.
     * @return The reason ID associated with the given type and user ID.
     */
    int getReasonId(int userId, int typeId);

    /**
     * Updates the reason associated with the punishment of a user.
     *
     * @param userId     The identifier of the user being punished.
     * @param typeId     The type identifier of the punishment.
     * @param executorId The identifier of the executor making the change.
     * @param reasonId   The identifier of the new reason for the punishment.
     */
    void setReasonId(int userId, int typeId, int executorId, int reasonId);

    /**
     * Retrieves the reason associated with a specified type and user by their respective IDs.
     *
     * @param userId The ID representing the user associated with the punishment or action
     * @param typeId The ID representing the type of the punishment or action
     * @return The reason as a string for the specified type and user
     */
    String getReason(int userId, int typeId);

    /**
     * Retrieves the duration of a punishment for a specified type and user.
     *
     * @param userId The ID of the user whose punishment duration is to be retrieved.
     * @param typeId The ID representing the type of punishment.
     * @return The duration of the punishment in milliseconds.
     */
    long getDuration(int userId, int typeId);

    /**
     * Retrieves the expiration time of a punishment for a specific user and type.
     *
     * @param userId The ID of the user associated with the punishment.
     * @param typeId The ID of the punishment type.
     * @return A {@code Timestamp} object representing the expiration date and time of the association.
     */
    Timestamp getExpiredAt(int userId, int typeId);

    /**
     * Sets the expiration time for a punishment record.
     *
     * @param userId     The identifier of the user being punished
     * @param typeId     The type of punishment being set
     * @param executorId The identifier of the executor making the change
     * @param time       The expiration time of the punishment, in milliseconds since epoch
     */
    void setExpiredAt(int userId, int typeId, int executorId, long time);

    /**
     * Determines whether the specified user is currently punished for the given type.
     *
     * @param userId The identifier of the user
     * @param typeId The identifier of the punishment type
     * @return true if the user is currently punished for the specified type, false otherwise
     */
    boolean isPunished(int userId, int typeId);

    /**
     * Retrieves the identifier of the user who created a punishment record.
     *
     * @param userId The ID of the affected user.
     * @param typeId The ID of the punishment type.
     * @return The ID of the user who created the punishment.
     */
    int getCreatedBy(int userId, int typeId);

    /**
     * Retrieves the timestamp representing when the entity associated with the specified type and user
     * was created.
     *
     * @param userId The ID of the user associated with the creation
     * @param typeId The ID of the type associated with the creation
     * @return The creation timestamp of the associated type and user
     */
    Timestamp getCreatedAt(int userId, int typeId);

    /**
     * Retrieves the ID of the user who last modified the punishment record.
     *
     * @param userId The user identifier related to the punishment record
     * @param typeId The type identifier associated with the punishment record
     * @return The ID of the user who last modified the punishment record
     */
    int getModifiedBy(int userId, int typeId);

    /**
     * Retrieves the timestamp of the last modification made to the punishment details
     * of a user specified by their type ID and user ID.
     *
     * @param userId The unique identifier for the user
     * @param typeId The unique identifier for the punishment type
     * @return A {@code Timestamp} representing the last modification time for the specified type and user
     */
    Timestamp getModifiedAt(int userId, int typeId);
}

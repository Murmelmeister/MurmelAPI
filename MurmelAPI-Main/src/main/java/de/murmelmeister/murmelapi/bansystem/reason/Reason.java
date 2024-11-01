package de.murmelmeister.murmelapi.bansystem.reason;

import java.util.List;

/**
 * The Reason interface provides methods to give Reason for the ban/mute system.
 */
public sealed interface Reason permits ReasonProvider {
    /**
     * Checks if a reason with the specified ID exists.
     *
     * @param id The ID of the reason to check.
     * @return True if a reason with the specified ID exists, false otherwise.
     */
    boolean exists(int id);

    /**
     * Adds a reason.
     *
     * @param reason The reason to be added.
     */
    void add(String reason);

    /**
     * Removes a reason.
     *
     * @param id The ID of the reason to be removed.
     */
    void remove(int id);

    /**
     * Updates the reason with the specified ID.
     *
     * @param id     The ID of the reason to update.
     * @param reason The updated reason.
     */
    void update(int id, String reason);

    /**
     * Retrieves a reason by its ID.
     *
     * @param id The ID of the reason to retrieve.
     * @return The reason associated with the specified ID, represented as a string.
     */
    String get(int id);

    /**
     * Retrieves a list of all reason IDs.
     *
     * @return A list of integers representing the IDs of all reasons.
     */
    List<Integer> getIds();
}

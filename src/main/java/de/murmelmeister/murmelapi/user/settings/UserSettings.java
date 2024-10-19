package de.murmelmeister.murmelapi.user.settings;

/**
 * User settings interface to manage user settings.
 */
public sealed interface UserSettings permits UserSettingsProvider {
    /**
     * Checks if a user exists.
     *
     * @param id The id of the user.
     * @return True if the user exists, otherwise false.
     */
    boolean existsUser(int id);

    /**
     * Creates a new user and checks if the user already exists.
     * If the user already exists, the method will return without creating a new user.
     *
     * @param id The id of the user.
     */
    void createUser(int id);

    /**
     * Deletes a user.
     *
     * @param id The id of the user.
     */
    void deleteUser(int id);

    /**
     * Obtains the first join time of a user.
     *
     * @param id The id of the user.
     * @return The first join time of the user.
     */
    long getFirstJoinTime(int id);

    /**
     * Obtains the first join date of a user.
     *
     * @param id The id of the user.
     * @return The first join date of the user.
     */
    String getFirstJoinDate(int id);

    /**
     * Sets the last quit time for a user identified by the given ID.
     * This method updates the last quit time of the user in the user settings database.
     *
     * @param id   The ID of the user.
     * @param time The last quit time to set for the user, represented as a long value.
     */
 //   void setLastQuitTime(int id, long time);

    /**
     * Retrieves the last quit time of a user.
     *
     * @param id The id of the user.
     * @return The last quit time of the user as a long value.
     */
//    long getLastQuitTime(int id);

    /**
     * Retrieves the last quit date of a user identified by the given ID.
     *
     * @param id The ID of the user.
     * @return The last quit date of the user as a string.
     */
//    String getLastQuitDate(int id);

    /**
     * Sets the online status of a user identified by the given ID.
     * This method updates the online status of the user in the user settings database.
     *
     * @param id     The ID of the user.
     * @param online The online status to set for the user, represented as a byte value.
     */
    void setOnline(int id, boolean online);

    /**
     * Retrieves the online status of a user identified by the given ID.
     * This method retrieves the online status of the user from the user settings database.
     *
     * @param id The ID of the user.
     * @return The online status of the user as a boolean value. True indicates that the user is online, false indicates that the user is offline.
     */
    boolean isOnline(int id);
}

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
}

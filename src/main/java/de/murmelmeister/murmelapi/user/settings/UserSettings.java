package de.murmelmeister.murmelapi.user.settings;

import java.sql.SQLException;

/**
 * User settings interface to manage user settings.
 */
public sealed interface UserSettings permits UserSettingsProvider {
    /**
     * Checks if a user exists.
     *
     * @param id The id of the user.
     * @return True if the user exists, otherwise false.
     * @throws SQLException If an SQL error occurs.
     */
    boolean existsUser(int id) throws SQLException;

    /**
     * Creates a new user and checks if the user already exists.
     * If the user already exists, the method will return without creating a new user.
     *
     * @param id The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void createUser(int id) throws SQLException;

    /**
     * Deletes a user.
     *
     * @param id The id of the user.
     * @throws SQLException If an SQL error occurs.
     */
    void deleteUser(int id) throws SQLException;

    /**
     * Gets the first join time of a user.
     *
     * @param id The id of the user.
     * @return The first join time of the user.
     * @throws SQLException If an SQL error occurs.
     */
    long getFirstJoinTime(int id) throws SQLException;

    /**
     * Gets the first join date of a user.
     *
     * @param id The id of the user.
     * @return The first join date of the user.
     * @throws SQLException If an SQL error occurs.
     */
    String getFirstJoinDate(int id) throws SQLException;
}

package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.settings.UserSettings;
import de.murmelmeister.murmelapi.user.settings.UserSettingsProvider;
import de.murmelmeister.murmelapi.utils.Database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    @Deprecated
    public static void main(String[] args) throws SQLException {
        try (FileInputStream file = new FileInputStream("./mysql.properties")) { // Test-Server path
            Properties properties = new Properties(System.getProperties());
            properties.load(file);
            Database.connect(properties.getProperty("DB_DRIVER"), properties.getProperty("DB_HOSTNAME"), properties.getProperty("DB_PORT"), properties.getProperty("DB_DATABASE"), properties.getProperty("DB_USERNAME"), properties.getProperty("DB_PASSWORD"));

            User user = MurmelAPI.getUser();
            UserSettings userSettings = MurmelAPI.getUserSettings();

            Database.disconnect();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the user provider.
     *
     * @return the user provider
     * @throws SQLException if an error occurs
     */
    public static User getUser() throws SQLException {
        return new UserProvider();
    }

    /**
     * Get the user settings provider.
     *
     * @return the user settings provider
     * @throws SQLException if an error occurs
     */
    public static UserSettings getUserSettings() throws SQLException {
        return new UserSettingsProvider();
    }
}

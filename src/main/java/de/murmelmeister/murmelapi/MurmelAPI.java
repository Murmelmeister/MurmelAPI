package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
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
            Group group = MurmelAPI.getGroup();

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
     * Get the group provider.
     *
     * @return the group provider
     * @throws SQLException if an error occurs
     */
    public static Group getGroup() throws SQLException {
        return new GroupProvider();
    }

    /**
     * Get the permission provider.
     *
     * @param group the group
     * @param user  the user
     * @return the permission provider
     */
    public static Permission getPermission(Group group, User user) {
        return new PermissionProvider(group, user);
    }
}

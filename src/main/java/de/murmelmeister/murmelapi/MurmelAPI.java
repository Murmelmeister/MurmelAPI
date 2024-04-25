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
    public static void main(String[] args) {
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

    private static final Group GROUP;
    private static final User USER;
    private static final Permission PERMISSION;

    static {
        try {
            USER = new UserProvider();
            GROUP = new GroupProvider();
            PERMISSION = new PermissionProvider(GROUP, USER);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get the group provider.
     *
     * @return the group provider
     */
    public static Group getGroup() {
        return GROUP;
    }

    /**
     * Get the user provider.
     *
     * @return the user provider
     */
    public static User getUser() {
        return USER;
    }

    /**
     * Get the permission provider.
     *
     * @return the permission provider
     */
    public static Permission getPermission() {
        return PERMISSION;
    }
}

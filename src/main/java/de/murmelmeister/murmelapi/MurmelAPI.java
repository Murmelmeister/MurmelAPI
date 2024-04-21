package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParent;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermission;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.settings.GroupSettings;
import de.murmelmeister.murmelapi.group.settings.GroupSettingsProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.parent.UserParent;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermission;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
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
            Group group = MurmelAPI.getGroup();
            GroupSettings groupSettings = MurmelAPI.getGroupSettings();
            UserPermission userPermission = MurmelAPI.getUserPermission();
            GroupPermission groupPermission = MurmelAPI.getGroupPermission();
            UserParent userParent = MurmelAPI.getUserParent();
            GroupParent groupParent = MurmelAPI.getGroupParent();

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
     * Get the group settings provider.
     *
     * @return the group settings provider
     * @throws SQLException if an error occurs
     */
    public static GroupSettings getGroupSettings() throws SQLException {
        return new GroupSettingsProvider();
    }

    /**
     * Get the user permission provider.
     *
     * @return the user permission provider
     * @throws SQLException if an error occurs
     */
    public static UserPermission getUserPermission() throws SQLException {
        return new UserPermissionProvider();
    }

    /**
     * Get the group permission provider.
     *
     * @return the group permission provider
     * @throws SQLException if an error occurs
     */
    public static GroupPermission getGroupPermission() throws SQLException {
        return new GroupPermissionProvider();
    }

    /**
     * Get the user parent provider.
     *
     * @return the user parent provider
     * @throws SQLException if an error occurs
     */
    public static UserParent getUserParent() throws SQLException {
        return new UserParentProvider();
    }

    /**
     * Get the group parent provider.
     *
     * @return the group parent provider
     * @throws SQLException if an error occurs
     */
    public static GroupParent getGroupParent() throws SQLException {
        return new GroupParentProvider();
    }

    /**
     * Get the permission provider.
     *
     * @param groupParent     The group parent provider.
     * @param groupPermission The group permission provider.
     * @param userParent      The user parent provider.
     * @param userPermission  The user permission provider.
     * @return the permission provider
     */
    public static Permission getPermission(GroupParent groupParent, GroupPermission groupPermission, UserParent userParent, UserPermission userPermission) {
        return new PermissionProvider(groupParent, groupPermission, userParent, userPermission);
    }
}

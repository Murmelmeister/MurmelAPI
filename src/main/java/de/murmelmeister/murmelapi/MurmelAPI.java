package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;

import java.sql.SQLException;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Group GROUP;
    private static final User USER;
    private static final Permission PERMISSION;
    private static final PlayTime PLAY_TIME;

    static {
        try {
            GROUP = new GroupProvider();
            USER = new UserProvider();
            PERMISSION = new PermissionProvider(GROUP, USER);
            PLAY_TIME = USER.getPlayTime();
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

    /**
     * Get the play time provider.
     *
     * @return the play time provider
     */
    public static PlayTime getPlayTime() {
        return PLAY_TIME;
    }
}

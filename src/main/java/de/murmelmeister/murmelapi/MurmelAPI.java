package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.bansystem.ban.Ban;
import de.murmelmeister.murmelapi.bansystem.ban.BanProvider;
import de.murmelmeister.murmelapi.bansystem.mute.Mute;
import de.murmelmeister.murmelapi.bansystem.mute.MuteProvider;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.playtime.PlayTime;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Group GROUP;
    private static final User USER;
    private static final Permission PERMISSION;
    private static final PlayTime PLAY_TIME;

    private static final Mute MUTE;
    private static final Ban BAN;

    static {
        GROUP = new GroupProvider();
        USER = new UserProvider();
        PERMISSION = new PermissionProvider(GROUP, USER);
        PLAY_TIME = USER.getPlayTime();
        MUTE = new MuteProvider();
        BAN = new BanProvider();
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

    /**
     * Get the mute provider.
     *
     * @return the mute provider
     */
    public static Mute getMute() {
        return MUTE;
    }

    /**
     * Get the ban provider.
     *
     * @return the ban provider
     */
    public static Ban getBan() {
        return BAN;
    }
}

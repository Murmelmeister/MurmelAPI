package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.bansystem.ban.Ban;
import de.murmelmeister.murmelapi.bansystem.ban.BanProvider;
import de.murmelmeister.murmelapi.bansystem.mute.Mute;
import de.murmelmeister.murmelapi.bansystem.mute.MuteProvider;
import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.time.JoinLogger;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.time.QuitLogger;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserProviderImpl;

import java.text.SimpleDateFormat;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Database DATABASE;

    private static String databaseName = "MurmelAPI";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    private static final Group GROUP;
    private static final UserProvider USER;
    private static final Permission PERMISSION;
    private static final PlayTime PLAY_TIME;
    private static final JoinLogger JOIN_LOGGER;
    private static final QuitLogger QUIT_LOGGER;

    private static final Mute MUTE;
    private static final Ban BAN;

    static {
        DATABASE = new Database();

        GROUP = new GroupProvider();
        USER = new UserProviderImpl();
        PERMISSION = new PermissionProvider(GROUP, USER);
        PLAY_TIME = USER.getPlayTime();
        JOIN_LOGGER = USER.getJoinLogger();
        QUIT_LOGGER = USER.getQuitLogger();
        MUTE = new MuteProvider();
        BAN = new BanProvider();
    }

    public static Database getDatabase() {
        return DATABASE;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static void setDatabaseName(String databaseName) {
        MurmelAPI.databaseName = databaseName;
    }

    public static SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    public static void setDateFormat(SimpleDateFormat dateFormat) {
        MurmelAPI.dateFormat = dateFormat;
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
    public static UserProvider getUser() {
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
     * Get the join logger provider.
     *
     * @return the join logger provider
     */
    public static JoinLogger getJoinLogger() {
        return JOIN_LOGGER;
    }

    /**
     * Get the quit logger provider.
     *
     * @return the quit logger provider
     */
    public static QuitLogger getQuitLogger() {
        return QUIT_LOGGER;
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

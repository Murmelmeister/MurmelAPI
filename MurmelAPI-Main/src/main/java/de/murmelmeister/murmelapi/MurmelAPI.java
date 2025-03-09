package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.logging.ActiveSession;
import de.murmelmeister.murmelapi.logging.ActiveSessionProvider;
import de.murmelmeister.murmelapi.logging.LoginHistory;
import de.murmelmeister.murmelapi.logging.LoginHistoryProvider;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.punishment.*;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLog;
import de.murmelmeister.murmelapi.punishment.log.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReason;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.time.PlayTime;
import de.murmelmeister.murmelapi.time.PlayTimeProvider;
import de.murmelmeister.murmelapi.user.User;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;

import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static String databaseName = "MurmelAPI";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final Database DATABASE;

    private static LoginHistory loginHistory;
    private static ActiveSession activeSession;
    private static User user;
    private static Group group;
    private static PlayTime playTime;
    private static Permission permission;
    private static PunishmentReason punishmentReason;
    private static PunishmentLog punishmentLog;
    private static PunishmentIP punishmentIP;
    private static PunishmentUser punishmentUser;

    static {
        DATABASE = new Database();
    }

    /**
     * Establishes a connection to the database using the provided credentials and URL,
     * and initializes necessary configurations.
     *
     * @param url      the database URL to connect to
     * @param user     the username for authentication
     * @param password the password for authentication
     */
    public static void connect(String url, String user, String password) {
        DATABASE.connect(url, user, password);
        setup();
    }

    /**
     * Disconnects the application from the currently connected database.
     * This method should be invoked when the database connection is no longer needed.
     * It ensures proper disconnection and resource cleanup related to the database connection.
     */
    public static void disconnect() {
        DATABASE.disconnect();
    }

    /**
     * Sets up the necessary database tables, procedures, and initializes providers for the application.
     * <p>
     * This method performs the initial configuration required for the application to function correctly.
     * It includes the creation of database tables and stored procedures for various data entities,
     * as well as initializing provider instances for managing these entities.
     * <p>
     * The setup process involves:
     * - Creating the necessary tables and procedures for handling users, groups, permissions, bans, mutes,
     * login histories, play times, edit logs, and related data.
     * - Initializing instances of providers and services such as editLogger, loginHistory, user, group,
     * permission, ban, and mute for subsequent use in the application.
     * <p>
     * This method is expected to be called once during the application's initialization phase.
     */
    public static void setup() {
        // Create all tables
        UserProvider.setup(DATABASE);
        GroupProvider.setup(DATABASE);
        LoginHistoryProvider.setup(DATABASE);
        ActiveSessionProvider.setup(DATABASE);
        PlayTimeProvider.setup(DATABASE);
        GroupColorProvider.setup(DATABASE);
        GroupParentProvider.setup(DATABASE);
        GroupPermissionProvider.setup(DATABASE);
        UserParentProvider.setup(DATABASE);
        UserPermissionProvider.setup(DATABASE);
        PunishmentType.setup(DATABASE);
        PunishmentReasonProvider.setup(DATABASE);
        PunishmentLogProvider.setup(DATABASE);
        PunishmentIPProvider.setup(DATABASE);
        PunishmentUserProvider.setup(DATABASE);
        // Initialize all providers
        loginHistory = getLoginHistory();
        activeSession = getActiveSession();
        user = getUser();
        group = getGroup();
        playTime = getPlayTime();
        permission = getPermission(group, user);
        punishmentReason = getPunishmentReason();
        punishmentLog = getPunishmentLog(punishmentReason);
        punishmentIP = getPunishmentIP(punishmentReason, punishmentLog);
        punishmentUser = getPunishmentUser(punishmentReason, punishmentLog);
    }

    public static void deleteUser(int userId) {
        playTime.deleteUser(userId);
        // TODO: Delete all user data
    }

    public static void deleteFullUser(int userId) {
        deleteUser(userId);
        // TODO: Delete all user data
        UUID mojangId = user.getUniqueId(userId);
        user.deleteUser(mojangId);
    }

    /**
     * Retrieves the name of the currently configured database.
     *
     * @return The name of the database as a String
     */
    public static String getDatabaseName() {
        return databaseName;
    }

    /**
     * Updates the name of the database used by the application.
     *
     * @param databaseName The name of the database to be set
     */
    public static void setDatabaseName(String databaseName) {
        MurmelAPI.databaseName = databaseName;
    }

    /**
     * Retrieves the current date format used by the application.
     *
     * @return The SimpleDateFormat instance representing the currently configured date format.
     */
    public static SimpleDateFormat getDateFormat() {
        return dateFormat;
    }

    /**
     * Sets the date format for use within the application.
     *
     * @param dateFormat The date format to be used, represented as a {@code SimpleDateFormat} instance
     */
    public static void setDateFormat(SimpleDateFormat dateFormat) {
        MurmelAPI.dateFormat = dateFormat;
    }

    /**
     * Returns the current instance of the database used by the application.
     *
     * @return The current instance of the database
     */
    public static Database getDatabase() {
        return DATABASE;
    }

    /**
     * Retrieves the instance of the LoginHistory provider. If the provider
     * is not already initialized, a new LoginHistoryProvider instance is
     * created using the pre-configured database.
     *
     * @return The LoginHistory instance, representing the login history data provider.
     */
    public static LoginHistory getLoginHistory() {
        if (loginHistory == null) loginHistory = new LoginHistoryProvider(DATABASE);
        return loginHistory;
    }

    /**
     * Retrieves the active session instance associated with the application.
     * If no active session exists, a new one is initialized using {@code ActiveSessionProvider}.
     *
     * @return The {@code ActiveSession} instance representing the application's active session.
     */
    public static ActiveSession getActiveSession() {
        if (activeSession == null) activeSession = new ActiveSessionProvider(DATABASE);
        return activeSession;
    }

    /**
     * Retrieves the singleton instance of the {@code User} object. If the instance does not
     * already exist, it is initialized using the {@code UserProvider} with the {@code DATABASE}.
     *
     * @return The singleton instance of the {@code User} object
     */
    public static User getUser() {
        if (user == null) user = new UserProvider(DATABASE);
        return user;
    }

    /**
     * Retrieves the instance of the {@code Group} object.
     * If the {@code Group} instance has not been initialized, this method initializes it
     * using the {@code GroupProvider} with the configured {@code DATABASE}.
     *
     * @return The {@code Group} instance for managing group-related operations.
     */
    public static Group getGroup() {
        if (group == null) group = new GroupProvider(DATABASE);
        return group;
    }

    /**
     * Retrieves the singleton instance of the PlayTime interface, initializing it if necessary.
     *
     * @return The singleton instance of the PlayTime interface for managing and manipulating user play times.
     */
    public static PlayTime getPlayTime() {
        if (playTime == null) playTime = new PlayTimeProvider(DATABASE);
        return playTime;
    }

    /**
     * Retrieves the Permission instance for the specified group and user. If the Permission instance
     * has not been initialized, it creates a new PermissionProvider instance using the given
     * group and user, and returns it.
     *
     * @param group The group for which the permissions are being retrieved.
     * @param user  The user for whom the permissions are being retrieved.
     * @return The Permission instance corresponding to the specified group and user.
     */
    public static Permission getPermission(Group group, User user) {
        if (permission == null) permission = new PermissionProvider(group, user);
        return permission;
    }

    /**
     * Retrieves the current permission instance for the application.
     * This method initializes the permission instance if it has not been
     * previously created, using the group and user instances.
     *
     * @return The initialized or existing {@code Permission} instance based on the current group and user.
     */
    public static Permission getPermission() {
        return getPermission(getGroup(), getUser());
    }

    /**
     * Retrieves the PunishmentReason instance, initializing it if necessary.
     * This method ensures a singleton-like behavior for the PunishmentReason instance by
     * lazily initializing it through the PunishmentReasonProvider using the DATABASE field,
     * if it has not already been instantiated.
     *
     * @return The current instance of PunishmentReason
     */
    public static PunishmentReason getPunishmentReason() {
        if (punishmentReason == null) punishmentReason = new PunishmentReasonProvider(DATABASE);
        return punishmentReason;
    }

    /**
     * Retrieves the punishment log associated with the specified punishment reason.
     * If the punishment log has not been initialized, it will create a new instance using the
     * provided reason and the database configuration.
     *
     * @param reason The reason for the punishment to retrieve or generate the punishment log
     * @return The instance of the punishment log corresponding to the given punishment reason
     */
    public static PunishmentLog getPunishmentLog(PunishmentReason reason) {
        if (punishmentLog == null) punishmentLog = new PunishmentLogProvider(DATABASE, reason);
        return punishmentLog;
    }

    /**
     * Retrieves the default punishment log associated with the system.
     * This method internally uses the default punishment reason to obtain the punishment log.
     *
     * @return The punishment log associated with the default punishment reason
     */
    public static PunishmentLog getPunishmentLog() {
        return getPunishmentLog(getPunishmentReason());
    }

    /**
     * Retrieves the PunishmentIP instance associated with the provided punishment reason and log.
     * If the instance is not initialized, it creates a new PunishmentIPProvider using the database, reason, and log.
     *
     * @param reason The PunishmentReason object used to associate specific punishment logic
     * @param log    The PunishmentLog object that tracks punishment-related actions and history
     * @return The PunishmentIP instance for managing punishment-related IP operations
     */
    public static PunishmentIP getPunishmentIP(PunishmentReason reason, PunishmentLog log) {
        if (punishmentIP == null) punishmentIP = new PunishmentIPProvider(DATABASE, reason, log);
        return punishmentIP;
    }

    /**
     * Retrieves the default instance of PunishmentIP using the default punishment reason
     * and punishment log.
     *
     * @return The default PunishmentIP instance, constructed using the default
     * punishment reason and punishment log.
     */
    public static PunishmentIP getPunishmentIP() {
        return getPunishmentIP(getPunishmentReason(), getPunishmentLog());
    }

    /**
     * Retrieves the instance of {@code PunishmentUser}, initializing it if necessary,
     * based on the provided {@code PunishmentReason} and {@code PunishmentLog}.
     *
     * @param reason The reason associated with the punishment
     * @param log    The log containing details of the punishment
     * @return The instance of {@code PunishmentUser} associated with the given reason and log
     */
    public static PunishmentUser getPunishmentUser(PunishmentReason reason, PunishmentLog log) {
        if (punishmentUser == null) punishmentUser = new PunishmentUserProvider(DATABASE, reason, log);
        return punishmentUser;
    }

    /**
     * Retrieves a `PunishmentUser` object by internally obtaining the required
     * `PunishmentReason` and `PunishmentLog` instances through corresponding methods.
     *
     * @return A `PunishmentUser` object constructed using the retrieved `PunishmentReason`
     * and `PunishmentLog`
     */
    public static PunishmentUser getPunishmentUser() {
        return getPunishmentUser(getPunishmentReason(), getPunishmentLog());
    }
}

package de.murmelmeister.murmelapi;

import de.murmelmeister.murmelapi.database.Database;
import de.murmelmeister.murmelapi.group.Group;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
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

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Database DATABASE;

    private static String databaseName = "MurmelAPI";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

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

    public static void connect(String url, String user, String password) {
        DATABASE.connectMySQL(url, user, password);
        setup();
    }

    public static void disconnect() {
        DATABASE.disconnect();
    }

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
        punishmentIP = getPunishmentIP(punishmentLog);
        punishmentUser = getPunishmentUser(punishmentLog);
    }

    public static int deleteUserSoft(int userId) {
        if (userId < 1) return 0;
        int sessionRow = activeSession.closeSession(userId);
        int loginRows = loginHistory.deleteUserLogins(userId);
        int permissionRow = user.getPermission().clearPermission(userId);
        int parentRow = user.getParent().clearParent(userId);
        int playTimeRow = playTime.deleteUser(userId);
        return sessionRow + loginRows + permissionRow + parentRow + playTimeRow;
    }

    public static int deleteUserHard(int userId) {
        if (userId < 1) return 0;
        int softDeleteRow = deleteUserSoft(userId);
        int punishmentRow = punishmentUser.unpunish(userId);
        int logsRow = punishmentLog.deleteUserLogs(userId);
        int userRow = user.deleteUser(userId);
        return softDeleteRow + punishmentRow + logsRow + userRow;
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

    public static LoginHistory getLoginHistory() {
        if (loginHistory == null)
            loginHistory = new LoginHistoryProvider(DATABASE);
        return loginHistory;
    }

    public static ActiveSession getActiveSession() {
        if (activeSession == null)
            activeSession = new ActiveSessionProvider(DATABASE);
        return activeSession;
    }

    public static User getUser() {
        if (user == null)
            user = new UserProvider(DATABASE);
        return user;
    }

    public static Group getGroup() {
        if (group == null)
            group = new GroupProvider(DATABASE);
        return group;
    }

    public static PlayTime getPlayTime() {
        if (playTime == null)
            playTime = new PlayTimeProvider(DATABASE);
        return playTime;
    }

    public static Permission getPermission(Group group, User user) {
        if (permission == null)
            permission = new PermissionProvider(group, user);
        return permission;
    }

    public static Permission getPermission() {
        return getPermission(getGroup(), getUser());
    }

    public static PunishmentReason getPunishmentReason() {
        if (punishmentReason == null)
            punishmentReason = new PunishmentReasonProvider(DATABASE);
        return punishmentReason;
    }

    public static PunishmentLog getPunishmentLog(PunishmentReason reason) {
        if (punishmentLog == null)
            punishmentLog = new PunishmentLogProvider(DATABASE, reason);
        return punishmentLog;
    }

    public static PunishmentLog getPunishmentLog() {
        return getPunishmentLog(getPunishmentReason());
    }

    public static PunishmentIP getPunishmentIP(PunishmentLog log) {
        if (punishmentIP == null)
            punishmentIP = new PunishmentIPProvider(DATABASE, log);
        return punishmentIP;
    }

    public static PunishmentIP getPunishmentIP() {
        return getPunishmentIP(getPunishmentLog());
    }

    public static PunishmentUser getPunishmentUser(PunishmentLog log) {
        if (punishmentUser == null)
            punishmentUser = new PunishmentUserProvider(DATABASE, log);
        return punishmentUser;
    }

    public static PunishmentUser getPunishmentUser() {
        return getPunishmentUser(getPunishmentLog());
    }
}

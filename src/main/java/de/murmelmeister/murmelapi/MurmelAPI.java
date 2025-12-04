package de.murmelmeister.murmelapi;

import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.GroupProviderImpl;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProviderImpl;
import de.murmelmeister.murmelapi.group.color.GroupColorType;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProviderImpl;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProviderImpl;
import de.murmelmeister.murmelapi.language.LanguageProvider;
import de.murmelmeister.murmelapi.language.LanguageProviderImpl;
import de.murmelmeister.murmelapi.language.message.MessageProvider;
import de.murmelmeister.murmelapi.language.message.MessageProviderImpl;
import de.murmelmeister.murmelapi.language.message.MessageService;
import de.murmelmeister.murmelapi.language.message.MurmelMessage;
import de.murmelmeister.murmelapi.permission.Permission;
import de.murmelmeister.murmelapi.permission.PermissionProvider;
import de.murmelmeister.murmelapi.punishment.PunishmentService;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProvider;
import de.murmelmeister.murmelapi.punishment.audit.PunishmentLogProviderImpl;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProvider;
import de.murmelmeister.murmelapi.punishment.ip.PunishmentCurrentIpProviderImpl;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProvider;
import de.murmelmeister.murmelapi.punishment.reason.PunishmentReasonProviderImpl;
import de.murmelmeister.murmelapi.punishment.type.PunishmentType;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsProvider;
import de.murmelmeister.murmelapi.settings.SettingsProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserProviderImpl;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.login.UserLoginProvider;
import de.murmelmeister.murmelapi.user.login.UserLoginProviderImpl;
import de.murmelmeister.murmelapi.user.parent.UserParentProvider;
import de.murmelmeister.murmelapi.user.parent.UserParentProviderImpl;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProvider;
import de.murmelmeister.murmelapi.user.permission.UserPermissionProviderImpl;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProvider;
import de.murmelmeister.murmelapi.user.playtime.UserPlayTimeProviderImpl;
import de.murmelmeister.murmelapi.user.session.UserSessionProvider;
import de.murmelmeister.murmelapi.user.session.UserSessionProviderImpl;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Database DATABASE;

    public static final String ENGLISH_CODE = "en-US";
    public static final String GERMAN_CODE = "de-DE";
    public static final int DEFAULT_GROUP_ID = 1;
    public static final int CONSOLE_USER_ID = -1;

    private static Long fetchLimit = null;
    private static long cacheCapacity = 10_000; // Default cache size
    private static Duration refreshInterval = Duration.ofMinutes(30);
    private static boolean bootstrapMessages = true;

    private static SettingsProvider settingsProvider;
    private static SettingsService settingsService;

    private static LanguageProvider languageProvider;
    private static MessageProvider messageProvider;
    private static MessageService messageService;

    private static UserProvider userProvider;
    private static UserPlayTimeProvider userPlayTimeProvider;
    private static UserLoginProvider userLoginProvider;
    private static UserSessionProvider userSessionProvider;
    private static UserService userService;

    private static GroupProvider groupProvider;
    private static GroupColorProvider groupColorProvider;

    private static UserPermissionProvider userPermissionProvider;
    private static UserParentProvider userParentProvider;
    private static GroupPermissionProvider groupPermissionProvider;
    private static GroupParentProvider groupParentProvider;
    private static Permission permission;

    private static PunishmentReasonProvider punishReasonProvider;
    private static PunishmentLogProvider punishLogProvider;
    private static PunishmentCurrentIpProvider punishIpProvider;
    private static PunishmentCurrentUserProvider punishUserProvider;
    private static PunishmentService punishmentService;

    static {
        DATABASE = new Database();
    }

    public static void connect(String propertyFileName) {
        //readBootstrapFlag(loadProperties(propertyFileName));
        DATABASE.connect(propertyFileName);
        //setup();
    }

    public static void connect(Properties properties) {
        //readBootstrapFlag(properties);
        DATABASE.connect(properties);
        //setup();
    }

    public static void connect(String url, String user, String password) {
        DATABASE.connect(url, user, password);
        //setup();
    }

    public static void disconnect() {
        closeCache();
        DATABASE.disconnect();
    }

    public static void setup() {
        // Create all tables
        SettingsProviderImpl.setup(DATABASE);
        LanguageProviderImpl.setup(DATABASE);
        LanguageProviderImpl.createDefaultLanguages(DATABASE);
        MessageProviderImpl.setup(DATABASE);

        UserProviderImpl.setup(DATABASE);
        UserProviderImpl.createConsoleUser(DATABASE);
        UserPlayTimeProviderImpl.setup(DATABASE);
        UserLoginProviderImpl.setup(DATABASE);
        UserSessionProviderImpl.setup(DATABASE);

        GroupProviderImpl.setup(DATABASE);
        GroupProviderImpl.createDefaultGroup(DATABASE);
        GroupColorType.setup(DATABASE);
        GroupColorType.createDefaultTypes(DATABASE);
        GroupColorProviderImpl.setup(DATABASE);

        UserPermissionProviderImpl.setup(DATABASE);
        UserParentProviderImpl.setup(DATABASE);
        GroupPermissionProviderImpl.setup(DATABASE);
        GroupParentProviderImpl.setup(DATABASE);
        PermissionProvider.setup(DATABASE);

        PunishmentType.setup(DATABASE);
        PunishmentType.createDefaultTypes(DATABASE);
        PunishmentReasonProviderImpl.setup(DATABASE);
        PunishmentLogProviderImpl.setup(DATABASE);
        PunishmentCurrentIpProviderImpl.setup(DATABASE);
        PunishmentCurrentUserProviderImpl.setup(DATABASE);
        // Initialize all providers
        settingsProvider = getSettingsProvider();
        settingsService = getSettingsService(settingsProvider);

        languageProvider = getLanguageProvider();
        messageProvider = getMessageProvider();
        messageService = getMessageService(languageProvider, messageProvider);
        if (bootstrapMessages)
            MurmelMessage.loadMessages(messageProvider);

        userProvider = getUserProvider();
        userPlayTimeProvider = getUserPlayTimeProvider();
        userLoginProvider = getUserLoginProvider();
        userSessionProvider = getUserSessionProvider();
        userService = getUserService(userProvider, userPlayTimeProvider, userLoginProvider, userSessionProvider);

        groupProvider = getGroupProvider();
        groupColorProvider = getGroupColorProvider();

        userPermissionProvider = getUserPermissionProvider();
        userParentProvider = getUserParentProvider();
        groupPermissionProvider = getGroupPermissionProvider();
        groupParentProvider = getGroupParentProvider();
        permission = getPermission(userProvider, groupParentProvider, groupPermissionProvider, userParentProvider, userPermissionProvider);

        punishReasonProvider = getPunishmentReasonProvider();
        punishLogProvider = getPunishmentLogProvider();
        punishIpProvider = getPunishmentCurrentIpProvider();
        punishUserProvider = getPunishmentCurrentUserProvider();
        punishmentService = getPunishmentService(punishReasonProvider, punishLogProvider, punishIpProvider, punishUserProvider);
    }

    public static void closeCache() {
        settingsProvider.closeCache();
        languageProvider.closeCache();
        messageProvider.closeCache();

        userProvider.closeCache();
        userPlayTimeProvider.closeCache();
        userLoginProvider.closeCache();
        userSessionProvider.closeCache();

        groupProvider.closeCache();
        groupColorProvider.closeCache();

        userPermissionProvider.closeCache();
        userParentProvider.closeCache();
        groupParentProvider.closeCache();
        groupPermissionProvider.closeCache();
        permission.closeCache();

        punishReasonProvider.closeCache();
        punishLogProvider.closeCache();
        punishIpProvider.closeCache();
        punishUserProvider.closeCache();
    }

    /*public static int deleteUserSoft(int userId) {
        if (userId < 1) return 0;
        int sessionRow = activeSession.closeSession(userId);
        int loginRows = loginHistory.deleteUserLogins(userId);
        int permissionRow = userProvider.getPermission().clearPermission(userId);
        int parentRow = userProvider.getParent().clearParent(userId);
        int playTimeRow = playTime.deleteUser(userId);
        return sessionRow + loginRows + permissionRow + parentRow + playTimeRow;
    }

    public static int deleteUserHard(int userId) {
        if (userId < 1) return 0;
        int softDeleteRow = deleteUserSoft(userId);
        //int punishmentRow = punishmentUser.unpunish(userId);
        //int logsRow = punishmentLog.deleteUserLogs(userId);
        int userRow = userProvider.deleteUser(userId);
        //return softDeleteRow + punishmentRow + logsRow + userRow;
        return softDeleteRow + userRow;
    }*/

    public static Database getDatabase() {
        return DATABASE;
    }

    public static DateTimeFormatter getDateTimeFormatter(int languageId) {
        String pattern = messageService.getMessage(MurmelMessage.DATE_TIME_FORMAT.getTag(), languageId);
        if (pattern == null)
            throw new IllegalArgumentException("No pattern for language " + languageId + " found!");
        return DateTimeFormatter.ofPattern(pattern);
    }

    public static Long getFetchLimit() {
        return fetchLimit;
    }

    public static void setFetchLimit(Long fetchLimit) {
        MurmelAPI.fetchLimit = fetchLimit;
    }

    public static long getCacheCapacity() {
        return cacheCapacity;
    }

    public static void setCacheCapacity(long cacheCapacity) {
        MurmelAPI.cacheCapacity = cacheCapacity;
    }

    public static Duration getRefreshInterval() {
        return refreshInterval;
    }

    public static void setRefreshInterval(Duration refreshInterval) {
        MurmelAPI.refreshInterval = refreshInterval;
    }

    public static boolean isBootstrapMessages() {
        return bootstrapMessages;
    }

    public static void setBootstrapMessages(boolean bootstrapMessages) {
        MurmelAPI.bootstrapMessages = bootstrapMessages;
    }

    public static SettingsProvider getSettingsProvider() {
        if (settingsProvider == null)
            settingsProvider = new SettingsProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return settingsProvider;
    }

    public static SettingsService getSettingsService(SettingsProvider settingsProvider) {
        if (settingsService == null)
            settingsService = new SettingsService(settingsProvider);
        return settingsService;
    }

    public static SettingsService getSettingsService() {
        return getSettingsService(getSettingsProvider());
    }

    public static LanguageProvider getLanguageProvider() {
        if (languageProvider == null)
            languageProvider = new LanguageProviderImpl(DATABASE, cacheCapacity);
        return languageProvider;
    }

    public static MessageProvider getMessageProvider() {
        if (messageProvider == null)
            messageProvider = new MessageProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return messageProvider;
    }

    public static MessageService getMessageService(LanguageProvider languageProvider, MessageProvider messageProvider) {
        if (messageService == null)
            messageService = new MessageService(languageProvider, messageProvider);
        return messageService;
    }

    public static MessageService getMessageService() {
        return getMessageService(getLanguageProvider(), getMessageProvider());
    }

    public static UserProvider getUserProvider() {
        if (userProvider == null)
            userProvider = new UserProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userProvider;
    }

    public static UserPlayTimeProvider getUserPlayTimeProvider() {
        if (userPlayTimeProvider == null)
            userPlayTimeProvider = new UserPlayTimeProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userPlayTimeProvider;
    }

    public static UserLoginProvider getUserLoginProvider() {
        if (userLoginProvider == null)
            userLoginProvider = new UserLoginProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userLoginProvider;
    }

    public static UserSessionProvider getUserSessionProvider() {
        if (userSessionProvider == null)
            userSessionProvider = new UserSessionProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userSessionProvider;
    }

    public static UserService getUserService(UserProvider userProvider,
                                             UserPlayTimeProvider playTimeProvider,
                                             UserLoginProvider loginProvider,
                                             UserSessionProvider sessionProvider) {
        if (userService == null)
            userService = new UserService(userProvider, playTimeProvider, loginProvider, sessionProvider);
        return userService;
    }

    public static UserService getUserService() {
        return getUserService(getUserProvider(), getUserPlayTimeProvider(), getUserLoginProvider(), getUserSessionProvider());
    }

    public static GroupProvider getGroupProvider() {
        if (groupProvider == null)
            groupProvider = new GroupProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return groupProvider;
    }

    public static GroupColorProvider getGroupColorProvider() {
        if (groupColorProvider == null)
            groupColorProvider = new GroupColorProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return groupColorProvider;
    }

    public static UserPermissionProvider getUserPermissionProvider() {
        if (userPermissionProvider == null)
            userPermissionProvider = new UserPermissionProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userPermissionProvider;
    }

    public static UserParentProvider getUserParentProvider() {
        if (userParentProvider == null)
            userParentProvider = new UserParentProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userParentProvider;
    }

    public static GroupPermissionProvider getGroupPermissionProvider() {
        if (groupPermissionProvider == null)
            groupPermissionProvider = new GroupPermissionProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return groupPermissionProvider;
    }

    public static GroupParentProvider getGroupParentProvider() {
        if (groupParentProvider == null)
            groupParentProvider = new GroupParentProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return groupParentProvider;
    }

    public static Permission getPermission(UserProvider userProvider, GroupParentProvider groupParentProvider,
                                           GroupPermissionProvider groupPermissionProvider, UserParentProvider userParentProvider,
                                           UserPermissionProvider userPermissionProvider) {
        if (permission == null)
            permission = new PermissionProvider(DATABASE, userProvider, groupParentProvider, groupPermissionProvider, userParentProvider, userPermissionProvider,
                    cacheCapacity, refreshInterval);
        return permission;
    }

    public static Permission getPermission() {
        return getPermission(getUserProvider(), getGroupParentProvider(), getGroupPermissionProvider(), getUserParentProvider(), getUserPermissionProvider());
    }

    public static PunishmentReasonProvider getPunishmentReasonProvider() {
        if (punishReasonProvider == null)
            punishReasonProvider = new PunishmentReasonProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return punishReasonProvider;
    }

    public static PunishmentLogProvider getPunishmentLogProvider() {
        if (punishLogProvider == null)
            punishLogProvider = new PunishmentLogProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return punishLogProvider;
    }

    public static PunishmentCurrentIpProvider getPunishmentCurrentIpProvider() {
        if (punishIpProvider == null)
            punishIpProvider = new PunishmentCurrentIpProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return punishIpProvider;
    }

    public static PunishmentCurrentUserProvider getPunishmentCurrentUserProvider() {
        if (punishUserProvider == null)
            punishUserProvider = new PunishmentCurrentUserProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return punishUserProvider;
    }

    public static PunishmentService getPunishmentService(PunishmentReasonProvider reasonProvider,
                                                         PunishmentLogProvider logProvider,
                                                         PunishmentCurrentIpProvider ipProvider,
                                                         PunishmentCurrentUserProvider userProvider) {
        if (punishmentService == null)
            punishmentService = new PunishmentService(reasonProvider, logProvider, ipProvider, userProvider);
        return punishmentService;
    }

    public static PunishmentService getPunishmentService() {
        return getPunishmentService(getPunishmentReasonProvider(), getPunishmentLogProvider(), getPunishmentCurrentIpProvider(), getPunishmentCurrentUserProvider());
    }

    private static void readBootstrapFlag(Properties properties) {
        if (properties == null)
            return;

        String flag = properties.getProperty("murmelapi.bootstrap.messages");
        if (flag != null)
            setBootstrapMessages(Boolean.parseBoolean(flag.trim()));
    }

    private static Properties loadProperties(String propertyFileName) {
        if (propertyFileName == null)
            return null;

        Properties properties = new Properties();
        try (FileInputStream stream = new FileInputStream(propertyFileName)) {
            properties.load(stream);
            return properties;
        } catch (IOException ignored) {
            return null;
        }
    }
}

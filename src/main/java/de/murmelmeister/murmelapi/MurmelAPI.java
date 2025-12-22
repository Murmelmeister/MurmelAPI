package de.murmelmeister.murmelapi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import de.murmelmeister.library.database.Database;
import de.murmelmeister.murmelapi.clan.ClanProvider;
import de.murmelmeister.murmelapi.clan.ClanProviderImpl;
import de.murmelmeister.murmelapi.clan.group.ClanGroupProvider;
import de.murmelmeister.murmelapi.clan.group.ClanGroupProviderImpl;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProvider;
import de.murmelmeister.murmelapi.clan.member.ClanMemberProviderImpl;
import de.murmelmeister.murmelapi.clan.parent.ClanParentProvider;
import de.murmelmeister.murmelapi.clan.parent.ClanParentProviderImpl;
import de.murmelmeister.murmelapi.clan.permission.ClanPermissionProvider;
import de.murmelmeister.murmelapi.clan.permission.ClanPermissionProviderImpl;
import de.murmelmeister.murmelapi.color.PrefixColorProvider;
import de.murmelmeister.murmelapi.color.PrefixColorProviderImpl;
import de.murmelmeister.murmelapi.group.GroupProvider;
import de.murmelmeister.murmelapi.group.GroupProviderImpl;
import de.murmelmeister.murmelapi.group.color.GroupColorProvider;
import de.murmelmeister.murmelapi.group.color.GroupColorProviderImpl;
import de.murmelmeister.murmelapi.group.parent.GroupParentProvider;
import de.murmelmeister.murmelapi.group.parent.GroupParentProviderImpl;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProvider;
import de.murmelmeister.murmelapi.group.permission.GroupPermissionProviderImpl;
import de.murmelmeister.murmelapi.inventory.InventoryTypeProvider;
import de.murmelmeister.murmelapi.inventory.InventoryTypeProviderImpl;
import de.murmelmeister.murmelapi.language.Language;
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
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProvider;
import de.murmelmeister.murmelapi.punishment.user.PunishmentCurrentUserProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsProvider;
import de.murmelmeister.murmelapi.settings.SettingsProviderImpl;
import de.murmelmeister.murmelapi.settings.SettingsService;
import de.murmelmeister.murmelapi.user.UserProvider;
import de.murmelmeister.murmelapi.user.UserProviderImpl;
import de.murmelmeister.murmelapi.user.UserService;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProvider;
import de.murmelmeister.murmelapi.user.color.UserPrefixColorProviderImpl;
import de.murmelmeister.murmelapi.user.inventory.UserInventoryProvider;
import de.murmelmeister.murmelapi.user.inventory.UserInventoryProviderImpl;
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
import de.murmelmeister.murmelapi.utils.update.RefreshListener;
import de.murmelmeister.murmelapi.utils.update.RefreshUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * The MurmelAPI main class.
 */
public final class MurmelAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(MurmelAPI.class);
    private static final Database DATABASE;
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static final String ENGLISH_CODE = "en-US";
    public static final String GERMAN_CODE = "de-DE";
    public static final int DEFAULT_GROUP_ID = 1;
    public static final int CONSOLE_USER_ID = -1;

    private static Long fetchLimit = null;
    private static long cacheCapacity = 10_000; // Default cache size
    private static Duration refreshInterval = Duration.ofMinutes(30);

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

    private static ClanProvider clanProvider;
    private static ClanMemberProvider clanMemberProvider;
    private static ClanGroupProvider clanGroupProvider;
    private static ClanParentProvider clanParentProvider;
    private static ClanPermissionProvider clanPermissionProvider;

    private static PrefixColorProvider prefixColorProvider;
    private static UserPrefixColorProvider userPrefixColorProvider;

    private static InventoryTypeProvider inventoryTypeProvider;
    private static UserInventoryProvider userInventoryProvider;

    static {
        DATABASE = new Database();
    }

    public static void connect(HikariConfig config) {
        DATABASE.connect(config);
    }

    public static void connect(String propertyFileName) {
        DATABASE.connect(propertyFileName);
    }

    public static void connect(Properties properties) {
        DATABASE.connect(properties);
    }

    public static void connect(String url, String user, String password) {
        DATABASE.connect(url, user, password);
    }

    public static void connectToMariadb(String hostname, int port, String databaseName, String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mariadb://" + hostname + ":" + port + "/" + databaseName + "?useUnicode=true&characterEncoding=UTF-8");
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.mariadb.jdbc.Driver");
        DATABASE.connect(config);
    }

    public static void disconnect() {
        closeCaches();
        DATABASE.disconnect();
    }

    public static void setup() {
        createAllTables();
        initProviders();
    }

    public static void createAllTables() {
        runSqlScript("schema.sql");
        runSqlScript("data.sql");
        PermissionProvider.setup(DATABASE);
    }

    private static void runSqlScript(String script) {
        try (InputStream in = MurmelAPI.class.getClassLoader().getResourceAsStream(script)) {
            if (in == null) throw new IllegalStateException("Missing schema.sql");
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                DATABASE.runSqlScript(reader);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to run schema.sql", e);
        }
    }

    public static void initProviders() {
        settingsProvider = getSettingsProvider();
        settingsService = getSettingsService(settingsProvider);

        languageProvider = getLanguageProvider();
        messageProvider = getMessageProvider();
        messageService = getMessageService(languageProvider, messageProvider);

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

        clanProvider = getClanProvider();
        clanMemberProvider = getClanMemberProvider();
        clanGroupProvider = getClanGroupProvider();
        clanParentProvider = getClanParentProvider();
        clanPermissionProvider = getClanPermissionProvider();

        prefixColorProvider = getPrefixColorProvider();
        userPrefixColorProvider = getUserPrefixColorProvider();

        inventoryTypeProvider = getInventoryTypeProvider();
        userInventoryProvider = getUserInventoryProvider();
    }

    public static void loadMessages() {
        messageProvider = getMessageProvider(); // If messageProvider are null
        MurmelMessage.loadMessages(messageProvider);
    }

    public static void closeCaches() {
        List<RefreshListener> listeners = List.copyOf(RefreshUtil.getListeners());
        listeners.forEach(listener -> {
            if (RefreshUtil.isRegistered(listener) && listener instanceof AutoCloseable closeable) {
                try {
                    closeable.close();
                } catch (Exception e) {
                    LOGGER.warn("Failed to close listener {}", listener, e);
                }
                RefreshUtil.unregister(listener);
            }
        });
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

    public static DecimalFormat getDecimalFormat(int languageId, String pattern) {
        Language language = getLanguageProvider().findById(languageId);
        if (language == null)
            throw new IllegalArgumentException("No language for id " + languageId + " found!");
        Locale locale = Locale.of(language.code());
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locale);
        return new DecimalFormat(pattern, symbols);
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

    public static SettingsProvider getSettingsProvider() {
        if (settingsProvider == null)
            settingsProvider = new SettingsProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return settingsProvider;
    }

    public static SettingsService getSettingsService(SettingsProvider settingsProvider) {
        if (settingsService == null)
            settingsService = new SettingsService(settingsProvider, GSON);
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

    public static ClanProvider getClanProvider() {
        if (clanProvider == null)
            clanProvider = new ClanProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return clanProvider;
    }

    public static ClanMemberProvider getClanMemberProvider() {
        if (clanMemberProvider == null)
            clanMemberProvider = new ClanMemberProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return clanMemberProvider;
    }

    public static ClanGroupProvider getClanGroupProvider() {
        if (clanGroupProvider == null)
            clanGroupProvider = new ClanGroupProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return clanGroupProvider;
    }

    public static ClanParentProvider getClanParentProvider() {
        if (clanParentProvider == null)
            clanParentProvider = new ClanParentProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return clanParentProvider;
    }

    public static ClanPermissionProvider getClanPermissionProvider() {
        if (clanPermissionProvider == null)
            clanPermissionProvider = new ClanPermissionProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return clanPermissionProvider;
    }

    public static PrefixColorProvider getPrefixColorProvider() {
        if (prefixColorProvider == null)
            prefixColorProvider = new PrefixColorProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return prefixColorProvider;
    }

    public static UserPrefixColorProvider getUserPrefixColorProvider() {
        if (userPrefixColorProvider == null)
            userPrefixColorProvider = new UserPrefixColorProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userPrefixColorProvider;
    }

    public static InventoryTypeProvider getInventoryTypeProvider() {
        if (inventoryTypeProvider == null)
            inventoryTypeProvider = new InventoryTypeProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return inventoryTypeProvider;
    }

    public static UserInventoryProvider getUserInventoryProvider() {
        if (userInventoryProvider == null)
            userInventoryProvider = new UserInventoryProviderImpl(DATABASE, fetchLimit, cacheCapacity, refreshInterval);
        return userInventoryProvider;
    }
}
